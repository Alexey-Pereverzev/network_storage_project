package common.messages;

public class ExitMessage extends AbstractMessage {
    private String login;

    public ExitMessage(String login) {
        this.login = login;
    }

    public String getLogin() {
        return login;
    }

}
