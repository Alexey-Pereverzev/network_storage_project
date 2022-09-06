package common;


public class RefreshServerMessage extends AbstractMessage {
    private String filename;

    public String getFilename() {
        return filename;
    }

    public RefreshServerMessage(String filename) {
        this.filename = filename;
    }
}


