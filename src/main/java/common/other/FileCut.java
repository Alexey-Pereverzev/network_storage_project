package common.other;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.size;


public record FileCut(Path path) {              //  класс для нарезки файла на части

    public int cutFile(int size, String rootPrefix, String currentClientPath) throws IOException {
        //  параметры - размер части и путь до папки с файлом; возвращаем число частей
        byte[] bytes = new byte[size];
        int dimension = (int) (Math.log10(((float) size(path) / size)) + 1);
        int partIndex = 0;
        String filePartName;
        FileInputStream stream = new FileInputStream(path.toFile());        //  InputStream читает файл
        int readBytes;
        String tempPrefix = rootPrefix + "temp/" + currentClientPath + "/";
        createDirectories(Paths.get(tempPrefix));
        while ((readBytes = stream.read(bytes)) != -1) {
            filePartName = String.valueOf(path.getFileName()).concat(Ending.ending(partIndex, dimension));
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempPrefix + filePartName));
            //  BufferedOutputStream записывает часть файла в отдельный файл
            if (readBytes < size) {             //  отдельно обрабатываем ситуацию с "хвостиком" меньшего размера
                byte[] tail = new byte[readBytes];
                System.arraycopy(bytes, 0, tail, 0, tail.length);
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
