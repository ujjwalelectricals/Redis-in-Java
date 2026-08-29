import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public final class GetCommand implements Command {
    private final Map<String, String> store;
    public GetCommand(Map<String, String> store) { this.store = store; }
    @Override public String name() { return "GET"; }
    @Override public void execute(List<String> args, OutputStream out) throws IOException {
        if (args.size() != 1) { RESP.error(out, "wrong number of arguments for 'get'"); return; }
        RESP.bulk(out, store.get(args.get(0)));
    }
}
