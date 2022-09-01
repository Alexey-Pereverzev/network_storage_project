package client;

import common.AbstractMessage;
import common.FileMessage;
import common.FileRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    @FXML
    private Label localDriveLabel;
    @FXML
    private Label cloudStorageLabel;
    @FXML
    private Label fileNameCloudLabel;
    @FXML
    private Label fileNameLocalLabel;
    @FXML
    private Label fileSizeCloudLabel;
    @FXML
    private Label fileSizeLocalLabel;
    @FXML
    private Button deleteCloudButton;
    @FXML
    private Button deleteLocalButton;
    @FXML
    private ListView<String> fileListCloud;
    @FXML
    private ListView<String> fileListLocal;
    @FXML
    private ListView<Integer> fileSizesCloud;
    @FXML
    private ListView<Integer> fileSizesLocal;
    @FXML
    private Button refreshCloudButton;
    @FXML
    private Button refreshLocalButton;
    @FXML
    private Button sendCloudButton;
    @FXML
    private Button sendLocalButton;

    private String selectedRecipient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.start();
        showServerFiles();
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = Network.readObject();
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get("client_storage/" + fm.getFilename()), fm.getData(),
                                StandardOpenOption.CREATE);
                        refreshLocalFilesList();
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();
        fileListLocal.setItems(FXCollections.observableArrayList());
        refreshLocalFilesList();
    }

    private void showServerFiles() {
        refreshCloudFilesList();
        fileListCloud.setCellFactory(lv -> {
            MultipleSelectionModel<String> selectionModel = fileListCloud.getSelectionModel();
            ListCell<String> cell = new ListCell<>();
            cell.textProperty().bind(cell.itemProperty());
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                fileListCloud.requestFocus();
                if (!cell.isEmpty()) {
                    int index = cell.getIndex();
                    if (selectionModel.getSelectedIndices().contains(index)) {
                        selectionModel.clearSelection(index);
                        selectedRecipient = null;
                    } else {
                        selectionModel.select(index);
                        selectedRecipient = cell.getItem();
                    }
                    event.consume();
                }
            });
            return cell;
        });
    }

    public void refreshLocalFilesList() {
        if (Platform.isFxApplicationThread()) {
            try {
                fileListLocal.getItems().clear();
                Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> fileListLocal.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Platform.runLater(() -> {
                try {
                    fileListLocal.getItems().clear();
                    Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> fileListLocal.getItems().add(o));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void refreshCloudFilesList() {
        if (Platform.isFxApplicationThread()) {
            try {
                fileListCloud.getItems().clear();
                Files.list(Paths.get("server_storage"))
                        .map(p -> p.getFileName().toString()).forEach(o -> fileListCloud.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Platform.runLater(() -> {
                try {
                    fileListCloud.getItems().clear();
                    Files.list(Paths.get("server_storage"))
                            .map(p -> p.getFileName().toString()).forEach(o -> fileListCloud.getItems().add(o));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @FXML
    void sendToClient(ActionEvent event) {
        String FileName = selectedRecipient;
        if (FileName.length() > 0) {
            Network.sendMsg(new FileRequest(FileName));
 //           FileName.clear();
        }
    }



    @FXML
    void deleteFromCloud(ActionEvent event) {

    }

    @FXML
    void deleteLocalFile(ActionEvent event) {

    }

    @FXML
    void refreshCloudDir(ActionEvent event) {

    }

    @FXML
    void refreshLocalDir(ActionEvent event) {

    }


    @FXML
    void sendToCloud(ActionEvent event) {

    }
}






