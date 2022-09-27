package client;

import common.messages.*;
import common.other.FileCollect;
import common.other.ListOfFileParts;
import common.other.PartiallySentEntry;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;


public class ClientHandler extends ChannelInboundHandlerAdapter {

    private ClientController clientController;
    private ClientApplication clientApplication;
    private final ArrayList<ListOfFileParts> listOfAllPartialUploads;
    //  список всех отправок по частям, которые обрабатываются в данный момент

    public ClientHandler() {
        listOfAllPartialUploads = new ArrayList<>();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println(ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException, InterruptedException {
        clientController = ObjectRegistry.getInstance(ClientController.class);
        clientApplication = ObjectRegistry.getInstance(ClientApplication.class);
        AbstractMessage am = (AbstractMessage) msg;
        if (am instanceof ListOfServerFiles) {           //  сообщение со списком файлов сервера
            if (Platform.isFxApplicationThread()) {         //  обработываем и выводим список в окно клиента
                clientController.processRefreshServerMessage((ListOfServerFiles) am);
            } else {
                Platform.runLater(() ->
                        clientController.processRefreshServerMessage((ListOfServerFiles) am));
            }
            if (!((ListOfServerFiles) am).getError().equals("ok")) {
                clientApplication.showErrorAlert("Ошибка", ((ListOfServerFiles) am).getError());
            }
        } else if (am instanceof FileMessage fm) {                     //  если сервер отправил нам файл
            String currentSuffix = clientController.getCurrentClientPath().equals("") ? ""
                    : (clientController.getCurrentClientPath() + "/");
            String currentPrefix = ClientController.ROOT_PREFIX + currentSuffix;
            Files.write(Paths.get(currentPrefix + fm.getFilename()), fm.getData(),
                    StandardOpenOption.CREATE);         //  сохраняем его
            clientController.refreshLocalFilesList();   //  и обновляем окно клианта
        } else if (am instanceof FilePartMessage) {         //  если сервер отправил нам файл по частям
            partedFileLogic((FilePartMessage) msg);    //  запускаем приемку по частям
        } else if (am instanceof AuthResponse ar) {                    //  если сервер обработал запрос на авторизацию
            processAuthResponse(ar);
        } else {                      //      другие сообщения не обрабатываем
            System.out.println("ПОКА НЕТ КОДА ДЛЯ ЭТОЙ ОПЕРАЦИИ");
        }
    }

    private void processAuthResponse(AuthResponse ar) {         //  обработка ответа сервера на авторизацию
        if (ar.getAuthMessage().equals("ok")) {                  //    если авторизация успешная
            Platform.runLater(() -> {
                clientApplication.setUsername(ar.getLogin());   //  запускаем окно клиента с его логином в заголовке
                try {
                    clientApplication.openStorageWindow();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        } else {                                                //  иначе выдаем сообщение об ошибке авторизации
            Platform.runLater(() ->
                    clientApplication.showErrorAlert("Ошибка аутентификации", ar.getAuthMessage()));
        }
    }

    private void partedFileLogic(FilePartMessage fpm) throws IOException {          //  приемка части файла
        String fileName = fpm.getFileName();
        String filePartName = fpm.getFilePartName();
        int index = fpm.getIndex();
        int parts = fpm.getParts();
        String currentPath = clientController.getCurrentClientPath();
        ListOfFileParts listOfFileParts = new ListOfFileParts("", currentPath);
        //  список принятых частей файла для данной отправки
        listOfFileParts.setParts(parts);
        listOfFileParts.setFileName(fileName);
        boolean inList = false;
        for (ListOfFileParts listOfAllPartialUpload : listOfAllPartialUploads) {
            if (listOfAllPartialUpload.getFileName().equals(fileName)) {     //  если это не первая часть отправки
                listOfFileParts = listOfAllPartialUpload;
                //  берем список принятых частей из списка всех закачек
                inList = true;
                break;
            }
        }
        String rootPrefix = ClientController.ROOT_PREFIX;
        String currentSuffix = (currentPath.equals("")) ? "" : (currentPath + "/");
        String tempPrefix = rootPrefix + "temp/" + currentSuffix;
        if (!inList) {                                          //  если ранее не было принято ни одной части
            listOfAllPartialUploads.add(listOfFileParts);       //  добавляем закачку данного файла в список закачек
            PartiallySentEntry pseNew = new PartiallySentEntry(filePartName, false);
            listOfFileParts.addEntries(pseNew, index);

            Files.createDirectories(Paths.get(tempPrefix));
            Files.write(Paths.get(tempPrefix + filePartName), fpm.getData(), StandardOpenOption.CREATE);
            //  сохраняем часть в /temp
            listOfFileParts.setEntries(index, true);        //  ставим флажок, что данная часть принята
        } else {                                        //  если приемка данного файла уже идёт
            Files.write(Paths.get(tempPrefix + filePartName), fpm.getData(), StandardOpenOption.CREATE);
            //  сохраняем часть в /temp
            listOfFileParts.addEntries(new PartiallySentEntry(filePartName, true), index);
            //  ставим флажок, что данная часть принята
        }
        boolean allPartsHere = true;                                // проверяем, приняли ли мы все части данной закачки
        for (int i = 0; i < listOfFileParts.getParts(); i++) {
            if (!listOfFileParts.getEntries().get(i).isAccept()) {
                allPartsHere = false;
                break;
            }
        }
        if (allPartsHere) {         //  если всё принято
            FileCollect fileCollect = new FileCollect(Path.of(rootPrefix + currentSuffix + fileName));
            int size = NettyClient.MAX_FILE_SIZE;
            fileCollect.collectFile(size, rootPrefix, currentSuffix, parts);         //  собираем файл из частей
            clientController.refreshLocalFilesList();                       //  обновляем GUI клиента
            listOfAllPartialUploads.remove(listOfFileParts);            //  удаляем закачку из списка закачек
        }
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.writeAndFlush(new ExitMessage(ObjectRegistry.getInstance(ClientApplication.class).getUsername()));
        ctx.close();
    }
}


