package connectionengine;

import java.io.Serial;
import java.io.Serializable;

/**
 * This PDU serves to transmit information about the player's request for a draw.
 */
class DrawPDU implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    // false == request, true == acceptance
    private boolean drawFlag;

    DrawPDU(boolean drawFlag) {
        this.drawFlag = drawFlag;
    }

    public boolean isDrawFlagActive() {
        return drawFlag;
    }
}

