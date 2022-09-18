package common.other;

public class Ending {               //  класс для генерации "хвостиков" названий частей файла

    public static String ending(int partIndex, int dimension) {
        //  параметры - номер части, размерность максмального номера. например, если у нас 243 части, номера будут 000...243
        String s = Integer.toString(partIndex);
        while (s.length()<dimension) {
            s = "0".concat(s);
        }
        s = s.concat(".tmp");
        return s;
    }
}
