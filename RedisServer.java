import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
        CommandRegistry registry = createRegistry();

        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("MiniRedis ready. Commands: PING SET GET DEL EXISTS");
            while (true) {
                Socket socket = server.accept();
                clients.submit(() -> handleClient(socket, registry));
            }
        }
    }

    private static CommandRegistry createRegistry() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new PingCommand());
        registry.register(new SetCommand(store));
        registry.register(new GetCommand(store));
        registry.register(new DelCommand(store));
        registry.register(new ExistsCommand(store));
        return registry;
    }

    private static void handleClient(Socket socket, CommandRegistry registry) {
        try (socket; InputStream in = socket.getInputStream(); OutputStream out = socket.getOutputStream()) {
            while (true) {
                List<String> request = RESP.readArray(in);
                if (request == null) return;
                registry.execute(request, out);
                out.flush();
            }
        } catch (IOException ignored) {
            // Client disconnected or sent malformed data.
        }
    }
}
