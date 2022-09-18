package client;

import common.messages.*;
import common.other.Ending;
import common.other.FileCut;
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
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private Map<Integer, String> selectedFiles;

    private static final int CLIENT_FILE = 0;
    private static final int CLOUD_FILE = 1;
    private static NettyClient nettyClient = ObjectRegistry.getInstance(NettyClient.class);
    public static final String ROOT_PATH = "client_storage";
    public static final String ROOT_PREFIX = ROOT_PATH + "/";


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ObjectRegistry.reg(this.getClass(), this);
        selectedFiles = new HashMap<>(2);
        selectedFiles.put(CLIENT_FILE, "");                 // выбранный файл на стороне клиента, "" - если файл не выбран
        selectedFiles.put(CLOUD_FILE, "");                  // выбранный файл на стороне облака, "" - если файл не выбран
        fileListLocal.setItems(FXCollections.observableArrayList());
        fileListCloud.setItems(FXCollections.observableArrayList());
        showClientFiles();                                 //   показать список файлов клиента
        try {
            showServerFiles();                              //  показать список файлов на сервере
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void showClientFiles() {
        refreshLocalFilesList();                            //  обновить список файлов клиента и их размеров
        cellClickLogic(fileListLocal, CLIENT_FILE);         //  установить фабрику обработки клика на список файлов клиента
    }


    private void showServerFiles() throws IOException, ClassNotFoundException {
        getCloudFilesList();                                //  запросить список файлов с сервера
        cellClickLogic(fileListCloud, CLOUD_FILE);          //  установить фабрику обработки клика на список файлов сервера
    }


    private void cellClickLogic(ListView<String> fileList, Integer side) {
        //  логика работы фабрики кликов на список файлов
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
                        selectedFiles.put(side, "");
                    } else {
                        selectionModel.select(index);
                        selectedFiles.put(side, cell.getItem());
                    }
                    event.consume();
                }
            });
            return cell;
        });
    }

    private void refreshLocal() {                   //  обновление списка локальных файлов - логика
        try {
            fileListLocal.getItems().clear();
            fileSizesLocal.getItems().clear();
            Files.list(Paths.get(ROOT_PATH)).map(p -> p.getFileName().toString())
                    .forEach(o -> fileListLocal.getItems().add(o));
            Files.list(Paths.get(ROOT_PATH)).map(p -> {
                try {
                    return Files.size(p);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }).forEach(o -> fileSizesLocal.getItems().add(Math.toIntExact(o)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void refreshLocalFilesList() {
        //  обновление списка локальных файлов - помещение процедуры в поток JavaFX
        if (Platform.isFxApplicationThread()) {
            refreshLocal();
        } else {
            Platform.runLater(() -> {
                refreshLocal();
            });
        }
    }


    public void getCloudFilesList() {                       //  обновление списка локальных файлов
        if (Platform.isFxApplicationThread()) {
            nettyClient.sendMsg(new CloudFileListRequest());
        } else {
            Platform.runLater(() -> {
                nettyClient.sendMsg(new CloudFileListRequest());
            });
        }
    }

    protected void processRefreshServerMessage(RefreshServerMessage am) {
        // обработка сообщения от сервера со списком файлов - помещение файлов и их размеров на форму
        fileListCloud.getItems().clear();
        fileSizesCloud.getItems().clear();
        RefreshServerMessage rsm = am;
        for (int i = 0; i < rsm.getFileNames().size(); i++) {
            fileListCloud.getItems().add(i, rsm.getFileNames().get(i));
            fileSizesCloud.getItems().add(i, rsm.getFileSizes().get(i));
        }
        selectedFiles.put(CLOUD_FILE, "");
    }

    @FXML
    void sendToClient(ActionEvent event) {                              //  скачать файл с сервера
        String fileName = selectedFiles.get(CLOUD_FILE);
        if (fileName.length() > 0) {
            nettyClient.sendMsg(new FileRequest(fileName));             //  запрос на сервер на скачивание файла
        }
    }

    @FXML
    void sendToCloud(ActionEvent event) throws IOException {        //  отправить файл в облако
        String fileName = selectedFiles.get(CLIENT_FILE);
        if (fileName.length() > 0) {
            Path path = Paths.get(ROOT_PREFIX + fileName);
            if (Files.size(Paths.get((ROOT_PREFIX + fileName))) <= NettyClient.MAX_FILE_SIZE) {
                nettyClient.sendMsg(new FileMessage(path));
            } else {                                        //  если размер файла больше максимального размера куска
                //  отсылаем его по частям
                int parts = new FileCut(path).cutFile(NettyClient.MAX_FILE_SIZE, ROOT_PREFIX);  // режем файл, сохраняем число              //   частей
                int dimension = (int) (Math.log10(parts) + 1);
                Thread t = new Thread(() -> {
                    try {
                        String filePartName = "";
                        for (int i = 0; i < parts; i++) {
                            filePartName = String.valueOf(path.getFileName()).concat(Ending.ending(i, dimension));
                            String partPathName = ROOT_PREFIX + "temp/" + filePartName;
                            Path partPath = Path.of(ROOT_PREFIX + "temp/" + filePartName);
                            int index = i;
                            nettyClient.sendMsg(new FilePartMessage(fileName, index, parts, filePartName, partPath));
                            Files.delete(Path.of(partPathName));           // отправляем часть и удаляем ее
                        }
                        Files.delete(Path.of(ROOT_PREFIX + "temp/"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                t.setDaemon(true);
                t.start();
            }
        }
    }


    @FXML
    void deleteFromCloud(ActionEvent event) {                           //  удалить файл на облаке
        String fileName = selectedFiles.get(CLOUD_FILE);
        if (fileName.length() > 0) {
            nettyClient.sendMsg(new DeleteFileRequest(fileName));       //  запрос на удаление файла с сервера
        }
    }

    @FXML
    void deleteLocalFile(ActionEvent event) {                           //  удаление локального файла
        String fileName = selectedFiles.get(CLIENT_FILE);
        if (fileName.length() > 0) {
            try {
                Files.delete(Paths.get(ROOT_PREFIX + fileName));
                refreshLocalFilesList();
                selectedFiles.put(CLIENT_FILE, "");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}






