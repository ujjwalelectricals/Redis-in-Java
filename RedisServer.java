import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedisServer {

    private static final int PORT = 6379;
    // In-memory key-value store
    private static final Map<String, String> store = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Starting mini-Redis server on port " + PORT + "...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());
                // Handle each client in a separate thread
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream()) {

            while (true) {
                List<String> command = parseRespArray(in);
                if (command == null || command.isEmpty()) {
                    break; // Connection closed or invalid request
                }

                String cmdName = command.get(0).toUpperCase();
                
                switch (cmdName) {
                    case "PING":
                        // RESP Simple String: +PONG\r\n
                        out.write("+PONG\r\n".getBytes());
                        break;

                    case "SET":
                        if (command.size() >= 3) {
                            String key = command.get(1);
                            String val = command.get(2);
                            store.put(key, val);
                            out.write("+OK\r\n".getBytes());
                        } else {
                            out.write("-ERR wrong number of arguments for 'SET'\r\n".getBytes());
                        }
                        break;

                    case "GET":
                        if (command.size() >= 2) {
                            String key = command.get(1);
                            String val = store.get(key);
                            if (val == null) {
                                // RESP Null Bulk String: $-1\r\n
                                out.write("$-1\r\n".getBytes());
                            } else {
                                // RESP Bulk String: $<length>\r\n<data>\r\n
                                String response = "$" + val.length() + "\r\n" + val + "\r\n";
                                out.write(response.getBytes());
                            }
                        } else {
                            out.write("-ERR wrong number of arguments for 'GET'\r\n".getBytes());
                        }
                        break;

                    default:
                        String err = "-ERR unknown command '" + cmdName + "'\r\n";
                        out.write(err.getBytes());
                        break;
                }
                out.flush();
            }
        } catch (Exception e) {
            System.out.println("Client disconnected.");
        }
    }

    // Helper method to parse incoming RESP protocol streams
    private static List<String> parseRespArray(InputStream in) throws Exception {
        int firstByte = in.read();
        if (firstByte == -1) return null; // EOF

        // RESP Arrays start with '*'
        if (firstByte == '*') {
            int numElements = Integer.parseInt(readLine(in));
            List<String> elements = new ArrayList<>();
            for (int i = 0; i < numElements; i++) {
                int type = in.read(); // Should be '$' for bulk strings
                if (type == '$') {
                    int length = Integer.parseInt(readLine(in));
                    byte[] bytes = new byte[length];
                    int readTotal = 0;
                    while (readTotal < length) {
                        int count = in.read(bytes, readTotal, length - readTotal);
                        if (count == -1) break;
                        readTotal += count;
                    }
                    readLine(in); // Consume trailing \r\n
                    elements.add(new String(bytes));
                }
            }
            return elements;
        } else {
            // Basic fallback for simple raw string commands (e.g., plain PING)
            StringBuilder sb = new StringBuilder();
            sb.append((char) firstByte);
            sb.append(readLine(in));
            List<String> simpleCmd = new ArrayList<>();
            for (String part : sb.toString().trim().split("\\s+")) {
                if (!part.isEmpty()) simpleCmd.add(part);
            }
            return simpleCmd;
        }
    }

    private static String readLine(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') {
                int next = in.read(); // Read following '\n'
                if (next == '\n' || next == -1) break;
            } else if (b == '\n') {
                break;
            } else {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }
}