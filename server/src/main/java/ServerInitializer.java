import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;


/**
 * Created by Jason MacKeigan on 2017-06-29 at 3:27 PM
 */
public class ServerInitializer extends ChannelInitializer<SocketChannel> {

    private final Server server;

    public ServerInitializer(Server server) {
        this.server = server;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(server.getSslContext().newHandler(ch.alloc()));

        pipeline.addLast(new LengthFieldBasedFrameDecoder(8192, 0, 4, 0, 4));
        pipeline.addLast(new ByteArrayDecoder());
        pipeline.addLast(new LengthFieldPrepender(4));
        pipeline.addLast(new ByteArrayEncoder());
        pipeline.addLast("handler", new ServerLoginHandler(server));
    }
}
