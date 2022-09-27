package common.messages;


public class DeleteFileRequest extends AbstractMessage {                    //  запрос на удаление файла на сервере
    private final String filename;

    public String getFilename() {
        return filename;
    }

    public DeleteFileRequest(String filename) {
        this.filename = filename;
    }
}
