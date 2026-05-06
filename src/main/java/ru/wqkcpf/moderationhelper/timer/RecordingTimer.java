package ru.wqkcpf.moderationhelper.timer;

import java.time.Duration;
import java.time.Instant;

public class RecordingTimer {
    private Instant startedAt;

    public void start() {
        startedAt = Instant.now();
    }

    public void stop() {
        startedAt = null;
    }

    public boolean isRunning() {
        return startedAt != null;
    }

    public String getFormattedElapsed() {
        if (startedAt == null) return "00:00";
        long seconds = Duration.between(startedAt, Instant.now()).toSeconds();
        long minutes = seconds / 60;
        long rest = seconds % 60;
        return String.format("%02d:%02d", minutes, rest);
    }
}
