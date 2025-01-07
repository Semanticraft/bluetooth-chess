package ui.connectionacceptance;

public class ConnectionRequest {
    private final long enemyID;
    private final int[][] state;

    public ConnectionRequest(long enemyID, int[][] state) {
        this.enemyID = enemyID;
        this.state = state;
    }

    public long getEnemyID() {
        return enemyID;
    }

    public int[][] getState() {
        return state;
    }
}
