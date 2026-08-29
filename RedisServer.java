import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisServer {
    private static final int PORT = 6379;
    private static final Map<String, String> store = new ConcurrentHashMap<>();
    private static final ExecutorService clients = Executors.newVirtualThreadPerTaskExecutor();

    public static void main(String[] args) throws IOException {
        System.out.println("Starting MiniRedis on port " + PORT + "...");
        try (ServerSocket server = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = server.accept();
                clients.submit(() -> handleClient(socket));
            }
        }
    }

    private static void handleClient(Socket socket) {
        try (socket; InputStream in = socket.getInputStream(); OutputStream out = socket.getOutputStream()) {
            while (true) {
                List<String> command = RESP.readArray(in);
                if (command == null || command.isEmpty()) return;
                execute(command, out);
                out.flush();
            }
        } catch (IOException ignored) {
            // Client disconnected or sent malformed data.
        }
    }

    private static void execute(List<String> command, OutputStream out) throws IOException {
        String name = command.get(0).toUpperCase();
        switch (name) {
            case "PING" -> RESP.simple(out, command.size() > 1 ? command.get(1) : "PONG");
            case "SET" -> {
                if (command.size() != 3) { RESP.error(out, "wrong number of arguments for 'set'"); return; }
                store.put(command.get(1), command.get(2));
                RESP.simple(out, "OK");
            }
            case "GET" -> {
                if (command.size() != 2) { RESP.error(out, "wrong number of arguments for 'get'"); return; }
                RESP.bulk(out, store.get(command.get(1)));
            }
            case "DEL" -> {
                if (command.size() != 2) { RESP.error(out, "wrong number of arguments for 'del'"); return; }
                RESP.simple(out, store.remove(command.get(1)) != null ? "1" : "0");
            }
            case "EXISTS" -> {
                if (command.size() != 2) { RESP.error(out, "wrong number of arguments for 'exists'"); return; }
                RESP.simple(out, store.containsKey(command.get(1)) ? "1" : "0");
            }
            case "COMMAND" -> RESP.simple(out, "OK");
            default -> RESP.error(out, "unknown command '" + name + "'");
        }
    }
}
