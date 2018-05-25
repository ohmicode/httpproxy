import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TransitThread extends Thread {
    private static final String EOL1 = "\n";
    private static final String EOL2 = "\r\n";

    private Socket socket;
    private Integer profileId;
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
            String inputLine = readSocketStream(socket.getInputStream());
            if (!inputLine.isEmpty()) {
                String response = transit(inputLine);
                out.println(response);
            } else {
                System.out.println(">>>>>>> Empty request processed <<<<<<<");
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readSocketStream(InputStream stream) throws IOException {
        byte[] buff = new byte[64 * 1024];
        String line = "";
        int length;
        do {
            length = stream.read(buff);
            if (length > 0) {
                line += new String(buff, 0, length);
            } else {
                System.out.printf(">>>>>>> read %s bytes <<<<<<<%n", length);
            }
        } while (length == buff.length);
        return line;
    }

    private String readHttpStream(InputStream stream) throws IOException {
        BufferedReader buffered = new BufferedReader(new InputStreamReader(stream));
        return buffered.lines().collect(Collectors.joining("\r\n"));
    }

    private String readHttpStream2(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder result = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    @Deprecated
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
                .append("Profile: {\"userProfileId\": ").append(profileId).append(", \"type\": \"ADMIN\"}")
                .append(bottom);
        return builder.toString();
    }

    @Deprecated
    private String transitOld(String httpRequest) {
        String request = injectProfileId(httpRequest);
        String response = null;

        System.out.println("\n\n==================================================================================");
        System.out.println(request);

        try (Socket socket = new Socket(serverName, portNumber)) {
            socket.getOutputStream().write(request.getBytes());
            response = readSocketStream(socket.getInputStream());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        System.out.println("=========================================");
        System.out.println(response);
        return response;
    }

    private String transit(String httpRequest) throws IOException {
        System.out.println("\n\n==================================================================================");
        System.out.println(httpRequest);

        String method = extractMethod(httpRequest);
//        System.out.println("method = " + method);
        String api = extractApi(httpRequest);
//        System.out.println("api = " + api);
        Map<String, String> headers = extractHeaders(httpRequest);
        String body = extractBody(httpRequest);
//        System.out.println("body = " + body);

        URL url = new URL("http://" + serverName + ":" + portNumber + api);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method);
        headers.forEach((key, value) -> con.setRequestProperty(key, value));
        con.setRequestProperty("Profile-Id", profileId.toString());
        con.setRequestProperty("Profile", "{\"userProfileId\": " + profileId + ", \"type\": \"ADMIN\"}");
        if (!body.isEmpty()) {
            con.setDoOutput(true);
            con.getOutputStream().write(body.getBytes());
        }

        String response = readSocketStream(con.getInputStream());

        System.out.println("=========================================");
        System.out.println(response);
        return response;
    }

    private String extractMethod(String request) {
        int apiPos = request.indexOf(' ');
        return request.substring(0, apiPos);
    }

    private String extractApi(String request) {
        int apiPos = request.indexOf(' ');
        int httpPos = request.indexOf(' ', apiPos+1);
        return request.substring(apiPos+1, httpPos);
    }

    private String extractBody(String request) {
        int bodyPos = request.indexOf("\r\n\r\n");
        return request.substring(bodyPos).trim();
    }

    private Map<String, String> extractHeaders(String request) {
        Map<String, String> headers = new HashMap<>();

        String[] lines = request.split("\r\n");
        for (int i=1; i<lines.length-1; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                String[] pair = line.split(":");
                headers.put(pair[0].trim(), pair[1].trim());
            }
        }

        return headers;
    }
}
