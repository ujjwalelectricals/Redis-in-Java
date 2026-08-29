import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public final class DelCommand implements Command {
    private final ExpiringStore store;
    public DelCommand(ExpiringStore store) { this.store = store; }
    @Override public String name() { return "DEL"; }
    @Override public void execute(List<String> args, OutputStream out) throws IOException {
        if (args.size() != 1) { RESP.error(out, "wrong number of arguments for 'del'"); return; }
        RESP.integer(out, store.delete(args.get(0)) ? 1 : 0);
    }
}
