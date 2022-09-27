package common.messages;


import java.util.ArrayList;

public class ListOfServerFiles extends AbstractMessage {        //  список файлов облака для отображения на клиенте
    private final ArrayList<String> fileNames;      //  список имен в отображаемой папке
    private final ArrayList<String> fileSizes;      //  список размеров, для диркторий значение соответствует ""
    private final int serverQuote;                  //  дисковая квота клиента на сервере
    private final int usedServerSpace;              //  пространство, занятое на сервере
    private final String error;                     //  сообщение об ошибке

    public ListOfServerFiles(ArrayList<String> fileNames, ArrayList<String> fileSizes, int serverQuote, int usedServerSpace, String error) {
        this.serverQuote = serverQuote;
        this.usedServerSpace = usedServerSpace;
        this.error = error;
        this.fileNames = new ArrayList<>(5);
        this.fileSizes = new ArrayList<>(5);

        for (int i = 0; i < fileNames.size(); i++) {
            this.fileNames.add(i, fileNames.get(i));
            this.fileSizes.add(i, fileSizes.get(i));
        }
    }

    public ArrayList<String> getFileNames() {
        return fileNames;
    }

    public ArrayList<String> getFileSizes() {
        return fileSizes;
    }

    public int getServerQuote() {
        return serverQuote;
    }

    public int getUsedServerSpace() {
        return usedServerSpace;
    }

    public String getError() {
        return error;
    }
}


