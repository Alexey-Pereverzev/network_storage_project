package client;

import common.messages.CreateDirMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MakeDirController {                        // контроллер окна создания директорий

    private ClientApplication clientApplication;
    private ClientController clientController;
    private static final int CLIENT_FILE = 0;
    private static final int CLOUD_FILE = 1;

    @FXML
    private TextField dirNameField;

    private int side;


    @FXML
    void createDir() {
        String s = dirNameField.getText();
        try {
            if (side == CLIENT_FILE) {
                createLocalDir(s);
            } else {
                Platform.runLater(() -> createCloudDir(s));
            }
        } catch (IOException e) {
            clientApplication.showErrorAlert("Ошибка", "Невозможно создать директорию " + s);
        }
        clientApplication.getMakeDirStage().close();


    }

    private void createCloudDir(String s) {                                 //  создание папки облака
        NettyClient nettyClient = ObjectRegistry.getInstance(NettyClient.class);
        nettyClient.sendMsg(new CreateDirMessage(s));                       //  запрос на создание папки
    }

    private void createLocalDir(String s) throws IOException {              //  создание папки на клиенте
        String currentClientPath = clientController.getCurrentClientPath();
        String currentSuffix = currentClientPath.equals("") ? "" : (currentClientPath + "/");
        Files.createDirectories(Path.of(ClientController.ROOT_PREFIX + currentSuffix + s));
        clientController.refreshLocalFilesList();
    }

    public void setStartClient(ClientApplication clientApplication, int side) {
        this.clientApplication = clientApplication;
        this.clientController = ObjectRegistry.getInstance(ClientController.class);
        this.side = side;
        ObjectRegistry.reg(MakeDirController.class, this);
    }

}
