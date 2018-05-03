import java.io.IOException;
import java.net.ServerSocket;

public class ProxyCore {

    private static final int DEFAULT_PORT_NUMBER = 8088;
    private static final int DEFAULT_PROFILE_ID  = 5563037;
    private static final String SERVER_NAME  = "localhost";
    private static final int SERVER_PORT  = 8080;

    public static void main(String[] args) {
        int profileId = (args.length > 0) ? parseProfileId(args[0]) : DEFAULT_PROFILE_ID;
        runServer(profileId);
    }

    private static int parseProfileId(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            return DEFAULT_PROFILE_ID;
        }
    }

    private static void runServer(int profileId) {
        try (ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT_NUMBER)) {
            boolean listening = true;

            while (listening) {
                new TransitThread(serverSocket.accept(), profileId, SERVER_NAME, SERVER_PORT)
                        .start();
            }

        } catch (IOException ioe) {
            System.err.println("Could not listen on port " + DEFAULT_PORT_NUMBER);
            System.exit(-1);
        }
    }
}
