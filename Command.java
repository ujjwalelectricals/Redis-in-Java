import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/** A single MiniRedis command implementation. */
public interface Command {
    String name();
    void execute(List<String> args, OutputStream out) throws IOException;
}
