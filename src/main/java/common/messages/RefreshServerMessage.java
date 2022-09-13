package common.messages;


import java.util.ArrayList;

public class RefreshServerMessage extends AbstractMessage {
    private ArrayList<String> fileNames;
    private ArrayList<Integer> fileSizes;

    public RefreshServerMessage(ArrayList<String> fileNames, ArrayList<Integer> fileSizes) {
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

    public ArrayList<Integer> getFileSizes() {
        return fileSizes;
    }
}


