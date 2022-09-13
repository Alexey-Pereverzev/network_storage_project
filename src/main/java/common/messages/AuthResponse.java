package common.messages;


public class AuthResponse extends AbstractMessage {         //  ответ авторизации
    private String login;                                   //  логин пользователя
    private final boolean isSuccessful;                     //  успешна ли авторизация

    public String getLogin() {
        return login;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }


    public AuthResponse(String login, boolean isSuccessful) {
        this.login = login;
        this.isSuccessful = isSuccessful;
    }


}
