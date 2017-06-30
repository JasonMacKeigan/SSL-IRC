import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import jdk.nashorn.internal.objects.Global;

import java.nio.charset.Charset;

/**
 * Created by Jason MacKeigan on 2017-06-30 at 2:53 AM
 */
public class ServerLoginHandler extends ChannelInboundHandlerAdapter {

    private static final ChannelGroup loggingIn = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private final Server server;

    public ServerLoginHandler(Server server) {
        this.server = server;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(future -> {
            loggingIn.add(ctx.channel());
            System.out.println("Logging in...");
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!loggingIn.contains(ctx.channel())) {
            ctx.close();
            return;
        }
        if (msg instanceof byte[]) {
            ByteBuf message = ctx.alloc().buffer().writeBytes((byte[]) msg);

            try {
                loggingIn.remove(ctx.channel());

                String username = NetworkUtil.readString(message);

                ByteBuf response = ctx.alloc().buffer();

                if (username.isEmpty()) {
                    ctx.writeAndFlush(response.writeByte(2));
                    return;
                }
                if (username.length() > 15) {
                    ctx.writeAndFlush(response.writeByte(3));
                    return;
                }
                if (server.containsUsername(username)) {
                    ctx.writeAndFlush(response.writeByte(1));
                    return;
                }
                ctx.writeAndFlush(response.writeByte(0));

                server.addChannel(ctx.channel(), username);

                ctx.pipeline().replace("handler", "handler", new ServerHandler(server));

//                for (String online : server.getUsernames()) {
//                    server.sendAddOnline(ctx.channel(), online);
//                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ReferenceCountUtil.release(message);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        ctx.close();
    }
}
