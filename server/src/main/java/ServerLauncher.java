/**
 * Created by Jason MacKeigan on 2017-06-29 at 3:25 PM
 */
public class ServerLauncher {

    public static void main(String... args) {
        try {
            Server server = new Server();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
