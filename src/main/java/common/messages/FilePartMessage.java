package common.messages;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilePartMessage extends AbstractMessage {                  //  отправка сообщения с частью файла
    private final String fileName;        //  имя файла, который мы отправляем
    private final int index;              //  номер части
    private final int parts;              //  общее число частей
    private final String filePartName;    //  имя куска файла
    private final byte[] data;            //  данные куска

    public byte[] getData() {
        return data;
    }

    public String getFilePartName() {
        return filePartName;
    }

    public String getFileName() {
        return fileName;
    }

    public int getIndex() {
        return index;
    }

    public int getParts() {
        return parts;
    }

    public FilePartMessage(String fileName, int index, int parts, String filePartName, Path path) throws IOException {
        this.fileName = fileName;
        this.index = index;
        this.parts = parts;
        this.filePartName = filePartName;
        this.data = Files.readAllBytes(path);
    }

}
