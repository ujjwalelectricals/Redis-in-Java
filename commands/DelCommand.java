import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public final class DelCommand implements Command {
    private final Map<String, String> store;
    public DelCommand(Map<String, String> store) { this.store = store; }
    @Override public String name() { return "DEL"; }
    @Override public void execute(List<String> args, OutputStream out) throws IOException {
        if (args.size() != 1) { RESP.error(out, "wrong number of arguments for 'del'"); return; }
        RESP.integer(out, store.remove(args.get(0)) != null ? 1 : 0);
    }
}
