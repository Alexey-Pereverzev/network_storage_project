package common.other;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record FileCollect(Path path) {              //  класс для сборки файла из полученных частей

    public void collectFile(int size, String rootPrefix, String currentSuffix, int parts) throws IOException {
        byte[] bytes = new byte[size];
        String filePartName;
        int dimension = (int) (Math.log10(parts) + 1);
        Files.deleteIfExists(path);
        Files.createFile(path);
        FileOutputStream stream = new FileOutputStream(path.toFile());
        // OutputStream для сохранения файла
        String tempPrefix = rootPrefix + "temp/" + currentSuffix;
        for (int i = 0; i < parts; i++) {
            filePartName = String.valueOf(path.getFileName()).concat(Ending.ending(i, dimension));
            String partPathName = tempPrefix + filePartName;
            int length = (int) Files.size(Path.of(partPathName));
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(partPathName));
            //  BufferedInputStream для чтения части
            if (length == size) {
                if (bis.read(bytes) != -1) {
                    stream.write(bytes);
                }
            } else {            //  обработка ситуации, когда у нас последняя часть меньше предыдущих
                byte[] tail = new byte[length];
                if (bis.read(tail) != -1) {
                    stream.write(tail);
                }
            }
            bis.close();
            Files.delete(Path.of(partPathName));        //  удаление временных файлов
        }

        DirTraveller.cleanUpDir(rootPrefix + "temp");

        stream.flush();
        stream.close();
    }
}
