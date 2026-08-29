import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExpirationTest {
    public static void main(String[] args) throws Exception {
        try (ExpiringStore store = new ExpiringStore()) {
            CommandRegistry registry = new CommandRegistry();
            registry.register(new SetCommand(store));
            registry.register(new GetCommand(store));
            registry.register(new TtlCommand(store));
            registry.register(new ExistsCommand(store));

            assertResponse(registry, List.of("SET", "ex", "hello", "EX", "1"), "+OK\r\n");
            assertResponse(registry, List.of("GET", "ex"), "$5\r\nhello\r\n");
            assertTtlBetween(registry, List.of("TTL", "ex"), 1, 1);

            Thread.sleep(1250);

            assertResponse(registry, List.of("GET", "ex"), "$-1\r\n");
            assertResponse(registry, List.of("TTL", "ex"), ":-2\r\n");
            assertResponse(registry, List.of("EXISTS", "ex"), ":0\r\n");

            assertResponse(registry, List.of("SET", "px", "world", "PX", "300"), "+OK\r\n");
            assertResponse(registry, List.of("GET", "px"), "$5\r\nworld\r\n");
            Thread.sleep(450);
            assertResponse(registry, List.of("GET", "px"), "$-1\r\n");

            assertResponse(registry, List.of("SET", "permanent", "yes"), "+OK\r\n");
            assertResponse(registry, List.of("TTL", "permanent"), ":-1\r\n");

            System.out.println("ALL EXPIRATION TESTS PASSED");
        }
    }

    private static void assertResponse(CommandRegistry registry, List<String> request, String expected) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        registry.execute(request, out);
        String actual = out.toString(StandardCharsets.UTF_8);
        if (!expected.equals(actual)) throw new AssertionError(request + " -> " + actual);
        System.out.println("PASS " + request);
    }

    private static void assertTtlBetween(CommandRegistry registry, List<String> request, long min, long max) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        registry.execute(request, out);
        String actual = out.toString(StandardCharsets.UTF_8);
        long ttl = Long.parseLong(actual.substring(1).trim());
        if (ttl < min || ttl > max) throw new AssertionError(request + " -> " + ttl);
        System.out.println("PASS " + request + " -> " + ttl + "s");
    }
}
