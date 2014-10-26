package at.ac.tuwien.aic.ws14.group2.onion.directory;

import static org.junit.Assert.*;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.*;
import org.junit.Test;
import at.ac.tuwien.aic.ws14.group2.onion.common.service.DirectoryService;

/**
 * Created by Thomas on 26.10.2014.
 */
public class ThriftTest {

    private static final int THRIFT_PORT = 9090;

    private static class ServiceImpl implements DirectoryService.Iface {

        public volatile boolean called;

        @Override
        public void ping() throws TException {
            called = true;
        }
    }

    @Test
    public void cleartext() throws TException, InterruptedException {

        // Server Code

        ServiceImpl handler = new ServiceImpl();
        DirectoryService.Processor<DirectoryService.Iface> processor = new DirectoryService.Processor<>(handler);

        TServerTransport serverTransport = new TServerSocket(9090);
        TServer server = new TSimpleServer(new TServer.Args(serverTransport).processor(processor));

        Runnable serverMethod = new Runnable() {
            @Override
            public void run() {
                System.out.println("server started");
                server.serve();
                System.out.println("server finished");
            }
        };

        Thread serverThread = new Thread(serverMethod);
        serverThread.start();


        // Client Code

        TTransport transport = new TSocket("localhost", THRIFT_PORT);
        transport.open();

        TProtocol protocol = new TBinaryProtocol(transport);
        DirectoryService.Client client = new DirectoryService.Client(protocol);

        assertEquals(false, handler.called);
        client.ping();
        assertEquals(true, handler.called);

        transport.close();

        server.stop();
        serverThread.join();
    }

    @Test
    public void secure() {
        // TODO
    }
}
