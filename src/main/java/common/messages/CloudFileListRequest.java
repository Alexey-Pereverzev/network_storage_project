package common.messages;


public class CloudFileListRequest extends AbstractMessage {             //  запрос списка файлов по относительному пути

    private final String currentCloudPath;

    public CloudFileListRequest(String currentCloudPath) {

        this.currentCloudPath = currentCloudPath;
    }

    public String getCurrentCloudPath() {
        return currentCloudPath;
    }
}
