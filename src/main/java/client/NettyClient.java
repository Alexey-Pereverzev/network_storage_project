package client;

import common.messages.AbstractMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;


public class NettyClient {

    public static final int MAX_OBJECT_SIZE = 100 * 1024 * 1024;

    private final Channel clientChannel;

    public NettyClient() {                          //  клиент на нетти
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .remoteAddress("localhost", 32323)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        socketChannel.pipeline().addLast(
                                new ObjectDecoder(MAX_OBJECT_SIZE, ClassResolvers.cacheDisabled(null)),
                                new ObjectEncoder(),
                                new ClientHandler()
                        );
                    }
                });

        ChannelFuture channelFuture = bootstrap.connect();
        this.clientChannel = channelFuture.channel();
    }


    public void sendMsg(AbstractMessage msg) {      //  отправка сообщения на сервер
        try {
            clientChannel.writeAndFlush(msg).sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }


}
