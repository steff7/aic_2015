package at.ac.tuwien.aic.ws14.group2.onion.node.common.cells;

import java.nio.ByteBuffer;

/**
 * Created by Thomas on 10.11.2014.
 */
public class DestroyCell extends Cell {
    /**
     * Creates a Destroy Cell without setting the Cell header.
     * Cell type and circuit ID will not be set.
     */
    DestroyCell() {
    }

    public DestroyCell(short circuitID) {
        super(CELL_TYPE_DESTROY, circuitID);
    }

    @Override
    protected void encodePayload(ByteBuffer buffer) {
    }
}
