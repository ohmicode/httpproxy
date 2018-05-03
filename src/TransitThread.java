import java.io.*;
import java.net.Socket;

public class TransitThread extends Thread {
    private static final String EOL1 = "\n";
    private static final String EOL2 = "\r\n";

    private Socket socket;
    private int profileId;
    private String serverName;
    private int portNumber;

    public TransitThread(Socket socket, int profileId, String serverName, int portNumber) {
        super("TransitThread");
        this.socket = socket;
        this.profileId = profileId;
        this.serverName = serverName;
        this.portNumber = portNumber;
    }

    public void run() {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String inputLine = readStream(socket.getInputStream());
            String response = transit(inputLine);
            out.println(response);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readStream(InputStream stream) throws IOException {
        byte[] buff = new byte[64 * 1024];
        String line = "";
        int length;
        do {
            length = stream.read(buff);
            line += new String(buff, 0, length);
        } while (length == buff.length);
        return line;
    }

    private String injectProfileId(String request) {
        int eol = request.indexOf(EOL1);
        if (eol < 0) eol = request.indexOf(EOL2);
        if (eol < 0) return request;

        String head = request.substring(0, eol);
        String bottom = request.substring(eol);
        StringBuilder builder = new StringBuilder(head)
                .append(EOL1)
                .append("Profile-Id: ").append(profileId)
                .append(EOL1)
                .append("Profile: {\"userProfileId\": ").append(profileId).append("}")
                .append(bottom);
        return builder.toString();
    }

    private String transit(String httpRequest) {
        String request = injectProfileId(httpRequest);
        String response = null;

        System.out.println("\n\n==================================================================================");
        System.out.println(request);

        try (Socket socket = new Socket(serverName, portNumber)) {
            socket.getOutputStream().write(request.getBytes());
            response = readStream(socket.getInputStream());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        System.out.println("=========================================");
        System.out.println(response);
        return response;
    }
}
