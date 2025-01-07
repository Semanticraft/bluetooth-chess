package connectionengine;

import java.io.Serial;
import java.io.Serializable;

/**
 * This PDU serves to transmit information about the player ID.
 */
class PlayerIDPDU implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final long playerID;

    PlayerIDPDU(long playerID) {
        this.playerID = playerID;
    }

    public long getPlayerID() {
        return playerID;
    }
}

