package common.messages;


public class UploadFileRequest extends AbstractMessage {

    private final String fileName;

    public UploadFileRequest(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }


}
