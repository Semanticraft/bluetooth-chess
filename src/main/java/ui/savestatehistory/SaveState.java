package ui.savestatehistory;

public class SaveState {
    private final long id;
    private final String timestamp;

    public SaveState(long id, String timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
