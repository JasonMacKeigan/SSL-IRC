import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.BootstrapConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.charset.Charset;
import java.util.concurrent.*;

/**
 * Created by Jason MacKeigan on 2017-06-29 at 4:08 PM
 */
public class Client {

    private final Version version = new Version(1.0);

    private final SslContext context;

    private final ClientFrame frame;

    private Host host = Host.LOCAL;

    private String username = String.format("Guest%s", ThreadLocalRandom.current().nextInt(1_000, 10_000));

    private final ScheduledExecutorService networkService = Executors.newSingleThreadScheduledExecutor();

    private Channel channel;

    public Client(ClientFrame frame) throws Exception {
        this.frame = frame;
        context = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    }

    public void afterFrameInit() {
        frame.setUsernameFieldText(username);
    }

    public void connect() {
        EventLoopGroup boss = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap().channel(NioSocketChannel.class).group(boss).handler(new LoggingHandler(LogLevel.INFO)).
                handler(new ClientInitializer(this)).
                option(ChannelOption.TCP_NODELAY, true).
                option(ChannelOption.SO_KEEPALIVE, true);

        networkService.submit(() -> {
            try {
                channel = bootstrap.connect(host.getHostname(), host.getPort()).sync().channel();

                channel.pipeline().get(SslHandler.class).handshakeFuture().addListener(future -> {
                    writeUsername();
                });

                while (channel.isOpen()) {
                    TimeUnit.SECONDS.sleep(1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                boss.shutdownGracefully();
            }
        });
    }

    public void close() {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }

    public void logout() {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }

    public void readMessage(String message) {
        System.out.println(String.format("(incoming) %s:%s", "NAME", message));
    }

    public void writeUsername() {
        if (channel == null) {
            System.out.println("Channel is not open, unable to write to server.");
            return;
        }
        channel.writeAndFlush(channel.alloc().buffer().writeShort(username.length()).
                writeBytes(username.getBytes(Charset.forName("UTF-8"))));
    }

    public void writeMessage(String message) {
        if (channel == null) {
            System.out.println("Channel is not open, unable to write to server.");
            return;
        }
        channel.writeAndFlush(channel.alloc().buffer().writeByte(0).writeShort(message.length()).writeBytes(message.getBytes(Charset.forName("UTF-8"))));
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public final String getUsername() {
        return username;
    }

    public final SslContext getSslContext() {
        return context;
    }

    public final Host getHost() {
        return host;
    }

    public final Version getVersion() {
        return version;
    }

    public final ClientFrame getFrame() {
        return frame;
    }

    public final Channel getChannel() {
        return channel;
    }
}
