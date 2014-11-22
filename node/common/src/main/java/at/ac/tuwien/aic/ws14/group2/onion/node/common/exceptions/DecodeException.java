package at.ac.tuwien.aic.ws14.group2.onion.node.common.exceptions;

/**
 * Created by Thomas on 09.11.2014.
 */
public class DecodeException extends CellException {
    public DecodeException() {
    }

    public DecodeException(String msg) {
        super(msg);
    }

    public DecodeException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public DecodeException(Throwable cause) {
        super(cause);
    }
}
