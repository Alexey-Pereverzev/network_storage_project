package common.messages;


public class AuthMessage extends AbstractMessage {
    private final String login;
    private final String password;

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public AuthMessage(String login, String password) {
        this.login = login;
        this.password = password;
    }


}
