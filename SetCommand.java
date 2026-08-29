import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public final class SetCommand implements Command {
    private final ExpiringStore store;
    public SetCommand(ExpiringStore store) { this.store = store; }
    @Override public String name() { return "SET"; }

    @Override public void execute(List<String> args, OutputStream out) throws IOException {
        if (args.size() < 2 || args.size() > 4) {
            RESP.error(out, "wrong number of arguments for 'set'"); return;
        }
        String key = args.get(0), value = args.get(1);
        long ttlMillis = -1;
        if (args.size() == 3) {
            RESP.error(out, "syntax error"); return;
        }
        if (args.size() == 4) {
            String mode = args.get(2).toUpperCase();
            try {
                long ttl = Long.parseLong(args.get(3));
                if (ttl <= 0) { RESP.error(out, "invalid expire time in 'set'"); return; }
                if (mode.equals("EX")) ttlMillis = Math.multiplyExact(ttl, 1000L);
                else if (mode.equals("PX")) ttlMillis = ttl;
                else { RESP.error(out, "syntax error"); return; }
            } catch (NumberFormatException | ArithmeticException e) {
                RESP.error(out, "invalid expire time in 'set"); return;
            }
        }
        store.set(key, value, ttlMillis);
        RESP.simple(out, "OK");
    }
}
