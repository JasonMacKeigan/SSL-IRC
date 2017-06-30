import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

/**
 * Created by Jason MacKeigan on 2017-06-30 at 3:24 AM
 */
public class NetworkUtil {

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    public static String readString(ByteBuf buffer) {
        byte[] payload = new byte[buffer.readShort()];

        buffer.readBytes(payload);

        return new String(payload, DEFAULT_CHARSET);
    }

    public static ByteBuf writeString(ByteBuf buffer, String text) {
        return buffer.writeShort(text.length()).writeBytes(text.getBytes(DEFAULT_CHARSET));
    }

}
