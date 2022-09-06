package server;

import common.DeleteFileRequest;
import common.FileMessage;
import common.FileRequest;
import common.RefreshServerMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


public class MainHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) {
                return;
            }
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                if (Files.exists(Paths.get("server_storage/" + fr.getFilename()))) {
                    FileMessage fm = new FileMessage(Paths.get("server_storage/" + fr.getFilename()));
                    ctx.writeAndFlush(fm);
                }
            }
            if (msg instanceof FileMessage) {
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get("server_storage/" + fm.getFilename()), fm.getData(),
                        StandardOpenOption.CREATE);
                RefreshServerMessage rm = new RefreshServerMessage(fm.getFilename());
                ctx.writeAndFlush(rm);
            }
            if (msg instanceof DeleteFileRequest) {
                DeleteFileRequest dfr = (DeleteFileRequest) msg;
                try {
                    Files.delete(Paths.get("server_storage/" + dfr.getFilename()));
                    RefreshServerMessage rm = new RefreshServerMessage(dfr.getFilename());
                    ctx.writeAndFlush(rm);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}

