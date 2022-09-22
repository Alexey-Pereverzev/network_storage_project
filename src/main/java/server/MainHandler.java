package server;

import common.messages.*;
import common.other.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;


public class MainHandler extends ChannelInboundHandlerAdapter {             //  основной хендлер

    private ArrayList<ListOfFileParts> listOfAllPartialUploads; //  список всех отправок по частям, которые обрабатываются
                                                                //  в данный момент
    private String rootDirPath;
    private String rootDirPrefix;



    public MainHandler(String rootDirPath) {
        this.rootDirPath = rootDirPath;
        this.rootDirPrefix = rootDirPath + "/";
        ObjectRegistry.reg(MainHandler.class,this);
        listOfAllPartialUploads = new ArrayList<>();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) {
                return;
            }
            ArrayList<String> fileNames = new ArrayList<>(5);
            ArrayList<Integer> fileSizes = new ArrayList<>(5);

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
                getCloudFilesWithSizes(fileNames, fileSizes);
                ctx.writeAndFlush(new RefreshServerMessage(fileNames,fileSizes));
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void acceptFileLogic(ChannelHandlerContext ctx, FileMessage msg, ArrayList<String> fileNames, ArrayList<Integer> fileSizes) throws IOException {            //  если нам прислали файл,
        FileMessage fm = msg;     //сохраняем файл в папку
        Files.write(Paths.get(rootDirPrefix + fm.getFilename()), fm.getData(),
                StandardOpenOption.CREATE);
        getCloudFilesWithSizes(fileNames, fileSizes);
        ctx.writeAndFlush(new RefreshServerMessage(fileNames, fileSizes));
        //  и отправляем клиенту обновленный список файлов облака
    }

    private void deleteFileLogic(ChannelHandlerContext ctx, DeleteFileRequest msg, ArrayList<String> fileNames, ArrayList<Integer> fileSizes) {             //  если получили запрос на удаление файла
        DeleteFileRequest dfr = msg;
        try {
            Files.delete(Paths.get(rootDirPrefix + dfr.getFilename()));
                                                //  удаление файла
            getCloudFilesWithSizes(fileNames, fileSizes);
            ctx.writeAndFlush(new RefreshServerMessage(fileNames, fileSizes));
                                            //  отправляем клиенту обновленный список файлов облака
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void partedFileLogic(ChannelHandlerContext ctx, FilePartMessage msg, ArrayList<String> fileNames, ArrayList<Integer> fileSizes)
            throws IOException, InterruptedException {               //  если прислали часть файла
        ListOfFileParts listOfFileParts = new ListOfFileParts("");
        //  список принятых частей файла для данной отправки
        FilePartMessage fpm = msg;
        String fileName = fpm.getFileName();
        String filePartName = fpm.getFilePartName();
        int index = fpm.getIndex();
        int parts = fpm.getParts();
        listOfFileParts.setParts(parts);
        listOfFileParts.setFileName(fileName);
        boolean inList = false;
        for (int i = 0; i < listOfAllPartialUploads.size(); i++) {
            if (listOfAllPartialUploads.get(i).getFileName().equals(fileName)) {       //  если это не первая часть отправки
                listOfFileParts = listOfAllPartialUploads.get(i);
                                            //  берем список принятых частей из списка всех закачек
                inList = true;
                break;
            }
        }
        if (!inList) {                                              //  если ранее не было принято ни одной части
            listOfAllPartialUploads.add(listOfFileParts);           //  добавляем закачку данного файла в список закачек
            PartiallySentEntry pseNew = new PartiallySentEntry(filePartName, false);
            listOfFileParts.addEntries(pseNew, index);
            Files.createDirectories(Paths.get(rootDirPrefix + "temp/"));
            Files.write(Paths.get(rootDirPrefix + "temp/" + filePartName), fpm.getData(),
                    StandardOpenOption.CREATE);                 //  сохраняем часть в /temp
            listOfFileParts.setEntries(index, true);            //  ставим флажок, что данная часть принята
        } else {                                 //  если приемка данного файла уже идёт
                Files.write(Paths.get(rootDirPrefix + "temp/" + filePartName), fpm.getData(),
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
            FileCollect fileCollect = new FileCollect(Path.of(rootDirPrefix + fileName));
            int size = ObjectRegistry.getInstance(Server.class).MAX_FILE_SIZE;
            fileCollect.collectFile(size, rootDirPrefix, parts, fileName);         //  собираем файл из частей
            getCloudFilesWithSizes(fileNames, fileSizes);
            Thread.sleep(100);
            ctx.writeAndFlush(new RefreshServerMessage(fileNames, fileSizes));   //  обновляем GUI клиента
            listOfAllPartialUploads.remove(listOfFileParts);                    //  удаляем закачку из списка закачек
        }
    }

    private void fileRequestLogic(ChannelHandlerContext ctx, FileRequest msg) throws IOException {
        //      если получили запрос на скачивание
        FileRequest fr = msg;
        if (Files.exists(Paths.get(rootDirPrefix + fr.getFilename()))) {
            String fileName = fr.getFilename();
            Path path = Paths.get(rootDirPrefix + fileName);
            if (Files.size(Paths.get(rootDirPrefix + fr.getFilename()))<=Server.MAX_FILE_SIZE) {
                    //  если файл можно отправить целиком
                FileMessage fm = new FileMessage(Paths.get(rootDirPrefix + fr.getFilename()));
                ctx.writeAndFlush(fm);          //  отправка файла
            } else {            //  если файл надо разбить на части
                int parts = new FileCut(path).cutFile(Server.MAX_FILE_SIZE, rootDirPrefix);
                                //  режем его на части
                int dimension = (int) (Math.log10(parts) + 1);

                Thread t = new Thread(() -> {
                    try {
                        String filePartName = "";
                        for (int i = 0; i < parts; i++) {
                            filePartName = String.valueOf(path.getFileName()).concat(Ending.ending(i,dimension));
                            String partPathName = rootDirPrefix + "temp/" + filePartName;
                            Path partPath = Path.of(rootDirPrefix + "temp/" + filePartName);
                            int index = i;
                            ctx.writeAndFlush(new FilePartMessage(fileName, index, parts, filePartName, partPath));
                                    //  отправляем часть файла
                            Files.delete(Path.of(partPathName));        //  удаляем часть
                        }
                        String[] files = new File((rootDirPrefix + "temp/")).list();
                        if (files.length == 0) {
                            Files.delete(Path.of(rootDirPrefix + "temp/"));   //  удаляем директорию /temp
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                t.setDaemon(true);
                t.start();
            }
        }
    }

    private void getCloudFilesWithSizes(ArrayList<String> fileNames, ArrayList<Integer> fileSizes) throws IOException {
                        //  получение списка файлов сервера для передачи клиенту
        fileNames.clear();
        fileSizes.clear();
        Files.list(Paths.get(rootDirPath)).map(p -> p.getFileName().toString())
                .forEach(o -> fileNames.add(o));
        Files.list(Paths.get(rootDirPath)).map(p -> {
            try {
                return Files.size(p);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).forEach(o -> fileSizes.add(Math.toIntExact(o)));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}

