package common.other;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileCollect {              //  класс для сборки файла из полученных частей
    private Path path;

    public FileCollect(Path path) {
        this.path = path;
    }

    public void collectFile (int size, String rootPrefix, int parts, String filename) throws IOException {
        byte[] bytes = new byte[size];
        String filePartName = "";
        int dimension = (int) (Math.log10(parts) + 1);
        Files.createFile(path);
        FileOutputStream stream = new FileOutputStream(path.toFile());
        // OutputStream для сохранения файла
        for (int i = 0; i < parts; i++) {
            filePartName = String.valueOf(path.getFileName()).concat(Ending.ending(i,dimension));
            String partPathName = rootPrefix + "temp/" + filePartName;
            int length = (int) Files.size(Path.of(partPathName));
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(partPathName));
            //  BufferedInputStream для чтения части
            if (length == size) {
                bis.read(bytes);
                stream.write(bytes);
            } else {            //  обработка ситуации, когда у нас последняя часть меньше предыдущих
                byte[] tail = new byte[length];
                bis.read(tail);
                stream.write(tail);
            }
            bis.close();
            Files.delete(Path.of(partPathName));        //  удаление временных файлов
        }
        Files.delete(Path.of(rootPrefix + "temp/"));    //  удаление временной директории
        stream.flush();
        stream.close();
    }
}
