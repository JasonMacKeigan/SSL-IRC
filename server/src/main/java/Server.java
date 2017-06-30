import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.DefaultEventExecutor;

import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Jason MacKeigan on 2017-06-29 at 3:24 PM
 */
public class Server {

    private static final int MESSAGE_SECONDS_DELAY = 5;

    private final ChannelGroup channels = new DefaultChannelGroup(new DefaultEventExecutor());

    private final Map<ChannelId, Long> messageTimestamp = new HashMap<>();

    private final Map<ChannelId, String> usernames = new HashMap<>();

    private final ScheduledExecutorService networkService = Executors.newSingleThreadScheduledExecutor();

    private final SelfSignedCertificate certificate;

    private final SslContext context;

    public Server() throws Exception {
        certificate = new SelfSignedCertificate();

        context = SslContextBuilder.forServer(certificate.certificate(), certificate.privateKey()).build();

        NioEventLoopGroup boss = new NioEventLoopGroup();

        NioEventLoopGroup worker = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap().group(boss, worker).
                channel(NioServerSocketChannel.class).
                handler(new LoggingHandler(LogLevel.INFO)).
                childHandler(new ServerInitializer(this)).
                childOption(ChannelOption.TCP_NODELAY, true);

        networkService.submit(() -> {
            try {
                bootstrap.bind(4321).sync().channel().closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                Arrays.asList(boss, worker).forEach(EventLoopGroup::shutdownGracefully);
            }
        });
    }

    public void readMessage(Channel from, String message) {
        if (!channels.contains(from)) {
            from.close();
            return;
        }
        long lastMessage = TimeUnit.NANOSECONDS.toSeconds(messageTimestamp.getOrDefault(from.id(), 0L));

        if (System.nanoTime() - lastMessage < MESSAGE_SECONDS_DELAY) {
            writeMessage(from, String.format("You recently sent a message, please wait %s seconds.", lastMessage));
            return;
        }
        String username = usernames.get(from.id());

        if (username == null) {
            return;
        }
        for (Channel channel : channels) {
            writeMessage(channel, String.format("%s:%s", username, message));
        }
    }

    public void writeMessage(Channel to, String message) {
        ByteBuf buffer = to.alloc().buffer().writeByte(0);

        NetworkUtil.writeString(buffer, message);

        to.writeAndFlush(buffer);
    }

    public void addChannel(Channel channel, String username) {
        channels.add(channel);
        messageTimestamp.put(channel.id(), 0L);
        usernames.put(channel.id(), username);

        for (Channel c : channels) {
            sendAddOnline(c, username);
        }
    }

    public void removeChannel(Channel channel) {
        channels.remove(channel);
        messageTimestamp.remove(channel.id());

        String removedUsername = usernames.remove(channel.id());

        if (removedUsername != null) {
            for (Channel c : channels) {
                sendRemoveOnline(c, removedUsername);
            }
        }
    }

    public void sendRemoveOnline(Channel channel, String username) {
        channel.writeAndFlush(NetworkUtil.writeString(channel.alloc().buffer().writeByte(3), username));
    }

    public void sendAddOnline(Channel channel, String username) {
        channel.writeAndFlush(NetworkUtil.writeString(channel.alloc().buffer().writeByte(2), username));
    }

    public boolean containsUsername(String username) {
        return usernames.values().stream().anyMatch(existing -> existing.equalsIgnoreCase(username));
    }

    public Collection<String> getUsernames() {
        return usernames.values();
    }

    public boolean containsChannel(Channel channel) {
        return channels.contains(channel);
    }

    public ChannelGroup getChannels() {
        return channels;
    }

    public SslContext getSslContext() {
        return context;
    }
}
