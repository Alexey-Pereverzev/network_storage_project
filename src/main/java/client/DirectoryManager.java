package client;

import common.other.SortFiles;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DirectoryManager {                         //  класс для работы со структурой директорий
    private String currentClientPath;                   //  относительный текущий путь клиента
    private String fullClientPath;                      //  полный текущий путь клиента

    public DirectoryManager(String rootPath) {
        this.currentClientPath = "";
        this.fullClientPath = rootPath;
    }

    public void setCurrentClientPath(String currentClientPath, String rootPath) {
        this.currentClientPath = currentClientPath;
        this.fullClientPath = rootPath + "/" + currentClientPath;
    }

    protected void refreshLocal(ListView<String> fileListLocal, ListView<String> fileSizesLocal) {
        //  обновление списка локальных файлов - логика
        try {
            fileListLocal.getItems().clear();
            fileSizesLocal.getItems().clear();
            Files.list(Paths.get(fullClientPath)).map(p -> p.getFileName().toString())
                    .forEach(o -> fileListLocal.getItems().add(o));
            Files.list(Paths.get(fullClientPath)).map(p -> {
                try {
                    if (Files.isDirectory(p)) {
                        return "";
                    } else {
                        return String.valueOf(Files.size(p));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }).forEach(o -> fileSizesLocal.getItems().add(o));
            SortFiles.sortFiles(fileListLocal, fileSizesLocal);
            if (!currentClientPath.equals("")) {
                fileListLocal.getItems().add(0, "/...");
                fileSizesLocal.getItems().add(0, "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clientEnter(String currentClientPath, String rootPath) {            //  вход внутрь папки клиента
        this.currentClientPath = currentClientPath;
        this.fullClientPath = rootPath + "/" + currentClientPath;
        ClientController cc = ObjectRegistry.getInstance(ClientController.class);
        cc.refreshLocalFilesList();
    }

    public void cloudEnter(String currentCloudPath) {                           //  запрос на вход внутрь папки на сервере
        ClientController cc = ObjectRegistry.getInstance(ClientController.class);
        cc.getCloudFilesList(currentCloudPath);
    }

}


