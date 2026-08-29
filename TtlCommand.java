import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public final class TtlCommand implements Command {
    private final ExpiringStore store;
    public TtlCommand(ExpiringStore store) { this.store = store; }
    @Override public String name() { return "TTL"; }
    @Override public void execute(List<String> args, OutputStream out) throws IOException {
        if (args.size() != 1) { RESP.error(out, "wrong number of arguments for 'ttl'"); return; }
        long millis = store.ttlMillis(args.get(0));
        if (millis < 0) {
            RESP.integer(out, millis); // -1 no expiry, -2 missing
        } else {
            RESP.integer(out, Math.max(0, (millis + 999) / 1000));
        }
    }
}
