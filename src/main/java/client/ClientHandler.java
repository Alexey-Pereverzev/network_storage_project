package client;

import common.messages.*;
import common.other.FileCollect;
import common.other.ListOfFileParts;
import common.other.PartiallySentEntry;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;
import server.Server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;


public class ClientHandler extends ChannelInboundHandlerAdapter {

    private ClientController clientController;
    private ClientApplication clientApplication;
    private ArrayList<ListOfFileParts> listOfAllPartialUploads; //  список всех отправок по частям, которые обрабатываются
                                                                //  в данный момент


    public ClientHandler() {
        listOfAllPartialUploads = new ArrayList<>();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException, InterruptedException {
        clientController = ObjectRegistry.getInstance(ClientController.class);
        clientApplication = ObjectRegistry.getInstance(ClientApplication.class);
        AbstractMessage am = (AbstractMessage) msg;
        if (am instanceof RefreshServerMessage) {           //  сообщение со списком файлов сервера
            if (Platform.isFxApplicationThread()) {         //  обработываем и выводим список в окно клиента
                clientController.processRefreshServerMessage((RefreshServerMessage) am);
            } else {
                Platform.runLater(() -> {
                    clientController.processRefreshServerMessage((RefreshServerMessage) am);
                });
            }
        } else if (am instanceof FileMessage) {                     //  если сервер отправил нам файл
            FileMessage fm = (FileMessage) am;
            Files.write(Paths.get(ClientController.ROOT_PREFIX + fm.getFilename()), fm.getData(),
                    StandardOpenOption.CREATE);         //  сохраняем его
            clientController.refreshLocalFilesList();   //  и обновляем окно клианта
        } else if (am instanceof FilePartMessage) {         //  если сервер отправил нам файл по частям
            partedFileLogic(ctx, (FilePartMessage) msg);    //  запускаем приемку по частям
        } else if (am instanceof AuthResponse) {                    //  если сервер обработал запрос на авторизацию
            AuthResponse ar = (AuthResponse) am;
            if (ar.getAuthMessage().equals("ok")) {                              //    если авторизация успешная
                Platform.runLater(() -> {
                    clientApplication.setUsername(ar.getLogin());   //  запускаем окно клиента с его логином в заголовке
                    try {
                        clientApplication.openStorageWindow();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            } else {                                                //  иначе выдаем сообщение об ошибке авторизации
                Platform.runLater(() -> {
                    clientApplication.showErrorAlert("Ошибка аутентификации",
                            ar.getAuthMessage());
                });
            }
        } else {                      //      другие сообщения пока не обрабатываем
            System.out.println("ПОКА НЕТ КОДА ДЛЯ ЭТОЙ ОПЕРАЦИИ");
        }
    }

    private void partedFileLogic(ChannelHandlerContext ctx, FilePartMessage msg) throws IOException {
        FilePartMessage fpm = msg;
        String fileName = fpm.getFileName();
        String filePartName = fpm.getFilePartName();
        int index = fpm.getIndex();
        int parts = fpm.getParts();
        ListOfFileParts listOfFileParts = new ListOfFileParts("");
        //  список принятых частей файла для данной отправки
        listOfFileParts.setParts(parts);
        listOfFileParts.setFileName(fileName);
        boolean inList = false;
        for (int i = 0; i < listOfAllPartialUploads.size(); i++) {
            if (listOfAllPartialUploads.get(i).getFileName().equals(fileName)) {     //  если это не первая часть отправки
                listOfFileParts = listOfAllPartialUploads.get(i);
                                                                        //  берем список принятых частей из списка всех закачек
                inList = true;
                break;
            }
        }
        String rootPrefix = clientController.ROOT_PREFIX;
        if (!inList) {                                          //  если ранее не было принято ни одной части
            listOfAllPartialUploads.add(listOfFileParts);       //  добавляем закачку данного файла в список закачек
            PartiallySentEntry pseNew = new PartiallySentEntry(filePartName, false);
            listOfFileParts.addEntries(pseNew, index);

            Files.createDirectories(Paths.get(rootPrefix + "temp/"));
            Files.write(Paths.get(rootPrefix + "temp/" + filePartName), fpm.getData(),
                    StandardOpenOption.CREATE);             //  сохраняем часть в /temp
            listOfFileParts.setEntries(index, true);        //  ставим флажок, что данная часть принята
        } else {                                        //  если приемка данного файла уже идёт
            Files.write(Paths.get(rootPrefix + "temp/" + filePartName), fpm.getData(),
                    StandardOpenOption.CREATE);             //  сохраняем часть в /temp
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
            FileCollect fileCollect = new FileCollect(Path.of(rootPrefix + fileName));
            int size = server.ObjectRegistry.getInstance(Server.class).MAX_FILE_SIZE;
            fileCollect.collectFile(size, rootPrefix, parts, fileName);         //  собираем файл из частей
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


