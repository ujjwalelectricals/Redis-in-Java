import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedisServerTest {
    public static void main(String[] args) throws Exception {
        Map<String, String> store = new ConcurrentHashMap<>();
        CommandRegistry registry = new CommandRegistry();
        registry.register(new PingCommand());
        registry.register(new SetCommand(store));
        registry.register(new GetCommand(store));
        registry.register(new DelCommand(store));
        registry.register(new ExistsCommand(store));

        assertResponse(registry, List.of("PING"), "+PONG\r\n");
        assertResponse(registry, List.of("PING", "hello"), "+hello\r\n");
        assertResponse(registry, List.of("SET", "name", "RAMPilot"), "+OK\r\n");
        assertResponse(registry, List.of("GET", "name"), "$7\r\nRAMPilot\r\n");
        assertResponse(registry, List.of("EXISTS", "name"), ":1\r\n");
        assertResponse(registry, List.of("DEL", "name"), ":1\r\n");
        assertResponse(registry, List.of("GET", "name"), "$-1\r\n");
        assertResponse(registry, List.of("EXISTS", "name"), ":0\r\n");

        System.out.println("ALL REDIS COMMAND TESTS PASSED");
    }

    private static void assertResponse(CommandRegistry registry, List<String> request, String expected) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        registry.execute(request, out);
        String actual = out.toString(StandardCharsets.UTF_8);
        if (!expected.equals(actual)) {
            throw new AssertionError(request + " expected " + printable(expected) + " but got " + printable(actual));
        }
        System.out.println("PASS  " + request + " -> " + printable(actual));
    }

    private static String printable(String value) {
        return value.replace("\r", "\\r").replace("\n", "\\n");
    }
}
