package at.ac.tuwien.aic.ws14.group2.onion.node.common;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * Created by Thomas on 09.11.2014.
 */
public class Cell {
    private short circuitID;

    static final int CELL_BYTES = 512;
    static final int CELL_HEADER_BYTES = 3;   // sizeof(circuitID) + sizeof(cellType)
    static final int CELL_PAYLOAD_BYTES = CELL_BYTES - CELL_HEADER_BYTES;

    static final int DIFFIE_HELLMAN_HALF_BYTES = 128;   // TODO: dummy value
    static final int SIGNATURE_BYTES = 64;              // TODO: dummy value

    static final byte CELL_TYPE_CREATE = 0;
    static final byte CELL_TYPE_CREATE_RESPONSE = 1;
//    static final byte CELL_TYPE_DESTROY = 2;
//    static final byte CELL_TYPE_DESTROY_RESPONSE = 3;
    static final byte CELL_TYPE_RELAY = 4;

    public static Cell receive(InputStream source) throws IOException {
        DataInputStream input = new DataInputStream(source);

        byte[] packet = new byte[CELL_BYTES];
        input.readFully(packet);

        ByteBuffer buffer = ByteBuffer.wrap(packet);

        short circuitID = buffer.getShort();
        byte cellType = buffer.get();

        Cell cell;
        switch (cellType) {
            case CELL_TYPE_CREATE:
                cell = new CreateCell(buffer);
                break;
            case CELL_TYPE_CREATE_RESPONSE:
                cell = new CreateResponseCell(buffer);
                break;
            case CELL_TYPE_RELAY:
                cell = new RelayCell(buffer);
                break;
            default:
                return null;
        }

        cell.circuitID = circuitID;

        return cell;
    }

    public void send(OutputStream destination) {

    }
}
