import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

/**
 * Created by Jason MacKeigan on 2017-06-29 at 4:08 PM
 */
public class ClientInitializer extends ChannelInitializer<SocketChannel> {

    private final Client client;

    public ClientInitializer(Client client) {
        this.client = client;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(client.getSslContext().newHandler(ch.alloc(), client.getHost().getHostname(), client.getHost().getPort()));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(8192, 0, 4, 0, 4));
        pipeline.addLast(new ByteArrayDecoder());
        pipeline.addLast(new LengthFieldPrepender(4));
        pipeline.addLast(new ByteArrayEncoder());
        pipeline.addLast("handler", new ClientLoginHandler(client));
    }
}
