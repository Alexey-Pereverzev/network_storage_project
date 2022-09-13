package common.messages;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMessage extends AbstractMessage {
    private String filename;
    private byte[] data;
    private boolean exists;
    private int size;


    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }


    public FileMessage(Path path) throws IOException {
        exists = Files.exists(path);
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
