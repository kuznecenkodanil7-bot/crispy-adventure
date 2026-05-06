package ru.wqkcpf.moderationhelper.stats;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStats {
    private final Map<String, Integer> counters = new ConcurrentHashMap<>();

    public SessionStats() {
        counters.put("warn", 0);
        counters.put("mute", 0);
        counters.put("ban", 0);
        counters.put("ipban", 0);
    }

    public void increment(String punishment) {
        String key = punishment.toLowerCase(Locale.ROOT);
        counters.compute(key, (k, v) -> v == null ? 1 : v + 1);
    }

    public int get(String punishment) {
        return counters.getOrDefault(punishment.toLowerCase(Locale.ROOT), 0);
    }
}
