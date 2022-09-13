package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import server.authentication.AuthenticationService;
import server.authentication.DBAuthenticationService;

import java.util.HashMap;

public class Server {

    public static final int MAX_OBJECT_SIZE = 100 * 1024 * 1024;
    private final AuthenticationService authenticationService;

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public Server(AuthenticationService authenticationService) {
        ObjectRegistry.reg(Server.class,this);
        this.authenticationService = authenticationService;
    }

    public void run() throws Exception {
        authenticationService.startAuthentication();
        EventLoopGroup mainGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(mainGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(
                                    new ObjectDecoder(MAX_OBJECT_SIZE, ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new MainHandler(new HashMap<>())
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
        new Server(new DBAuthenticationService()).run();            //  запускаем сервер с авторизацией через БД
    }
}

