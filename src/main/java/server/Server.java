package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import server.authentication.AuthenticationService;
import server.authentication.DBAuthenticationService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class Server {

    public static final int MAX_FILE_SIZE = 10 * 1024 * 1024;
    public static final int MAX_OBJECT_SIZE = MAX_FILE_SIZE * 2;    //  максимальныцй размер объекта должен быть больше
    // максимального размера куска файла, т.к. иначе некоторые сообщения от клиента, внутри которых лежит такой кусок, не
    // "пролезут" через декодер
    public static final String ROOT_PATH = "server_storage";
    public static final String ROOT_PREFIX = ROOT_PATH + "/";
    private final AuthenticationService authenticationService;
    private final HashMap<String, ChannelHandlerContext> authorizedClients;

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public Server(AuthenticationService authenticationService, HashMap<String, ChannelHandlerContext> authorizedClients) {
        this.authorizedClients = authorizedClients;
        this.authenticationService = authenticationService;
        ObjectRegistry.reg(Server.class, this);
    }

    public void run() throws Exception {
        if (Files.notExists(Path.of(ROOT_PATH))) {
            Files.createDirectories(Path.of(ROOT_PATH));
        }
        authenticationService.startAuthentication();
        EventLoopGroup mainGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(mainGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline().addLast(
                                    new ObjectDecoder(MAX_OBJECT_SIZE, ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new AuthHandler(authorizedClients)
                            );
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = b.bind(32323).sync();
            future.channel().closeFuture().sync();
        } finally {
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            authenticationService.endAuthentication();
        }
    }

    public static void main(String[] args) throws Exception {
        new Server(new DBAuthenticationService(new HashEncoder()), new HashMap<>()).run();
        //  запускаем сервер с авторизацией через БД
    }
}

