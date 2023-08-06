package common.other;

public class PartiallySentEntry {           //  1 элемент списка принятых частей файла

    private String filePartName;            //  название части файла, которую мы приняли
    boolean accept;                         //  принята ли данная часть (true - да)

    public boolean isAccept() {
        return accept;
    }

    public void setAccept(boolean accept) {
        this.accept = accept;
    }

    public void setFilePartName(String filePartName) {
        this.filePartName = filePartName;
    }

    public String getFilePartName() {
        return filePartName;
    }

    public PartiallySentEntry(String filePartName, boolean accept) {
        this.filePartName = filePartName;
        this.accept = accept;
    }

}
