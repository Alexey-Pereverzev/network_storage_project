package server;

import common.messages.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import server.authentication.AuthenticationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;


public class MainHandler extends ChannelInboundHandlerAdapter {
                            //  основной хендлер, авторизация пока происходит тоже здесь

    private HashMap<ChannelHandlerContext,String> authorizedClients;
            //  мэпа для сохранения авторизованных пользователей

    public MainHandler(HashMap<ChannelHandlerContext, String> authorizedClients) {
        this.authorizedClients = authorizedClients;
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
                FileRequest fr = (FileRequest) msg;
                if (Files.exists(Paths.get("server_storage/" + fr.getFilename()))) {
                    FileMessage fm = new FileMessage(Paths.get("server_storage/" + fr.getFilename()));
                    if (Files.size(Paths.get("server_storage/" + fr.getFilename()))<=Server.MAX_OBJECT_SIZE) {
                        ctx.writeAndFlush(fm);          //  отправка файла
                    } else {
                        System.out.println("File is too big for now");
                                        //  если файл больше максимального размера куска, пока ничего не делаем
                                        //  позже здесь будет отправка по частям
                    }
                }
            }
            if (msg instanceof FileMessage) {           //  если нам прислали файл, сохраняем его в папку
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get("server_storage/" + fm.getFilename()), fm.getData(),
                        StandardOpenOption.CREATE);
                getCloudFilesWithSizes(fileNames, fileSizes);
                ctx.writeAndFlush(new RefreshServerMessage(fileNames,fileSizes));
                                                        //  и отправляем клиенту обновленный список файлов облака
            }
            if (msg instanceof DeleteFileRequest) {     //  если получили запрос на удаление файла
                DeleteFileRequest dfr = (DeleteFileRequest) msg;
                try {
                    Files.delete(Paths.get("server_storage/" + dfr.getFilename()));
                                                        //  удаление файла
                    getCloudFilesWithSizes(fileNames, fileSizes);
                    ctx.writeAndFlush(new RefreshServerMessage(fileNames,fileSizes));
                                                    //  отправляем клиенту обновленный список файлов облака
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (msg instanceof CloudFileListRequest) {  //  если у нас запросили список файлов облака, отправляем его
                getCloudFilesWithSizes(fileNames, fileSizes);
                ctx.writeAndFlush(new RefreshServerMessage(fileNames,fileSizes));
            }
            if (msg instanceof AuthMessage) {            // если пришел запрос на авторизацию,
                                                        //делаем попытку авторизации по логину и паролю из сообщения
                AuthMessage auth = (AuthMessage) msg;
                boolean successAuth = processAuthentication(auth.getLogin(),auth.getPassword());
                if (successAuth) {
                    authorizedClients.put(ctx, auth.getLogin());
                                                //  если авторизация успешна, добавляем пользователя в мэпу
                } else {
                    System.out.println("Неудачная попытка аутентификации");
                }
                ctx.writeAndFlush(new AuthResponse(auth.getLogin(), successAuth));
                                            //  отправляем ответ об авторизации
            }
            if (msg instanceof ExitMessage) {       //  если клиент закончил работу
                authorizedClients.remove(ctx);      //  удаляем его из мэпы
                ctx.close();                        //  и закрываем соединение
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private boolean processAuthentication(String login, String password) {
                                        //  авторизация через выбранный метод авторизации на сервере
        Server s = ObjectRegistry.getInstance(Server.class);
        AuthenticationService auth = s.getAuthenticationService();
        try {
            return auth.authUserByLoginAndPassword(login, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void getCloudFilesWithSizes(ArrayList<String> fileNames, ArrayList<Integer> fileSizes) throws IOException {
                        //  получение списка файлов сервера для передачи клиенту
        fileNames.clear();
        fileSizes.clear();
        Files.list(Paths.get("server_storage")).map(p -> p.getFileName().toString())
                .forEach(o -> fileNames.add(o));
        Files.list(Paths.get("server_storage")).map(p -> {
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

