package common.messages;

public class ExitMessage extends AbstractMessage {                  //  сообщаем серверу, что клиент завершает работу
    private final String login;

    public ExitMessage(String login) {
        this.login = login;
    }

    public String getLogin() {
        return login;
    }

}
