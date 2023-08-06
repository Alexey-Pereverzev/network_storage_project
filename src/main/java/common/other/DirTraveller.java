package common.other;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class DirTraveller {                         //  служебный класс для обхода папок сервера/клиента)

    public static int getUsedSpace(String iteratorPath) throws IOException {
        //  рекурсивный алгоритм для подсчета занятого места в определенной папке
        int usedSpace = 0;
        List<Path> collect = Files.list(Paths.get(iteratorPath)).collect(Collectors.toList());
        for (Path path : collect) {
            if (!Files.isDirectory(path)) {
                usedSpace += Files.size(path);
            } else {
                usedSpace += getUsedSpace(iteratorPath + "/" + path.getFileName());
            }
        }
        return usedSpace;
    }


    public static boolean cleanUpDir(String rootTempDir) throws IOException {
        //  рекурсивная функция для зачистки пустых служебных папок, оставщихся после отправки/сборки частей файлов
        List<Path> collect = Files.list(Paths.get(rootTempDir)).collect(Collectors.toList());
        boolean emptyLevel = true;
        for (Path path : collect) {                         //  проверяем, есть ли в папке по крайней мере один файл
            if ((!Files.isDirectory(path))) {
                emptyLevel = false;
            } else {
                emptyLevel = emptyLevel && cleanUpDir(String.valueOf(path));
            }
        }
        if (emptyLevel) {               //  если таких нет, папку можно удалить
            Files.delete(Path.of(rootTempDir));
            return true;
        } else {
            return false;
        }
    }


}
