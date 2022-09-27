package common.messages;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMessage extends AbstractMessage {              //  отправка сообщения с файлом небольшого размера
    private final String filename;
    private final byte[] data;
    private int size;


    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }


    public FileMessage(Path path) throws IOException {
        boolean exists = Files.exists(path);
        filename = path.getFileName().toString();
        data = Files.readAllBytes(path);
        if (exists) {
            size = (int) Files.size(path);
        }
    }

    public int getSize() {
        return size;
    }

}
