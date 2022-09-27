package server;

import common.messages.*;
import common.other.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;


public class MainHandler extends ChannelInboundHandlerAdapter {             //  основной хендлер обработки сообщений на сервере

    private final ArrayList<ListOfFileParts> listOfAllPartialUploads;
    //  список всех отправок по частям, которые обрабатываются в данный момент
    private final String rootDirPath;               //  путь до папки клиента на сервере
    private final String rootDirPrefix;             //  то же самое +"/"
    private int usedServerSpace = 0;                //  занятое место на сервере
    private static final int SERVER_QUOTE = 1024 * 1024 * 1024;     //  квота клиента (установили 1Гб)
    private String currentPath;                     //  путь до текущей папки облака относительно корня
    private String currentPrefix;                   //  то же самое +"/"
    private String error="ok";                      //  сообщение об ошибке


    public MainHandler(String rootDirPath) {
        this.rootDirPath = rootDirPath;                 //  получаем корневой путь от AuthHandler
        this.rootDirPrefix = rootDirPath + "/";
        listOfAllPartialUploads = new ArrayList<>();    //  создаем список активных загрузок частей файлов
        currentPath = "";                               //  текущий относительный путь - для корня это ""
        currentPrefix = currentPath + "/";
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) {
                return;
            }
            ArrayList<String> fileNames = new ArrayList<>(5);
            ArrayList<String> fileSizes = new ArrayList<>(5);

            if (msg instanceof FileRequest) {               //      если получили запрос на скачивание
                fileRequestLogic(ctx, (FileRequest) msg);
            }
            if (msg instanceof FileMessage) {           //  если нам прислали файл
                acceptFileLogic(ctx, (FileMessage) msg, fileNames, fileSizes);
            }
            if (msg instanceof FilePartMessage) {       //  если прислали часть файла
                partedFileLogic(ctx, (FilePartMessage) msg, fileNames, fileSizes);
            }
            if (msg instanceof DeleteFileRequest) {     //  если получили запрос на удаление файла
                deleteFileLogic(ctx, (DeleteFileRequest) msg, fileNames, fileSizes);
            }
            if (msg instanceof CloudFileListRequest) {  //  если у нас запросили список файлов облака, отправляем его
                currentPath = ((CloudFileListRequest) msg).getCurrentCloudPath();
                currentPrefix = currentPath + "/";
                usedServerSpace = getCloudFilesWithSizes(fileNames, fileSizes);
                ctx.writeAndFlush(new ListOfServerFiles(fileNames, fileSizes, SERVER_QUOTE, usedServerSpace, error));
            }
            if (msg instanceof CreateDirMessage) {      //  если получили запрос на создание директории
                createDirLogic(ctx, (CreateDirMessage) msg, fileNames, fileSizes);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }


    private void acceptFileLogic(ChannelHandlerContext ctx, FileMessage fm, ArrayList<String> fileNames, ArrayList<String> fileSizes) throws IOException {            //  если нам прислали файл,
        Files.write(Paths.get(rootDirPrefix + currentPrefix + fm.getFilename()), fm.getData(),
                StandardOpenOption.CREATE);         //  сохраняем его
        usedServerSpace = getCloudFilesWithSizes(fileNames, fileSizes);     // подсчитываем новый размер клиентской папки
        ctx.writeAndFlush(new ListOfServerFiles(fileNames, fileSizes, SERVER_QUOTE, usedServerSpace, error));
        //  и отправляем клиенту обновленный список файлов облака
    }

