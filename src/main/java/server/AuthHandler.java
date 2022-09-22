package server;

import common.messages.AuthMessage;
import common.messages.AuthResponse;
import common.messages.ExitMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import server.authentication.AuthenticationService;

import java.sql.SQLException;
import java.util.HashMap;


public class AuthHandler extends ChannelInboundHandlerAdapter {
    private HashMap<String, ChannelHandlerContext> authorizedClients;
    //  мэпа для сохранения авторизованных пользователей

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
            if (msg instanceof AuthMessage) {            // если пришел запрос на авторизацию,
                // делаем попытку авторизации по логину и паролю из сообщения
                AuthMessage auth = (AuthMessage) msg;
                String login = auth.getLogin();
                boolean isInHashMap = authorizedClients.entrySet().stream().anyMatch(entry -> entry.getKey().equals(login));
                if (isInHashMap) {
                    ctx.writeAndFlush(new AuthResponse(auth.getLogin(), "Уже есть такой пользователь"));
                } else {
                    String authMessage = processAuthentication(auth.getLogin(), auth.getPassword());

                    if (authMessage.equals("ok")) {
                        authorizedClients.put(auth.getLogin(), ctx);
                        //  если авторизация успешна, добавляем пользователя в мэпу
                    } else {
                        System.out.println("Неудачная попытка аутентификации");
                    }
                    ctx.writeAndFlush(new AuthResponse(auth.getLogin(), authMessage));
                    //  отправляем ответ об авторизации
                }
            }
            if (msg instanceof ExitMessage) {       //  если клиент закончил работу
                System.out.println("КЛИЕНТ ОТКЛЮЧИЛСЯ!");
                ExitMessage em = (ExitMessage) msg;
                authorizedClients.remove(em.getLogin());      //  удаляем его из мэпы
                ctx.close();                        //  и закрываем соединение
            } else {
                ctx.fireChannelRead(msg);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private String processAuthentication(String login, String password) {
        //  авторизация через выбранный метод авторизации на сервере
        Server s = ObjectRegistry.getInstance(Server.class);
        AuthenticationService auth = s.getAuthenticationService();
        try {
            return auth.authUserByLoginAndPassword(login, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Ошибка авторизации";
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

}
