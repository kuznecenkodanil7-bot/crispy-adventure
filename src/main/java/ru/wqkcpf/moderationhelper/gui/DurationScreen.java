package ru.wqkcpf.moderationhelper.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import ru.wqkcpf.moderationhelper.ModerationHelperClient;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class DurationScreen extends Screen {
    private static final Pattern DURATION = Pattern.compile("^(perm|forever|навсегда|\\d+[dh])$", Pattern.CASE_INSENSITIVE);

    private final String nick;
    private final String punishment;
    private final String reason;
    private final List<String> durations;
    private final Path tempScreenshot;
    private TextFieldWidget durationField;

    public DurationScreen(String nick, String punishment, String reason, List<String> durations, Path tempScreenshot) {
        super(Text.literal("Duration"));
        this.nick = nick;
        this.punishment = punishment;
        this.reason = reason;
        this.durations = normalizeDurations(durations);
        this.tempScreenshot = tempScreenshot;
    }

    @Override
    protected void init() {
        int buttonW = 420;
        int buttonH = 24;
        int centerX = width / 2;
        int startY = Math.max(45, height / 2 - 125);

        int y = startY + 66;
        for (String duration : durations) {
            String label = formatDurationLabel(duration);
            addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> executePunishment(duration))
                    .dimensions(centerX - buttonW / 2, y, buttonW, buttonH).build());
            y += 30;
        }

        durationField = new TextFieldWidget(textRenderer, centerX - buttonW / 2, y + 8, buttonW, 22, Text.literal("time"));
        durationField.setMaxLength(20);
        durationField.setText(durations.isEmpty() ? "1d" : durations.get(0));
        addDrawableChild(durationField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Выдать наказание со своим временем"), button -> executeCustomDuration())
                .dimensions(centerX - buttonW / 2, y + 36, buttonW, buttonH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), button -> client.setScreen(new ReasonScreen(nick, punishment, tempScreenshot)))
                .dimensions(centerX - 90, y + 66, 180, buttonH).build());
    }

    private List<String> normalizeDurations(List<String> source) {
        Set<String> set = new LinkedHashSet<>();
        if (source != null) {
            for (String duration : source) {
                if (duration != null && !duration.isBlank()) set.add(duration.trim());
            }
        }
        if (set.isEmpty()) set.add("1d");
        return new ArrayList<>(set);
    }

    private String formatDurationLabel(String duration) {
        if (duration.equalsIgnoreCase("perm") || duration.equalsIgnoreCase("forever") || duration.equalsIgnoreCase("навсегда")) {
            return "Навсегда / perm";
        }
        return duration;
    }

    private void executeCustomDuration() {
        String duration = durationField.getText().trim().toLowerCase();
        if (!DURATION.matcher(duration).matches()) {
            ModerationHelperClient.clientMessage("Неверное время. Пример: 12h, 7d, 30d или perm.");
            return;
        }
        executePunishment(duration);
    }

    private void executePunishment(String selectedDuration) {
        if (client == null || client.getNetworkHandler() == null) return;

        String duration = selectedDuration.trim().equalsIgnoreCase("навсегда") ? "perm" : selectedDuration.trim().toLowerCase();
        String command = punishment.toLowerCase() + " " + nick + " " + duration + " " + reason;
        client.getNetworkHandler().sendChatCommand(command.trim());

        ModerationHelperClient.STATS.increment(punishment);
        ModerationHelperClient.RECENT_PLAYERS.add(nick);
        ModerationHelperClient.SCREENSHOTS.finalizeScreenshot(tempScreenshot, nick, punishment, duration, reason);

        if (punishment.equalsIgnoreCase("ipban") && !reason.trim().startsWith("3.8")) {
            ModerationHelperClient.OBS.stopRecording();
        }

        client.setScreen(null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);

        int panelW = 460;
        int panelH = Math.min(height - 40, 390);
        int x = (width - panelW) / 2;
        int y = Math.max(20, height / 2 - panelH / 2);
        ScreenUtil.darkPanel(context, x, y, panelW, panelH);

        ScreenUtil.centerTitle(context, client, "Кого хочешь наказать: §f" + nick, x, y + 14, panelW);
        ScreenUtil.centerMuted(context, client, "Выбери срок наказания", x, y + 38, panelW);
        ScreenUtil.muted(context, client, "Категория: §f" + categoryTitle() + " §7| Причина: §f" + reason, x + 20, y + 58);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private String categoryTitle() {
        return switch (punishment.toLowerCase()) {
            case "mute" -> "Мут";
            case "ban" -> "Бан";
            case "ipban" -> "IP-бан";
            default -> punishment;
        };
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
