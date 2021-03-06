package at.ac.tuwien.aic.ws14.group2.onion.node.local.socks.server;

import at.ac.tuwien.aic.ws14.group2.onion.directory.api.service.ChainNodeInformation;
import at.ac.tuwien.aic.ws14.group2.onion.directory.api.service.DirectoryService;
import at.ac.tuwien.aic.ws14.group2.onion.node.common.node.Endpoint;
import at.ac.tuwien.aic.ws14.group2.onion.node.local.node.ChainMetaData;
import at.ac.tuwien.aic.ws14.group2.onion.node.local.node.LocalNodeCore;
import at.ac.tuwien.aic.ws14.group2.onion.node.local.socks.SocksCallBack;
import at.ac.tuwien.aic.ws14.group2.onion.node.local.socks.exceptions.AddressTypeNotSupportedException;
import at.ac.tuwien.aic.ws14.group2.onion.node.local.socks.exceptions.CommandNotSupportedException;
import at.ac.tuwien.aic.ws14.group2.onion.node.local.socks.exceptions.ErrorCode;
import at.ac.tuwien.aic.ws14.group2.onion.node.local.socks.messages.*;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Consumer;

/**
 * Created by klaus on 11/30/14.
 */
public class SocksWorker implements Runnable, AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(SocksWorker.class.getName());
	private static final int CHAIN_LENGTH = 3;

	private final Socket socket;
	private final Consumer<SocksWorker> closeCallback;
	private final SynchronousQueue<ChainMetaData> chainEstablishedAnswerQueue = new SynchronousQueue<>();
	private final LocalNodeCore localNodeCore;
	private final DirectoryService.Client directoryClient;
	private Short circuitId;
	private SocksDataForwarder socksDataForwarder;

	public SocksWorker(Socket socket, LocalNodeCore localNodeCore, DirectoryService.Client directoryClient, Consumer<SocksWorker> closeCallback) {
		this.socket = Objects.requireNonNull(socket);
		this.closeCallback = Objects.requireNonNull(closeCallback);
		this.localNodeCore = Objects.requireNonNull(localNodeCore);
		this.directoryClient = Objects.requireNonNull(directoryClient);
	}

	@Override
	public void run() {
		if (!socket.isConnected())
			throw new IllegalStateException("TCP socket not connected");

		try {
			try {
				DataInputStream inputStream = new DataInputStream(socket.getInputStream());
				OutputStream outputStream = socket.getOutputStream();

				/*
				 * SOCKS connection initialization
				 */

				// Read the method selection request
				MethodSelectionRequest methodSelectionRequest = MethodSelectionRequest.fromInputStream(inputStream);

				// Check request
				if (Arrays.stream(methodSelectionRequest.getMethods()).noneMatch(Method.NO_AUTHENTICATION_REQUIRED::equals)) {
					// No supported method found

					// Tell the originator
					MethodSelectionReply reply = new MethodSelectionReply(Method.NO_ACCEPTABLE_METHODS);
					outputStream.write(reply.toByteArray());

					// ...and quit
					return;
				}

				// Send method selection reply
				MethodSelectionReply methodSelectionReply = new MethodSelectionReply(Method.NO_AUTHENTICATION_REQUIRED);
				outputStream.write(methodSelectionReply.toByteArray());

				// Read the command request
				CommandRequest commandRequest;
				try {
					commandRequest = CommandRequest.fromInputStream(inputStream);

					if (!Command.CONNECT.equals(commandRequest.getCommand()))
						throw new CommandNotSupportedException(commandRequest.getCommand().getValue());
				} catch (AddressTypeNotSupportedException | CommandNotSupportedException e) {
					// Something is not supported

					// Tell the originator
					CommandReply reply = new CommandReply(e.getReplyType(), new SocksAddress(socket.getLocalAddress(), socket.getLocalPort()));
					outputStream.write(reply.toByteArray());

					// ...and quit
					return;
				}

				SocksAddress destination = commandRequest.getDestination();

				// Create the chain
				logger.info("Creating chain and connecting to " + destination.getAddress());
				createChain();

				// Connect to the target
				logger.info("Connecting to destination");
				if (this.localNodeCore.hasWebCallback())
					this.localNodeCore.getWebCallback().info(circuitId, "Connecting to destination...");

				localNodeCore.connectTo(circuitId, convertSocksAddressToEndpoint(destination));

				if (this.localNodeCore.hasWebCallback()) {
					this.localNodeCore.getWebCallback().info(circuitId, "Connection established");
					this.localNodeCore.getWebCallback().info(circuitId, "Starting data transfer...");
				}

				// Create and start the forwarder of for the client data
				logger.info("Starting SOCKS data forwarder");
				socksDataForwarder = new SocksDataForwarder(socket, circuitId, localNodeCore);
				socksDataForwarder.setName("SOCKS data forwarder thread of " + Thread.currentThread().getName());
				socksDataForwarder.setUncaughtExceptionHandler(Thread.currentThread().getUncaughtExceptionHandler());
				socksDataForwarder.start();

				// Send succeeded command reply (currently just a fake bound address is returned)
				CommandReply commandReply = new CommandReply(ReplyType.SUCCEEDED, new SocksAddress(InetAddress.getLoopbackAddress(), 0xdead));
				outputStream.write(commandReply.toByteArray());

				// Wait on SocksDataForwarder
				socksDataForwarder.join();

				if (this.localNodeCore.hasWebCallback()) {
					this.localNodeCore.getWebCallback().info(circuitId, "Data received completely");
					this.localNodeCore.getWebCallback().info(circuitId, "Destroying the chain again...");
				}

                shutdownChain();

				if (this.localNodeCore.hasWebCallback())
					this.localNodeCore.getWebCallback().info(circuitId, "Chain destroyed");

			} finally {
				close();
			}
		} catch (Exception e) {
			Thread.UncaughtExceptionHandler eh = Thread.currentThread()
					.getUncaughtExceptionHandler();
			if (eh != null)
				eh.uncaughtException(Thread.currentThread(), e);
			else
				logger.error("Uncaught exception in thread: " + Thread.currentThread().getName(), e);
		}
	}

	private Endpoint convertSocksAddressToEndpoint(SocksAddress socksAddress) {
		Objects.requireNonNull(socksAddress);

		switch (socksAddress.getAddressType()) {
			case DOMAINNAME:
				return new Endpoint(socksAddress.getHostName(), socksAddress.getPort());
			case IP_V4_ADDRESS:
			case IP_V6_ADDRESS:
				return new Endpoint(socksAddress.getAddress(), socksAddress.getPort());
			default:
				throw new IllegalStateException("address type is in an illegal state");

		}
	}

	private void createChain() throws TException, InterruptedException {
		ChainMetaData chainMetaData;

		synchronized (this.directoryClient) {
			do {
				// Get chain from the directory
				List<ChainNodeInformation> chainMetaDataList = directoryClient.getChain(CHAIN_LENGTH);
				chainMetaData = ChainMetaData.fromChainNodeInformationList(chainMetaDataList);

				// Check if we can create a chain
			} while (!localNodeCore.checkExitNode(chainMetaData));
		}

        // Create the chain
        localNodeCore.createChain(chainMetaData, new SocksCallbackImpl());
		circuitId = chainMetaData.getCircuitID();
		if (this.localNodeCore.hasWebCallback())
			this.localNodeCore.getWebCallback().info(circuitId, "Creating chain...");

		// Wait for completion of the chain creation
		chainMetaData = this.chainEstablishedAnswerQueue.take();

		if (this.localNodeCore.hasWebCallback())
			this.localNodeCore.getWebCallback().info(circuitId, "Chain established");
	}

    private void shutdownChain() {
        logger.info("shutting down chain");
        localNodeCore.destroyChain(circuitId);
    }

	@Override
	public void close() throws IOException {
		socket.close();

		if (socksDataForwarder != null)
			socksDataForwarder.close();

		this.closeCallback.accept(this);
	}

	private class SocksCallbackImpl implements SocksCallBack {

		@Override
		public void chainEstablished(ChainMetaData chainMetaData) {
			if (SocksWorker.this.localNodeCore.hasWebCallback())
				SocksWorker.this.localNodeCore.getWebCallback().chainEstablished(circuitId, chainMetaData);
			SocksWorker.this.chainEstablishedAnswerQueue.offer(chainMetaData);
		}

		@Override
		public void chainDestroyed() {
			if (SocksWorker.this.localNodeCore.hasWebCallback())
				SocksWorker.this.localNodeCore.getWebCallback().chainDestroyed(circuitId);
			try {
				SocksWorker.this.close();
			} catch (IOException e) {
				logger.error("Uncaught exception while closing SocksWorker", e);
			}
		}

		@Override
		public void responseData(long sequenceNumber, byte[] data) {
			if (SocksWorker.this.socksDataForwarder == null) {
				logger.error("Got response data when socksDataForwarder was still null. Ignoring data!");
				return;
			}

			// Send response data
			SocksWorker.this.socksDataForwarder.sendDataBack(sequenceNumber, data);
		}

		@Override
		public void error(ErrorCode errorCode) {
			if (SocksWorker.this.localNodeCore.hasWebCallback())
				SocksWorker.this.localNodeCore.getWebCallback().error(circuitId,
						String.format("Got error with error code %s", errorCode.name()));
			try {
				SocksWorker.this.close();
			} catch (IOException e) {
				logger.error("Uncaught exception while closing SocksWorker", e);
			}
		}
	}
}
