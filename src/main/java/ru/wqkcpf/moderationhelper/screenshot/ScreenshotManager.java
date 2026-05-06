package ru.wqkcpf.moderationhelper.screenshot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import ru.wqkcpf.moderationhelper.ModerationHelperClient;
import ru.wqkcpf.moderationhelper.config.ModConfig;

import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Stream;

public class ScreenshotManager {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());
    private final ModConfig config;

    public ScreenshotManager(ModConfig config) {
        this.config = config;
    }

    public void prepareDirectories() {
        try {
            Files.createDirectories(root());
            Files.createDirectories(root().resolve("temp"));
            Files.createDirectories(root().resolve("warn"));
            Files.createDirectories(root().resolve("mute"));
            Files.createDirectories(root().resolve("ban"));
            Files.createDirectories(root().resolve("ipban"));
            Files.createDirectories(root().resolve("archive"));
        } catch (IOException e) {
            ModerationHelperClient.LOGGER.error("Cannot create screenshot folders", e);
        }
    }

    public Path captureTemp(String nick) {
        prepareDirectories();
        String fileName = sanitize(nick) + "_" + FILE_TIME.format(Instant.now()) + ".png";
        Path output = root().resolve("temp").resolve(fileName);
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            ScreenshotRecorder.takeScreenshot(client.getFramebuffer(), (NativeImage image) -> {
                try (image) {
                    Files.createDirectories(output.getParent());
                    image.writeTo(output);
                    ModerationHelperClient.clientMessage("Скрин сохранён: temp/" + output.getFileName());
                } catch (Exception e) {
                    ModerationHelperClient.LOGGER.error("Cannot write temp screenshot", e);
                    ModerationHelperClient.clientMessage("Не удалось сохранить скриншот.");
                }
            });
            return output;
        } catch (Exception e) {
            ModerationHelperClient.LOGGER.error("Cannot capture screenshot", e);
            ModerationHelperClient.clientMessage("Ошибка скриншота, меню всё равно открыто.");
            return null;
        }
    }

    public void finalizeScreenshot(Path tempPath, String nick, String punishment, String duration, String reason) {
        if (tempPath == null) return;
        try {
            if (Files.notExists(tempPath)) return;
            Path targetDir = root().resolve(punishment.toLowerCase(Locale.ROOT));
            Files.createDirectories(targetDir);
            String fileName = sanitize(nick) + "_" + sanitize(punishment) + "_" + sanitize(duration) + "_" + sanitize(reason) + "_" + FILE_TIME.format(Instant.now()) + ".png";
            Path target = unique(targetDir.resolve(fileName));
            Files.move(tempPath, target, StandardCopyOption.REPLACE_EXISTING);
            ModerationHelperClient.clientMessage("Скрин перенесён: " + punishment + "/" + target.getFileName());
        } catch (Exception e) {
            ModerationHelperClient.LOGGER.error("Cannot finalize screenshot", e);
            ModerationHelperClient.clientMessage("Не удалось перенести скриншот из temp.");
        }
    }

    public void cleanupOldScreenshots() {
        prepareDirectories();
        String mode = config.screenshotCleanupMode == null ? "OFF" : config.screenshotCleanupMode.toUpperCase(Locale.ROOT);
        if (mode.equals("OFF")) return;

        int days = Math.max(1, config.screenshotRetentionDays);
        Instant border = Instant.now().minusSeconds(days * 24L * 60L * 60L);
        try (Stream<Path> stream = Files.walk(root())) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> !path.toString().contains(root().resolve("archive").toString()))
                    .filter(path -> isOlderThan(path, border))
                    .forEach(path -> cleanupOne(path, mode));
        } catch (Exception e) {
            ModerationHelperClient.LOGGER.warn("Screenshot cleanup failed", e);
        }
    }

    private boolean isOlderThan(Path path, Instant border) {
        try {
            FileTime time = Files.getLastModifiedTime(path);
            return time.toInstant().isBefore(border);
        } catch (IOException e) {
            return false;
        }
    }

    private void cleanupOne(Path path, String mode) {
        try {
            if (mode.equals("DELETE")) {
                Files.deleteIfExists(path);
            } else if (mode.equals("ARCHIVE")) {
                Path archive = root().resolve("archive").resolve(path.getFileName());
                Files.createDirectories(archive.getParent());
                Files.move(path, unique(archive), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            ModerationHelperClient.LOGGER.warn("Cannot cleanup screenshot {}", path, e);
        }
    }

    public Path root() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.runDirectory.toPath().resolve(config.screenshotFolder == null || config.screenshotFolder.isBlank()
                ? "moderation_screenshots"
                : config.screenshotFolder);
    }

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) return "none";
        String sanitized = value.replaceAll("[\\\\/:*?\"<>|]+", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_");
        if (sanitized.length() > 90) sanitized = sanitized.substring(0, 90);
        return sanitized;
    }

    private Path unique(Path path) {
        if (Files.notExists(path)) return path;
        String name = path.getFileName().toString();
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }
        for (int i = 2; i < 1000; i++) {
            Path candidate = path.getParent().resolve(base + "_" + i + ext);
            if (Files.notExists(candidate)) return candidate;
        }
        return path;
    }
}
