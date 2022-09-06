package client;

import common.*;
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
import java.util.HashMap;
import java.util.Map;
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
    private Button sendCloudButton;
    @FXML
    private Button sendLocalButton;

    private Map<Integer,String> selectedFiles;

    private static final int CLIENT_FILE = 0;
    private static final int CLOUD_FILE = 1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.start();
        showClientFiles();
        showServerFiles();
        selectedFiles = new HashMap<>(2);
        selectedFiles.put(CLIENT_FILE,"");                 // выбранный файл на стороне клиента, "" - если файл не выбран
        selectedFiles.put(CLOUD_FILE,"");                  // выбранный файл на стороне облака, "" - если файл не выбран
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
                    if (am instanceof RefreshServerMessage) {
                        refreshCloudFilesList();
                        selectedFiles.put(CLOUD_FILE,"");
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
        fileListCloud.setItems(FXCollections.observableArrayList());
        refreshLocalFilesList();
        refreshCloudFilesList();
    }

    private void showServerFiles() {
        refreshCloudFilesList();
        cellClickLogic(fileListCloud, CLOUD_FILE);
    }

    private void showClientFiles() {
        refreshLocalFilesList();
        cellClickLogic(fileListLocal, CLIENT_FILE);
    }

    private void cellClickLogic(ListView<String> fileList, Integer side) {
        fileList.setCellFactory(lv -> {
            MultipleSelectionModel<String> selectionModel = fileList.getSelectionModel();
            ListCell<String> cell = new ListCell<>();
            cell.textProperty().bind(cell.itemProperty());
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                fileList.requestFocus();
                if (!cell.isEmpty()) {
                    int index = cell.getIndex();
                    if (selectionModel.getSelectedIndices().contains(index)) {
                        selectionModel.clearSelection(index);
                        selectedFiles.put(side,"");
                    } else {
                        selectionModel.select(index);
                        selectedFiles.put(side,cell.getItem());
                    }
                    event.consume();
                }
            });
            return cell;
        });
    }


    private void refreshFilesList(ListView<String> fileList, ListView<Integer> fileSizes, String storage) {
        try {
            fileList.getItems().clear();
            fileSizes.getItems().clear();
            Files.list(Paths.get(storage)).map(p -> p.getFileName().toString()).forEach(o -> fileList.getItems().add(o));
            Files.list(Paths.get(storage)).map(p -> {
                try {
                    return Files.size(p);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }).forEach(o -> fileSizes.getItems().add(Math.toIntExact(o)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void refreshLocalFilesList() {
        if (Platform.isFxApplicationThread()) {
            refreshFilesList(fileListLocal, fileSizesLocal,"client_storage");
        } else {
            Platform.runLater(() -> {
                refreshFilesList(fileListLocal, fileSizesLocal, "client_storage");
            });
        }
    }


    public void refreshCloudFilesList() {
        if (Platform.isFxApplicationThread()) {
            refreshFilesList(fileListCloud, fileSizesCloud, "server_storage");
        } else {
            Platform.runLater(() -> {
                refreshFilesList(fileListCloud, fileSizesCloud, "server_storage");
            });
        }
    }

    @FXML
    void sendToClient(ActionEvent event) {
        String fileName = selectedFiles.get(CLOUD_FILE);
        if (fileName.length()>0) {
            Network.sendMsg(new FileRequest(fileName));
        }
    }

    @FXML
    void sendToCloud(ActionEvent event) throws IOException {
        String fileName = selectedFiles.get(CLIENT_FILE);
        if (fileName.length()>0) {
            Network.sendMsg(new FileMessage(Paths.get("client_storage/" + fileName)));
        }

    }



    @FXML
    void deleteFromCloud(ActionEvent event) {
        String fileName = selectedFiles.get(CLOUD_FILE);
        if (fileName.length()>0) {
            Network.sendMsg(new DeleteFileRequest(fileName));
        }
    }

    @FXML
    void deleteLocalFile(ActionEvent event) {
        String fileName = selectedFiles.get(CLIENT_FILE);
        if (fileName.length()>0) {
            try {
                Files.delete(Paths.get("client_storage/" + fileName));
                refreshLocalFilesList();
                selectedFiles.put(CLIENT_FILE,"");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }





}






