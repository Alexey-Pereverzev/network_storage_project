package common.messages;


public class CreateDirMessage extends AbstractMessage {             //  команда создания директории на сервере

    private final String dirName;

    public String getDirName() {
        return dirName;
    }

    public CreateDirMessage(String dirName) {
        this.dirName = dirName;
    }
}
