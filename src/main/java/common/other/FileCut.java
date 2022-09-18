package common.other;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class FileCut {              //  класс для нарезки файла на части
    private Path path;

    public FileCut(Path path) {
        this.path = path;
    }

    public int cutFile(int size, String rootPrefix) throws IOException {       //  параметры - размер части и путь до
                                                                                // папки с файлом
                                                                                // возвращаем число частей
        byte[] bytes = new byte[size];
        int dimension = (int) (Math.log10((Files.size(path) / size)) + 1);
        int partIndex = 0;
        String filePartName = "";
        FileInputStream stream = new FileInputStream(path.toFile());        //  InputStream читает файл
        int readBytes;
        Files.createDirectories(Paths.get(rootPrefix + "temp/"));
        while ((readBytes = stream.read(bytes)) != -1) {
            filePartName = String.valueOf(path.getFileName()).concat(Ending.ending(partIndex, dimension));
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(rootPrefix + "temp/" + filePartName));
            //  BufferedOutputStream записывает часть файла в отдельный файл
            if (readBytes < size) {             //  отдельно обрабатываем ситуацию с "хвостиком" меньшего размера
                byte[] tail = new byte[readBytes];
                for (int i = 0; i < tail.length; i++) {
                    tail[i] = bytes[i];
                }
                bos.write(tail);
            } else {
                bos.write(bytes);
            }
            bos.flush();
            bos.close();
            partIndex++;
        }
        stream.close();
        return partIndex;
    }

}
