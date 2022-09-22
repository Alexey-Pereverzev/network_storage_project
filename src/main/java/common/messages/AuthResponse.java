package common.messages;


public class AuthResponse extends AbstractMessage {         //  ответ авторизации
    private String login;                                   //  логин пользователя
    private final String authMessage;                     //  успешна ли авторизация

    public String getLogin() {
        return login;
    }


    public String getAuthMessage() {
        return authMessage;
    }


    public AuthResponse(String login, String isSuccessful) {
        this.login = login;
        this.authMessage = isSuccessful;
    }


}
