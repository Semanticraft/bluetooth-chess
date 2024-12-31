package connectionengine;

import java.io.Serial;
import java.io.Serializable;

/**
 * This PDU serves to transmit information about errors during connection.
 */
class ErrorPDU implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String type;
    private String message;

    ErrorPDU(String type, String message) {
        this.type = type;
        this.message = message;
    }
    
    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}

