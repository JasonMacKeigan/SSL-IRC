/**
 * Created by Jason MacKeigan on 2017-06-29 at 4:12 PM
 */
public enum Host {
    LOCAL("localhost", "localhost", 4321),
    JASON("Jason (public)", "private", 4321)
    ;

    private final String identifier;

    private final String hostname;

    private final int port;

    Host(String identifier, String hostname, int port) {
        this.identifier = identifier;
        this.hostname = hostname;
        this.port = port;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }
}
