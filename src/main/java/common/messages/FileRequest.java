package common.messages;


public class FileRequest extends AbstractMessage {              //  запрос на загрузку файла с облака
    private final String filename;

    public String getFilename() {
        return filename;
    }

    public FileRequest(String filename) {
        this.filename = filename;
    }

}
