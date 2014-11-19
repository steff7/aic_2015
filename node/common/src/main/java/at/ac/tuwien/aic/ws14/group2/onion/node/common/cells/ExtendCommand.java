package at.ac.tuwien.aic.ws14.group2.onion.node.common.cells;

import java.net.*;
import java.nio.ByteBuffer;

/**
 * Created by Thomas on 09.11.2014.
 */
public class ExtendCommand extends Command {

    private Inet4Address target;
    private byte[] encryptedDiffieHalf;

    /**
     * Reads a Command assuming that the Command Type field has already been read.
     * The Command type will not be set.
     */
    ExtendCommand(ByteBuffer buffer) {
        try {
            byte[] ip = new byte[4];
            buffer.get(ip);
            target = (Inet4Address)Inet4Address.getByAddress(ip);
        } catch (UnknownHostException ex) {
            // IP address cannot be of invalid length.
        }

        encryptedDiffieHalf = new byte[Cell.DIFFIE_HELLMAN_HALF_BYTES];
        buffer.get(encryptedDiffieHalf);
    }

    public ExtendCommand(Inet4Address target, byte[] encryptedDiffieHalf) {
        super(COMMAND_TYPE_EXTEND);

        this.target = target;
        this.encryptedDiffieHalf = encryptedDiffieHalf;
    }

    public InetAddress getTarget() {
        return target;
    }

    public byte[] getDiffieHellmanHalf(byte[] privateKey) {
        // TODO: decrypt
        return encryptedDiffieHalf;
    }

    @Override
    protected void encodePayload(ByteBuffer buffer) {
        buffer.put(target.getAddress());
        buffer.put(encryptedDiffieHalf);
    }
}