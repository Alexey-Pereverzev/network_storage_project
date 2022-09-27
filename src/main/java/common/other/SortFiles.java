package common.other;

import javafx.scene.control.ListView;

public class SortFiles {                //  сортировщик списка файлов, перемещает диерктории наверх по списку, после директорий идут файлы
    //  проверка на директорию идет в сравнении размера файлов со значением "", это значение мы присвоили размеру директории при
    //  построении списка размеров файлов
    public static void sortFiles(ListView<String> fileList, ListView<String> fileSizes) {
        int size = fileList.getItems().size();
        int i = 0;
        while (i < size - 1) {
            if (!fileSizes.getItems().get(i).equals("") && fileSizes.getItems().get(i + 1).equals("")) {
                swap(fileList, fileSizes, i);
                if (i > 0) {
                    i = i - 1;
                } else {
                    i = i + 1;
                }
            } else {
                i = i + 1;
            }
        }
    }

    private static void swap(ListView<String> fileList, ListView<String> fileSizes, int i) {
        //  функция для перестановки 2х соседних значений списка файлов и размеров файлов, реализована для ситуации, когда
        //  нужно переставить местами файл и директорию
        String buf;
        buf = fileList.getItems().get(i);
        fileList.getItems().set(i, fileList.getItems().get(i + 1));
        fileList.getItems().set(i + 1, buf);
        fileSizes.getItems().set(i + 1, fileSizes.getItems().get(i));
        fileSizes.getItems().set(i, "");
    }
}
