import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import javafx.application.Platform;

/**
 * Created by Jason MacKeigan on 2017-06-30 at 7:43 AM
 */
public class ClientLoginHandler extends ChannelInboundHandlerAdapter {

    private final Client client;

    public ClientLoginHandler(Client client) {
        this.client = client;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof byte[]) {
            ByteBuf buffer = ctx.alloc().buffer().writeBytes((byte[]) msg);

            try {
                int response = buffer.readByte();

                ClientFrame frame = client.getFrame();

                if (response == 0) {
                    Platform.runLater(frame::showView);

                    ctx.pipeline().replace("handler", "handler", new ClientHandler(client));
                } else {
                    switch (response) {
                        case 1:
                            Platform.runLater(() -> frame.setInfoSceneOutput("This username is already taken."));
                            break;
                        case 2:
                            Platform.runLater(() -> frame.setInfoSceneOutput("Username is empty, must be at least one character."));
                            break;
                        case 3:
                            Platform.runLater(() -> frame.setInfoSceneOutput("Username is too long, cannot exceed 15 characters."));
                            break;
                        default:
                            Platform.runLater(() -> frame.setInfoSceneOutput("Unavailable, please try again later."));
                            break;
                    }
                    ctx.close();
                }
            } finally {
                ReferenceCountUtil.release(buffer);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
