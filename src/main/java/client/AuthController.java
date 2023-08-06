package client;

import common.messages.AuthMessage;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AuthController {                       //      контроллер окна аутентификации
    @FXML
    public TextField loginField;

    @FXML
    public PasswordField passwordField;

    private ClientApplication clientApplication;

    @FXML
    public void checkAuth() {
        //  считываем логин и пароль, если они не пустые, отправляем на сервер запрос на авторизацию
        //  в противном случае выдаем предупреждение
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        if (login.length() == 0 || password.length() == 0) {
            clientApplication.showErrorAlert("Ошибка ввода при аутентификации", "Поля не должны быть пустыми");
            return;
        }
        NettyClient nettyClient = ObjectRegistry.getInstance(NettyClient.class);
        nettyClient.sendMsg(new AuthMessage(login, password));
    }

    public void setStartClient(ClientApplication clientApplication) {
        this.clientApplication = clientApplication;
    }
}

