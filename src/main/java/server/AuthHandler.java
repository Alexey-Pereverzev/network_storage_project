package server;

import common.messages.AuthMessage;
import common.messages.AuthResponse;
import common.messages.ExitMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import server.authentication.AuthenticationService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;

import static server.Server.ROOT_PREFIX;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    //  первый хендлер пайплайна, обрабатывает запрос на авторизацию, пропускает входящий запрос далее для авторизованного пользователя,
    //  отключает клиента, если он прислал ExitMessage
    private final HashMap<String, ChannelHandlerContext> authorizedClients;
    //  мэпа для сохранения авторизованных пользователей
    private String rootDirPrefix;           //  путь до корневой папки данного клиента


    public AuthHandler(HashMap<String, ChannelHandlerContext> authorizedClients) {
        this.authorizedClients = authorizedClients;
        ObjectRegistry.reg(AuthHandler.class, this);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) {
                return;
            }
            if (msg instanceof AuthMessage auth) {
                // если пришел запрос на авторизацию, делаем попытку авторизации по логину и паролю из сообщения
                String login = auth.getLogin();
                boolean isInHashMap = authorizedClients.entrySet().stream().anyMatch(entry -> entry.getKey().equals(login));
                if (isInHashMap) {          //  если пользователь уже авторизован, сообщаем об этом клиенту
                    ctx.writeAndFlush(new AuthResponse(auth.getLogin(), "Уже есть такой пользователь"));
                } else {        //  если нет, пытаемся авторизоваться по логину и паролю
                    String authMessage = processAuthentication(auth.getLogin(), auth.getPassword());

                    if (authMessage.equals("ok")) {         //  если ответ от БД положительный
                        String s = auth.getLogin();
                        authorizedClients.put(s, ctx);      //  добавляем пользователя в мэпу
                        String rootDirPath = ROOT_PREFIX + s;
                        rootDirPrefix = rootDirPath + "/";      //  задаем корневой путь на сервере для данного клиента
                        if (Files.notExists(Path.of(rootDirPath))) {        //  создаем папку клиента, если она еще не существует
                            Files.createDirectories(Path.of(rootDirPrefix));
                        }
                        ctx.pipeline().addLast(new MainHandler(rootDirPath));
                        //  создаем в пайплайне хендлер для обработки сообщений, пробрасывая на него путь к папке клиента

                    } else {
                        System.out.println("Неудачная попытка аутентификации");
                    }
                    ctx.writeAndFlush(new AuthResponse(auth.getLogin(), authMessage));  //  отправляем ответ об авторизации
                }
            }
            if (msg instanceof ExitMessage em) {                        //  если клиент закончил работу
                System.out.println("КЛИЕНТ ОТКЛЮЧИЛСЯ!");
                String username = em.getLogin();
                if (username.length() > 0) {                                //   если клиент авторизован
                    authorizedClients.remove(username);                    //  удаляем его из мэпы
                }
                Files.deleteIfExists(Path.of(rootDirPrefix + "temp/"));    //  удаление временной директории
                ctx.close();                                               //  закрываем соединение
            } else {                                                //  если пришло другое сообщение
                if (authorizedClients.containsValue(ctx)) {         //  проверяем, авторизован ли пользователь
                    ctx.fireChannelRead(msg);                       //  если да, пробрасываем сообщение дальше по пайплайну
                } else {
                    System.out.println("USER IS NOT AUTHORIZED");
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }


    private String processAuthentication(String login, String password) {           //  авторизация через БД
        //  авторизация через выбранный метод авторизации на сервере
        Server s = ObjectRegistry.getInstance(Server.class);
        AuthenticationService auth = s.getAuthenticationService();
        try {
            return auth.authUserByLoginAndPassword(login, password);        //  если авторизация успешная, придет сообщение "ok"
        } catch (SQLException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "Ошибка авторизации";        //  если сработало исключение, сообщим об ошибке
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

}
