package client;

import common.messages.*;
import common.other.DirTraveller;
import common.other.Ending;
import common.other.FileCut;
import common.other.SortFiles;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    @FXML
    private ListView<String> fileListCloud;
    @FXML
    private ListView<String> fileListLocal;
    @FXML
    private ListView<String> fileSizesCloud;
    @FXML
    private ListView<String> fileSizesLocal;

    @FXML
    private Label cloudStorageLabel;

    private Map<Integer, String> selectedFiles;

    private static final int CLIENT_FILE = 0;
    private static final int CLOUD_FILE = 1;
    private static final NettyClient nettyClient = ObjectRegistry.getInstance(NettyClient.class);
    public static final String ROOT_PATH = "client_storage";
    public static final String ROOT_PREFIX = ROOT_PATH + "/";
    private String currentClientPath;                           //  текущая папка клиента относительно корня
    private ArrayList<String> parentClientPath;                 //  путь от текущей папки до корня на клиенте (стек)
    private String currentCloudPath;                            //  текущая папка сервера относительно корня
    private ArrayList<String> parentCloudPath;                  //  путь от текущей папки до корня на сервере (стек)
    private int serverQuote = 0;                                //  дисковая квота (нам ее передаст сервер)
    private int usedServerSpace;                                //  сколько занято на сервере
    private DirectoryManager directoryManager;                  //  класс переключения директорий

    public String getCurrentClientPath() {
        return currentClientPath;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ObjectRegistry.reg(this.getClass(), this);
        directoryManager = new DirectoryManager(ROOT_PATH);
        ObjectRegistry.reg(DirectoryManager.class, directoryManager);
        selectedFiles = new HashMap<>(2);
        selectedFiles.put(CLIENT_FILE, "");                 // выбранный файл на стороне клиента, "" - если файл не выбран
        selectedFiles.put(CLOUD_FILE, "");                  // выбранный файл на стороне облака, "" - если файл не выбран
        fileListLocal.setItems(FXCollections.observableArrayList());
        fileListCloud.setItems(FXCollections.observableArrayList());
        currentClientPath = "";
        currentCloudPath = "";
        parentClientPath = new ArrayList<>();
        parentCloudPath = new ArrayList<>();
    }

    protected void showClientFiles() throws IOException {
        if (Files.notExists(Path.of(ROOT_PATH))) {
            Files.createDirectories(Path.of(ROOT_PATH));
        }
        refreshLocalFilesList();                            //  обновить список файлов клиента и их размеров
//        cellClickLogic(fileListLocal, fileSizesLocal, CLIENT_FILE);
        cellClickLogicClient(fileListLocal);         //  установить фабрику обработки клика на список файлов клиента
    }


    protected void showServerFiles() throws IOException, ClassNotFoundException {
        getCloudFilesList(currentCloudPath);                                //  запросить список файлов с сервера
//        cellClickLogic(fileListCloud, fileSizesCloud, CLOUD_FILE);
        cellClickLogicCloud(fileListCloud);          //  установить фабрику обработки клика на список файлов сервера
    }

    private void cellClickLogicCloud(ListView<String> fileList) {           //  фабрика работы со списком файлов облака
        fileList.setCellFactory(lv -> {
            MultipleSelectionModel<String> selectionModel = fileList.getSelectionModel();
            ListCell<String> cell = new ListCell<>();
            cell.textProperty().bind(cell.itemProperty());
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {        // выделение файла для отправки / удаления
                fileList.requestFocus();
                if (!cell.isEmpty()) {
                    int index = cell.getIndex();
                    if (selectionModel.getSelectedIndices().contains(index)) {
                        selectionModel.clearSelection(index);
                        selectedFiles.put(CLOUD_FILE, "");
                    } else {
                        selectionModel.select(index);
                        selectedFiles.put(CLOUD_FILE, cell.getItem());
                    }
                    event.consume();
                }
            });
            cell.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {    //  заход внутрь директории двойным кликом
                fileList.requestFocus();
                if (event.getClickCount() == 2) {
                    if (!cell.isEmpty()) {
                        String s = cell.getItem();

                        if (s.equals("/...")) {                         //  выход из директории на уровень выше
                            currentCloudPath = parentCloudPath.get(0);
                            parentCloudPath.remove(0);
                            getCloudFilesList(currentCloudPath);        //  запрос у сервера списка файлов верхней директории
                        } else {
                            if (isDirectory(s, fileList, fileSizesCloud)) {     // если файл - директория
                                parentCloudPath.add(0, currentCloudPath);       //  сохраняем текущую папку в стек переходов
                                if (currentCloudPath.equals("")) {
                                    currentCloudPath = s;
                                } else {
                                    currentCloudPath = currentCloudPath + "/" + s;
                                }
                                directoryManager.cloudEnter(currentCloudPath);      // заходим внутрь
                            }
                        }
                    }
                    event.consume();
                }
            });
            return cell;
        });
    }


    private void cellClickLogicClient(ListView<String> fileList) {          //  фабрика работы со списком файлов клиента
        // очень хотел унифицировать предыдущий и текущий методы, но ничего из этого не вышло, поэтому сорри, будет второй кусок
        // почти такого же кода. Внутрь лямбды мне просто историю переходов никак не получилось пробросить

        fileList.setCellFactory(lv -> {
            MultipleSelectionModel<String> selectionModel = fileList.getSelectionModel();
            ListCell<String> cell = new ListCell<>();
            cell.textProperty().bind(cell.itemProperty());
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {        // выделение файла для отправки / удаления
                fileList.requestFocus();
                if (!cell.isEmpty()) {
                    int index = cell.getIndex();
                    if (selectionModel.getSelectedIndices().contains(index)) {
                        selectionModel.clearSelection(index);
                        selectedFiles.put(CLIENT_FILE, "");
                    } else {
                        selectionModel.select(index);
                        selectedFiles.put(CLIENT_FILE, cell.getItem());
                    }
                    event.consume();
                }
            });
            cell.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {            //  заход внутрь директории двойным кликом
                fileList.requestFocus();
                if (event.getClickCount() == 2) {
                    if (!cell.isEmpty()) {
                        String s = cell.getItem();
                        if (s.equals("/...")) {                                 //  выход из директории на уровень выше
                            currentClientPath = parentClientPath.get(0);
                            parentClientPath.remove(0);
                            directoryManager.setCurrentClientPath(currentClientPath, ROOT_PATH);
                            refreshLocalFilesList();                            //  обновление файлов клиента
                        } else {
                            if (isDirectory(s, fileList, fileSizesLocal)) {      // если файл - директория
                                parentClientPath.add(0, currentClientPath);     //  сохраняем текущую папку в стек переходов
                                if (currentClientPath.equals("")) {
                                    currentClientPath = s;
                                } else {
                                    currentClientPath = currentClientPath + "/" + s;
                                }
                                directoryManager.clientEnter(currentClientPath, ROOT_PATH);     // заходим внутрь
                            }
                        }
                    }
                    event.consume();
                }
            });
            return cell;
        });
    }




    public void refreshLocalFilesList() {
        //  обновление списка локальных файлов - помещение процедуры в поток JavaFX
        if (Platform.isFxApplicationThread()) {
            directoryManager.refreshLocal(fileListLocal, fileSizesLocal);
        } else {
            Platform.runLater(() -> directoryManager.refreshLocal(fileListLocal, fileSizesLocal));
        }
    }


    public void getCloudFilesList(String currentCloudPath) {                       //  обновление списка локальных файлов
        if (Platform.isFxApplicationThread()) {
            nettyClient.sendMsg(new CloudFileListRequest(currentCloudPath));
        } else {
            Platform.runLater(() -> nettyClient.sendMsg(new CloudFileListRequest(currentCloudPath)));
        }
    }

    protected void processRefreshServerMessage(ListOfServerFiles losf) {
        // обработка сообщения от сервера со списком файлов - помещение файлов и их размеров на форму
        fileListCloud.getItems().clear();
        fileSizesCloud.getItems().clear();
        for (int i = 0; i < losf.getFileNames().size(); i++) {
            fileListCloud.getItems().add(i, losf.getFileNames().get(i));
            fileSizesCloud.getItems().add(i, losf.getFileSizes().get(i));
        }
        SortFiles.sortFiles(fileListCloud, fileSizesCloud);         //  сортировка - папки должны отображаться выше, файлы ниже
        if (!currentCloudPath.equals("")) {
            fileListCloud.getItems().add(0, "/...");
            fileSizesCloud.getItems().add(0, "");
        }
        serverQuote = losf.getServerQuote();
        usedServerSpace = losf.getUsedServerSpace();
        selectedFiles.put(CLOUD_FILE, "");
        cloudStorageLabel.setText("Cloud Storage - " + (serverQuote - usedServerSpace) + "b free");
        //  пишем, сколько свободного места осталось. настройка ставится в классе MainHandler на стороне сервера.
        //  по умолчанию сейчас это 1 Гб
    }

    @FXML
    void sendToClient() {                                               //  скачать файл с сервера
        String fileName = selectedFiles.get(CLOUD_FILE);
        if (fileName.length() > 0) {
            nettyClient.sendMsg(new FileRequest(fileName));             //  запрос на сервер на скачивание файла
        }
    }

    @FXML
    void sendToCloud() throws IOException {                             //  отправить файл в облако
        String fileName = selectedFiles.get(CLIENT_FILE);
        if (fileName.length() > 0) {
            String currentClientPrefix = currentClientPath.equals("") ? "" : (currentClientPath + "/");
            Path path = Paths.get(ROOT_PREFIX + currentClientPrefix + fileName);
            if (Files.isDirectory(path)) {
                return;
            }
            int fileSize = (int) Files.size(Paths.get(ROOT_PREFIX + currentClientPrefix + fileName));
            if (serverQuote < (usedServerSpace + fileSize)) {
                ClientApplication ca = ObjectRegistry.getInstance(ClientApplication.class);
                ca.showErrorAlert("Ошибка отправки файла", "Недостаточно места в облачном хранилище");
                return;
            }
            if (fileSize <= NettyClient.MAX_FILE_SIZE) {
                nettyClient.sendMsg(new FileMessage(path));
            } else {                          //  если размер файла больше максимального размера куска, отсылаем его по частям
                int parts = new FileCut(path).cutFile(NettyClient.MAX_FILE_SIZE, ROOT_PREFIX, currentClientPath);
                // режем файл, сохраняем число частей
                int dimension = (int) (Math.log10(parts) + 1);
                Thread t = new Thread(() -> {
                    try {
                        String filePartName;
                        String tempPrefix = ROOT_PREFIX + "temp/" + currentClientPrefix;
                        for (int i = 0; i < parts; i++) {
                            filePartName = String.valueOf(path.getFileName()).concat(Ending.ending(i, dimension));
                            String partPathName = tempPrefix + filePartName;
                            Path partPath = Path.of(tempPrefix + filePartName);
                            nettyClient.sendMsg(new FilePartMessage(fileName, i, parts, filePartName, partPath));
                            Files.delete(Path.of(partPathName));           // отправляем часть и удаляем ее
                        }
                        DirTraveller.cleanUpDir(ROOT_PREFIX + "temp");  //  удаляем ненужные temp директории
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
    void deleteFromCloud() {                           //  удалить файл на облаке
        String fileName = selectedFiles.get(CLOUD_FILE);
        if (fileName.length() > 0) {
            nettyClient.sendMsg(new DeleteFileRequest(fileName));       //  запрос на удаление файла с сервера
        }
    }

    @FXML
    void deleteLocalFile() {                           //  удаление локального файла
        String fileName = selectedFiles.get(CLIENT_FILE);
        if (fileName.length() > 0) {
            try {
                String currentClientPrefix = currentClientPath.equals("") ? "" : (currentClientPath + "/");
                Files.delete(Paths.get(ROOT_PREFIX + currentClientPrefix + fileName));
                refreshLocalFilesList();
                selectedFiles.put(CLIENT_FILE, "");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isDirectory(String s, ListView<String> fileList, ListView<String> fileSizes) {
        // проверка, файл директория или нет - по полю размера. Для директорий это поле равно ""
        int index = fileList.getItems().indexOf(s);
        String size = fileSizes.getItems().get(index);
        return (size.equals(""));
    }

    @FXML
    void makeDirClient() throws IOException {                                   //  создание директории на клиенте
        ClientApplication ca = ObjectRegistry.getInstance(ClientApplication.class);
        ca.openMakeDirWindow(CLIENT_FILE);
    }

    @FXML
    void makeDirCloud() throws IOException {                                    // сохдание директории на облаке
        ClientApplication ca = ObjectRegistry.getInstance(ClientApplication.class);
        ca.openMakeDirWindow(CLOUD_FILE);

    }
}






