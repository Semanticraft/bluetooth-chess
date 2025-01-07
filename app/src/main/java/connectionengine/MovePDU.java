package connectionengine;

import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * This PDU serves to transmit information about the player's half move.
 */
class MovePDU implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int[][] newState;
    private int endCondition;

    MovePDU(int[][] newState, int endCondition) {
        this.newState = newState;
        this.endCondition = endCondition;
    }

    public int[][] getNewState() {
        return newState;
    }

    public int getEndCondition() {
        return endCondition;
    }

    @Serial
    private void writeObject(@NotNull ObjectOutputStream out) throws IOException {
        out.writeObject(newState);
        out.writeInt(endCondition);
    }

    @Serial
    private void readObject(@NotNull ObjectInputStream in) throws IOException, ClassNotFoundException {
        newState = (int[][]) in.readObject();
        endCondition = in.readInt();
    }
}

