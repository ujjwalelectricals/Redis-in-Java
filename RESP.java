import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Minimal RESP2 reader/writer used by our Redis-compatible server. */
public final class RESP {
    private RESP() {}

    public static List<String> readArray(InputStream in) throws IOException {
        int first = in.read();
        if (first == -1) return null;
        if (first != '*') throw new IOException("Expected RESP array");

        int count = Integer.parseInt(readLine(in));
        if (count < 0 || count > 1024) throw new IOException("Invalid array length");

        List<String> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            if (in.read() != '$') throw new IOException("Expected bulk string");
            int length = Integer.parseInt(readLine(in));
            if (length < 0 || length > 64 * 1024 * 1024) throw new IOException("Invalid bulk length");
            byte[] data = in.readNBytes(length);
            if (data.length != length) throw new IOException("Unexpected end of request");
            if (in.read() != '\r' || in.read() != '\n') throw new IOException("Invalid RESP terminator");
            result.add(new String(data, StandardCharsets.UTF_8));
        }
        return result;
    }

    public static void simple(OutputStream out, String value) throws IOException {
        write(out, "+" + value + "\r\n");
    }

    public static void error(OutputStream out, String value) throws IOException {
        write(out, "-ERR " + value + "\r\n");
    }

    public static void integer(OutputStream out, long value) throws IOException {
        write(out, ":" + value + "\r\n");
    }

    public static void bulk(OutputStream out, String value) throws IOException {
        if (value == null) { write(out, "$-1\r\n"); return; }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        write(out, "$" + bytes.length + "\r\n");
        out.write(bytes);
        write(out, "\r\n");
    }

    private static void write(OutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') {
                if (in.read() != '\n') throw new IOException("Invalid line ending");
                return sb.toString();
            }
            sb.append((char) b);
            if (sb.length() > 4096) throw new IOException("Line too long");
        }
        throw new IOException("Unexpected end of request");
    }
}
