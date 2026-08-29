import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public final class SetCommand implements Command {
    private final Map<String, String> store;
    public SetCommand(Map<String, String> store) { this.store = store; }
    @Override public String name() { return "SET"; }
    @Override public void execute(List<String> args, OutputStream out) throws IOException {
        if (args.size() != 2) { RESP.error(out, "wrong number of arguments for 'set'"); return; }
        store.put(args.get(0), args.get(1));
        RESP.simple(out, "OK");
    }
}
