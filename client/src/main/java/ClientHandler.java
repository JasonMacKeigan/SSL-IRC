import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import javafx.application.Platform;

/**
 * Created by Jason MacKeigan on 2017-06-29 at 4:08 PM
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {

    private final Client client;

    public ClientHandler(Client client) {
        this.client = client;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof byte[]) {
            ByteBuf message = ctx.alloc().buffer().writeBytes((byte[]) msg);

            try {
                int id = message.readByte();

                if (id == 0) {
                    String incomingMessage = NetworkUtil.readString(message);

                    Platform.runLater(() -> client.getFrame().appendMessage(incomingMessage));
                } else if (id == 2) {
                    String usernameToAdd = NetworkUtil.readString(message);

                    Platform.runLater(() -> client.getFrame().addOnline(usernameToAdd));
                } else if (id == 3) {
                    String usernameToRemove = NetworkUtil.readString(message);

                    Platform.runLater(() -> client.getFrame().removeOnline(usernameToRemove));
                }
            } finally {
                ReferenceCountUtil.release(message);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
