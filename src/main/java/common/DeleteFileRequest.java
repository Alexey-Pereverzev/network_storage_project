package common;


public class DeleteFileRequest extends AbstractMessage {
    private String filename;

    public String getFilename() {
        return filename;
    }

    public DeleteFileRequest(String filename) {
        this.filename = filename;
    }
}
