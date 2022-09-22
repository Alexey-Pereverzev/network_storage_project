package common.other;

import java.util.ArrayList;

public class ListOfFileParts {                  //  структура для хранения состояния процесса приемки файла
    ArrayList<PartiallySentEntry> entries;      //  список названий принятых частей
    private String fileName;                    //  название принимаемого файла
    private int parts;                          //  количество частей для приемки

    public ArrayList<PartiallySentEntry> getEntries() {
        return entries;
    }

    public void setEntries(int index, boolean b) {
        this.entries.get(index).setAccept(b);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getParts() {
        return parts;
    }

    public void setParts(int parts) {
        this.parts = parts;
        if (entries.size() == 0) {
            for (int i = 0; i < parts; i++) {
                entries.add(new PartiallySentEntry("", false));
            }
        }
    }

    public ListOfFileParts(String fileName) {
        this.fileName = fileName;
        entries = new ArrayList<>();
    }

    public void addEntries(PartiallySentEntry pse, int index) {            //  функция добавления части в список принятых частей
        this.entries.get(index).setAccept(pse.isAccept());
        this.entries.get(index).setFilePartName(pse.getFilePartName());
    }
}