    private void deleteFileLogic(ChannelHandlerContext ctx, DeleteFileRequest dfr, ArrayList<String> fileNames, ArrayList<String> fileSizes) {             //  если получили запрос на удаление файла
        try {
            Files.delete(Paths.get(rootDirPrefix + currentPrefix + dfr.getFilename()));
            //  удаляем файл
            usedServerSpace = getCloudFilesWithSizes(fileNames, fileSizes); // подсчитываем новый размер клиентской папки
            ctx.writeAndFlush(new ListOfServerFiles(fileNames, fileSizes, SERVER_QUOTE, usedServerSpace, error));
            //  и отправляем клиенту обновленный список файлов облака
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void partedFileLogic(ChannelHandlerContext ctx, FilePartMessage fpm, ArrayList<String> fileNames, ArrayList<String> fileSizes)  throws IOException, InterruptedException {               //  если прислали часть файла
        ListOfFileParts listOfFileParts = new ListOfFileParts("", currentPath);
        //  создаем новый список принятых частей файла для данной отправки из сообщения FilePartMessage
        String fileName = fpm.getFileName();
        String filePartName = fpm.getFilePartName();
        int index = fpm.getIndex();
        int parts = fpm.getParts();
        listOfFileParts.setParts(parts);
        listOfFileParts.setFileName(fileName);
        boolean inList = false;
        for (ListOfFileParts someUpload : listOfAllPartialUploads) {
            if (someUpload.getFileName().equals(fileName) && someUpload.getCurrentPath().equals(currentPath)) {
                //  если это не первая часть отправки
                listOfFileParts = someUpload;
                //  берем список принятых частей из списка всех закачек
                inList = true;
                break;
            }
        }
        String currentSuffix = (currentPrefix.equals("/")) ? "" : currentPrefix;
        String tempPrefix = rootDirPrefix + "temp/" + currentSuffix;
        if (!inList) {                                              //  если ранее не было принято ни одной части
            listOfAllPartialUploads.add(listOfFileParts);           //  добавляем закачку данного файла в список закачек
            PartiallySentEntry pseNew = new PartiallySentEntry(filePartName, false);
            listOfFileParts.addEntries(pseNew, index);

            Files.createDirectories(Paths.get(tempPrefix));
            Files.write(Paths.get(tempPrefix + filePartName), fpm.getData(),
                    StandardOpenOption.CREATE);                 //  сохраняем часть в /temp
            listOfFileParts.setEntries(index, true);            //  ставим флажок, что данная часть принята
        } else {                                 //  если приемка данного файла уже идёт
            Files.write(Paths.get(tempPrefix + filePartName), fpm.getData(),
                    StandardOpenOption.CREATE);               //  сохраняем часть в /temp
            listOfFileParts.addEntries(new PartiallySentEntry(filePartName, true), index);
            //  ставим флажок, что данная часть принята
        }

        boolean allPartsHere = true;                                 // проверяем, приняли ли мы все части данной закачки
        for (int i = 0; i < listOfFileParts.getParts(); i++) {
            if (!listOfFileParts.getEntries().get(i).isAccept()) {
                allPartsHere = false;
                break;
            }
        }
        if (allPartsHere) {                  //  если всё принято
            FileCollect fileCollect = new FileCollect(Path.of(rootDirPrefix + currentSuffix + fileName));
            int size = Server.MAX_FILE_SIZE;
            fileCollect.collectFile(size, rootDirPrefix, currentSuffix, parts);         //  собираем файл из частей
            usedServerSpace = getCloudFilesWithSizes(fileNames, fileSizes);
            Thread.sleep(100);
            ctx.writeAndFlush(new ListOfServerFiles(fileNames, fileSizes, SERVER_QUOTE, usedServerSpace, error));
                                                                                //  обновляем GUI клиента
            listOfAllPartialUploads.remove(listOfFileParts);                    //  и удаляем закачку из списка закачек
        }
    }

    private void fileRequestLogic(ChannelHandlerContext ctx, FileRequest fr) throws IOException {
        //      если получили запрос на скачивание
        String currentSuffix = (currentPrefix.equals("/")) ? "" : currentPrefix;
        if (Files.exists(Paths.get(rootDirPrefix + currentSuffix + fr.getFilename()))) {
            String fileName = fr.getFilename();
            Path path = Paths.get(rootDirPrefix + currentSuffix + fileName);
            if (Files.size(Paths.get(rootDirPrefix + currentSuffix + fr.getFilename())) <= Server.MAX_FILE_SIZE) {
                //  если файл можно отправить целиком
                FileMessage fm = new FileMessage(Paths.get(rootDirPrefix + currentSuffix + fr.getFilename()));
                ctx.writeAndFlush(fm);          //  отправка файла
            } else {            //  если файл надо разбить на части
                int parts = new FileCut(path).cutFile(Server.MAX_FILE_SIZE, rootDirPrefix, currentPath);
                //  режем его на части
                int dimension = (int) (Math.log10(parts) + 1);

                Thread t = new Thread(() -> {
                    try {
                        String filePartName;
                        String tempPrefix = rootDirPrefix + "temp/" + currentSuffix;
                        for (int i = 0; i < parts; i++) {
                            filePartName = String.valueOf(path.getFileName()).concat(Ending.ending(i, dimension));
                            String partPathName = tempPrefix + filePartName;
                            Path partPath = Path.of(tempPrefix + filePartName);
                            ctx.writeAndFlush(new FilePartMessage(fileName, i, parts, filePartName, partPath));
                            //  отправляем часть файла
                            Files.delete(Path.of(partPathName));        //  удаляем часть
                        }

                        DirTraveller.cleanUpDir(rootDirPrefix + "temp");

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                t.setDaemon(true);
                t.start();
            }
        }
    }

    private void createDirLogic(ChannelHandlerContext ctx, CreateDirMessage cdm, ArrayList<String> fileNames, ArrayList<String> fileSizes) {
        String currentSuffix = currentPath.equals("") ? "" : (currentPath + "/");
        error = "ok";
        try {
            Files.createDirectories(Path.of(rootDirPrefix + currentSuffix + cdm.getDirName()));
            usedServerSpace = getCloudFilesWithSizes(fileNames, fileSizes);
        } catch (IOException e) {
            error = "Невозможно создать директорию";
        }
        ctx.writeAndFlush(new ListOfServerFiles(fileNames, fileSizes, SERVER_QUOTE, usedServerSpace, error));
    }


    private int getCloudFilesWithSizes(ArrayList<String> fileNames, ArrayList<String> fileSizes) throws IOException {
        //  получение списка файлов сервера для передачи клиенту
        fileNames.clear();
        fileSizes.clear();
        Files.list(Paths.get(rootDirPrefix + currentPath)).map(p -> p.getFileName().toString()).forEach(fileNames::add);
        Files.list(Paths.get(rootDirPrefix + currentPath)).map(p -> {
            try {
                if (Files.isDirectory(p)) {
                    return "";
                } else {
                    return String.valueOf(Files.size(p));
                }
            } catch (IOException e) {
                error = e.getMessage();
                e.printStackTrace();
                return null;
            }
        }).forEach(fileSizes::add);
        return DirTraveller.getUsedSpace(rootDirPath);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}

