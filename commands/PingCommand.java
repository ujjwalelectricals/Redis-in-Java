import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public final class PingCommand implements Command {
    @Override public String name() { return "PING"; }
    @Override public void execute(List<String> args, OutputStream out) throws IOException {
        if (args.size() > 1) { RESP.error(out, "wrong number of arguments for 'ping'"); return; }
        RESP.simple(out, args.isEmpty() ? "PONG" : args.get(0));
    }
}
