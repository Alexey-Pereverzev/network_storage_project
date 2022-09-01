package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientController.class.getResource("client-view.fxml"));
        Scene scene = new Scene((Parent) fxmlLoader.load(), 600, 400);
        stage.setTitle("Mighty Cloud");
        stage.setScene(scene);
        stage.setY(200);
        stage.setX(200);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch();
    }
}



