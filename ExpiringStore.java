import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Thread-safe key/value store with Redis-style millisecond expirations. */
public final class ExpiringStore implements AutoCloseable {
    private static final long NO_EXPIRY = -1L;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "miniredis-expiry");
        t.setDaemon(true);
        return t;
    });

    public ExpiringStore() {
        reaper.scheduleAtFixedRate(this::removeExpired, 100, 100, TimeUnit.MILLISECONDS);
    }

    public void set(String key, String value, long ttlMillis) {
        long expiresAt = ttlMillis < 0 ? NO_EXPIRY : System.currentTimeMillis() + ttlMillis;
        entries.put(key, new Entry(value, expiresAt));
    }

    public String get(String key) {
        Entry entry = entries.get(key);
        if (entry == null) return null;
        if (expired(entry)) {
            entries.remove(key, entry);
            return null;
        }
        return entry.value;
    }

    public boolean exists(String key) {
        return get(key) != null;
    }

    public boolean delete(String key) {
        return entries.remove(key) != null;
    }

    /** Returns remaining TTL in milliseconds, -1 for no expiry, -2 for missing. */
    public long ttlMillis(String key) {
        Entry entry = entries.get(key);
        if (entry == null) return -2;
        if (expired(entry)) {
            entries.remove(key, entry);
            return -2;
        }
        if (entry.expiresAt == NO_EXPIRY) return -1;
        return Math.max(0, entry.expiresAt - System.currentTimeMillis());
    }

    private boolean expired(Entry entry) {
        return entry.expiresAt != NO_EXPIRY && entry.expiresAt <= System.currentTimeMillis();
    }

    private void removeExpired() {
        long now = System.currentTimeMillis();
        entries.entrySet().removeIf(e -> e.getValue().expiresAt != NO_EXPIRY && e.getValue().expiresAt <= now);
    }

    @Override
    public void close() {
        reaper.shutdownNow();
    }

    private record Entry(String value, long expiresAt) {}
}
