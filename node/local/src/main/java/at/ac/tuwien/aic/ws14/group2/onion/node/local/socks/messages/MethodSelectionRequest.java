package at.ac.tuwien.aic.ws14.group2.onion.node.local.socks.messages;

import at.ac.tuwien.aic.ws14.group2.onion.node.local.socks.exceptions.MessageParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * Created by klaus on 11/11/14.
 */
public class MethodSelectionRequest extends SocksMessage {
	private static final Logger logger = LoggerFactory.getLogger(MethodSelectionRequest.class.getName());

	private final Method[] methods;

	public MethodSelectionRequest(Method... methods) {
		this.methods = Objects.requireNonNull(methods);

		if (methods.length > 0xFF)
			throw new IllegalArgumentException("no more than " + 0xFF + " methods allowed");
	}

	/**
	 * Parses a byte array to a new instance of this class.
	 *
	 * @throws MessageParsingException  if the data cannot be parsed because it doesn't match the RFC 1928 specification
	 * @throws BufferUnderflowException if the byte array provided is shorter than the expected length
	 */
	public static MethodSelectionRequest fromByteArray(byte[] data) throws MessageParsingException, BufferUnderflowException {
		Objects.requireNonNull(data);

		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.order(SocksMessage.NETWORK_BYTE_ORDER);

		byte version = bb.get();
		if (version != SocksMessage.VERSION)
			throw new MessageParsingException(String.format("wrong version byte: expected: 0x%02X; found: 0x%02X", SocksMessage.VERSION, version));

		int length = Byte.toUnsignedInt(bb.get());
		byte[] methodsBytes = new byte[length];
		bb.get(methodsBytes);

		Method[] methods = new Method[length];

		// Convert the bytes to the enumeration
		for (int i = 0; i < methodsBytes.length; i++) {
			try {
				methods[i] = Method.fromByte(methodsBytes[i]);
			} catch (IllegalArgumentException e) {
				methods[i] = Method.UNKNOWN_METHOD;
				logger.debug(e.getMessage());
			}
		}

		return new MethodSelectionRequest(methods);
	}

	public Method[] getMethods() {
		return methods;
	}

	@Override
	public byte[] toByteArray() {

		ByteBuffer bb = ByteBuffer.allocate(1 /* VER */ + 1 /* NMETHODS */ + methods.length);

		bb.put(SocksMessage.VERSION);
		bb.put((byte) methods.length);

		for (Method m : methods) {
			bb.put(m.getValue());
		}

		return bb.array();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MethodSelectionRequest that = (MethodSelectionRequest) o;

		if (!Arrays.equals(methods, that.methods)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return methods != null ? Arrays.hashCode(methods) : 0;
	}
}
