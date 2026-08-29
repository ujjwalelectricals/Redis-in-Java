import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public final class ExistsCommand implements Command {
    private final Map<String, String> store;
    public ExistsCommand(Map<String, String> store) { this.store = store; }
    @Override public String name() { return "EXISTS"; }
    @Override public void execute(List<String> args, OutputStream out) throws IOException {
        if (args.size() != 1) { RESP.error(out, "wrong number of arguments for 'exists'"); return; }
        RESP.integer(out, store.containsKey(args.get(0)) ? 1 : 0);
    }
}
