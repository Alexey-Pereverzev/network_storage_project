package client;

import common.messages.AbstractMessage;
import common.messages.AuthResponse;
import common.messages.FileMessage;
import common.messages.RefreshServerMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


public class ClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException, InterruptedException {
        ClientApplication clientApplication = ObjectRegistry.getInstance(ClientApplication.class);
        AbstractMessage am = (AbstractMessage) msg;
        ClientController clientController = ObjectRegistry.getInstance(ClientController.class);
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
            Files.write(Paths.get("client_storage/" + fm.getFilename()), fm.getData(),
                                StandardOpenOption.CREATE);         //  сохраняем его
                        clientController.refreshLocalFilesList();   //  и обновляем окно клианта
        }
        else if (am instanceof AuthResponse) {                    //  если сервер обработал запрос на авторизацию
            AuthResponse ar = (AuthResponse) am;
            if (ar.isSuccessful()) {                              //    если авторизация успешная
                System.out.println(clientApplication);
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
                            "Неверное имя пользователя и/или пароль");
                });
            }
        }
        else {                      //      другие сообщения пока не обрабатываем
            System.out.println("ПОКА НЕТ КОДА ДЛЯ ЭТОЙ ОПЕРАЦИИ");
        }
    }



    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}


