package client;

import common.messages.ExitMessage;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientApplication extends Application {

    private Stage primaryStage;                         //  основное окно
    private Stage authStage;                            //  окно авторизации
    private Stage makeDirStage;                         //  окно создания директории
    private String username = "";
    private static final NettyClient nettyClient = ObjectRegistry.getInstance(NettyClient.class);

    public Stage getMakeDirStage() {
        return makeDirStage;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;
        ObjectRegistry.reg(ClientApplication.class, this);
        authAndWork();
    }

    public void authAndWork() throws IOException {
        openAuthDialogue();                                 //  открываем окно авторизации (с методом show)
        createStorageWindow();                              //  и создаем основную сцену (пока без show)
    }

    public void openAuthDialogue() throws IOException {             //  установка сцены авторизации
        FXMLLoader authLoader = new FXMLLoader(ClientApplication.class.getResource("auth-view.fxml"));
        authStage = new Stage();
        Scene scene = new Scene(authLoader.load());
        authStage.setScene(scene);
        authStage.initModality(Modality.WINDOW_MODAL);
        authStage.initOwner(primaryStage);
        authStage.setTitle("Authentication ");
        authStage.setX(100);
        authStage.setY(100);
        authStage.setResizable(false);
        authStage.show();
        authStage.setOnCloseRequest(event -> {
            nettyClient.sendMsg(new ExitMessage(""));
            System.out.println("EXIT");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(0);
        });
        AuthController authController = authLoader.getController();
        authController.setStartClient(this);
    }

    private void createStorageWindow() throws IOException {             //  создание основной сцены
        FXMLLoader fxmlLoader = new FXMLLoader(ClientController.class.getResource("client-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 400);
        primaryStage.setScene(scene);
        primaryStage.setX(200);
        primaryStage.setY(200);
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(event -> {
            nettyClient.sendMsg(new ExitMessage(username));
            System.out.println("EXIT");
            String rootPrefix = ClientController.ROOT_PREFIX;
            try {
                Files.deleteIfExists(Path.of(rootPrefix + "temp/"));    //  удаление временной директории при закрытии
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(0);
        });
    }

    public void openStorageWindow() throws IOException, InterruptedException {
        //  запускаем, если авторизация удачная
        authStage.close();                      //  закрываем окно авторизации
        primaryStage.show();                    //  открываем основную сцену
        ClientController cc = ObjectRegistry.getInstance(ClientController.class);
        cc.showClientFiles();                                 //   показать список файлов клиента
        try {
            cc.showServerFiles();                              //  показать список файлов на сервере
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        primaryStage.setTitle("Alex Cloud - " + username);  //  устанавливаем заголовок с именем пользователя
    }

    public void openMakeDirWindow(int side) throws IOException {        //  установка сцены создания директории
        FXMLLoader mkDirLoader = new FXMLLoader(ClientApplication.class.getResource("mkdir-view.fxml"));
        makeDirStage = new Stage();
        Scene scene = new Scene(mkDirLoader.load());
        makeDirStage.setScene(scene);
        makeDirStage.initModality(Modality.WINDOW_MODAL);
        makeDirStage.initOwner(primaryStage);
        makeDirStage.setX(100);
        makeDirStage.setY(100);
        makeDirStage.setResizable(false);
        makeDirStage.show();
        MakeDirController makeDirController = mkDirLoader.getController();
        makeDirController.setStartClient(this, side);
    }


    public void showErrorAlert(String title, String errorMessage) {
        //  сообщение об ошибке
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(errorMessage);
        alert.show();

    }

    public static void main(String[] args) {
        Application.launch();
    }
}



