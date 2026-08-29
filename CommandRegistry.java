import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Central registry that maps Redis command names to implementations. */
public final class CommandRegistry {
    private final Map<String, Command> commands = new HashMap<>();

    public void register(Command command) {
        commands.put(command.name().toUpperCase(Locale.ROOT), command);
    }

    public void execute(List<String> request, OutputStream out) throws IOException {
        if (request == null || request.isEmpty()) {
            RESP.error(out, "empty command");
            return;
        }

        String name = request.get(0).toUpperCase(Locale.ROOT);
        Command command = commands.get(name);
        if (command == null) {
            RESP.error(out, "unknown command '" + name + "'");
            return;
        }

        command.execute(request.subList(1, request.size()), out);
    }
}
