import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.ReferenceCountUtil;

import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Jason MacKeigan on 2017-06-29 at 3:30 PM
 */
public class ServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = Logger.getLogger(ServerHandler.class.getName());

    private final Server server;

    public ServerHandler(Server server) {
        this.server = server;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof byte[]) {
            ByteBuf message = ctx.alloc().buffer().writeBytes((byte[]) msg);

            try {
                int id = message.readByte();

                if (id == 0) {
                    String incomingMessage = NetworkUtil.readString(message);

                    server.readMessage(ctx.channel(), incomingMessage);
                } else {
                    logger.log(Level.ALL, String.format("Unknown packet id: %s", id));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ReferenceCountUtil.release(message);
            }
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);

        server.removeChannel(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
