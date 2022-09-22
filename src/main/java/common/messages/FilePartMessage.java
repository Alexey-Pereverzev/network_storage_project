package common.messages;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilePartMessage extends AbstractMessage {
    private String fileName;        //  имя файла, который мы отправляем
    private int index;              //  номер части
    private int parts;              //  общее число частей
    private String filePartName;    //  имя куска файла
    private byte[] data;            //  данные куска

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
