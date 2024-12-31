package connectionengine;

import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * This PDU serves to transmit information about the saved state. If the saved state is empty, a new
 * game will be started.
 */
class StartPDU implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int[][] savedState = new int[][]{};
    private long enemyID;

    StartPDU(int[][] savedState, long enemyID) {
        if (savedState != null) this.savedState = savedState;
        this.enemyID = enemyID;
    }

    StartPDU() {}

    public int[][] getSavedState() {
        return savedState;
    }

    public long getEnemyID() {
        return enemyID;
    }

    @Serial
    private void writeObject(@NotNull ObjectOutputStream out) throws IOException {
        out.writeObject(savedState);
        out.writeObject(enemyID);
    }

    @Serial
    private void readObject(@NotNull ObjectInputStream in) throws IOException, ClassNotFoundException {
        savedState = (int[][]) in.readObject();
        enemyID = (long) in.readLong();
    }

}
