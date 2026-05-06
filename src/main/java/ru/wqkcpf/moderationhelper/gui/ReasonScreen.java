package ru.wqkcpf.moderationhelper.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import ru.wqkcpf.moderationhelper.ModerationHelperClient;
import ru.wqkcpf.moderationhelper.config.ModConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ReasonScreen extends Screen {
    private final String nick;
    private final String punishment;
    private final Path tempScreenshot;
    private TextFieldWidget customReasonField;

    public ReasonScreen(String nick, String punishment, Path tempScreenshot) {
        super(Text.literal("Reason"));
        this.nick = nick;
        this.punishment = punishment;
        this.tempScreenshot = tempScreenshot;
    }

    @Override
    protected void init() {
        int buttonW = 420;
        int buttonH = 24;
        int centerX = width / 2;
        int startY = Math.max(30, height / 2 - 205);

        List<ModConfig.ReasonOption> reasons = applicableReasons();
        int y = startY + 58;
        for (ModConfig.ReasonOption option : reasons) {
            String label = option.code() + " — " + shortName(option.description());
            addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> onReasonSelected(option))
                    .dimensions(centerX - buttonW / 2, y, buttonW, buttonH).build());
            y += 30;
        }

        int fieldY = Math.min(height - 72, y + 8);
        customReasonField = new TextFieldWidget(textRenderer, centerX - buttonW / 2, fieldY, buttonW, 22, Text.literal("Своя причина"));
        customReasonField.setMaxLength(120);
        addDrawableChild(customReasonField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Своя причина → выбор времени"), button -> customReason())
                .dimensions(centerX - buttonW / 2, fieldY + 28, buttonW, buttonH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), button -> client.setScreen(new PunishmentScreen(nick, tempScreenshot)))
                .dimensions(centerX - 90, fieldY + 58, 180, buttonH).build());
    }

    private void onReasonSelected(ModConfig.ReasonOption option) {
        String reason = option.code();
        if (punishment.equalsIgnoreCase("warn")) {
            executePunishment(reason, "");
            return;
        }
        client.setScreen(new DurationScreen(nick, punishment, reason, option.durationSuggestions(), tempScreenshot));
    }

    private void customReason() {
        String reason = customReasonField.getText().trim();
        if (reason.isBlank()) {
            ModerationHelperClient.clientMessage("Введите свою причину или выберите причину кнопкой.");
            return;
        }
        if (punishment.equalsIgnoreCase("warn")) {
            executePunishment(reason, "");
            return;
        }
        client.setScreen(new DurationScreen(nick, punishment, reason, collectCategoryDurations(), tempScreenshot));
    }

    private void executePunishment(String reason, String duration) {
        if (client == null || client.getNetworkHandler() == null) return;

        String command;
        if (punishment.equalsIgnoreCase("warn")) {
            command = "warn " + nick + " " + reason;
        } else {
            command = punishment.toLowerCase() + " " + nick + " " + duration + " " + reason;
        }

        client.getNetworkHandler().sendChatCommand(command.trim());
        ModerationHelperClient.STATS.increment(punishment);
        ModerationHelperClient.RECENT_PLAYERS.add(nick);
        ModerationHelperClient.SCREENSHOTS.finalizeScreenshot(tempScreenshot, nick, punishment, duration, reason);

        if (punishment.equalsIgnoreCase("ipban") && !reason.trim().startsWith("3.8")) {
            ModerationHelperClient.OBS.stopRecording();
        }

        client.setScreen(null);
    }

    private List<String> collectCategoryDurations() {
        Set<String> durations = new LinkedHashSet<>();
        for (ModConfig.ReasonOption option : applicableReasons()) {
            if (option.durationSuggestions() == null) continue;
            for (String duration : option.durationSuggestions()) {
                if (duration != null && !duration.isBlank()) durations.add(duration.trim());
            }
        }
        if (durations.isEmpty()) {
            durations.add("1h");
            durations.add("12h");
            durations.add("1d");
            durations.add("7d");
            durations.add("30d");
            durations.add("perm");
        }
        return new ArrayList<>(durations);
    }

    private List<ModConfig.ReasonOption> applicableReasons() {
        List<ModConfig.ReasonOption> result = new ArrayList<>();
        for (ModConfig.ReasonOption option : ModerationHelperClient.CONFIG.quickReasons) {
            if (option.supports(punishment)) result.add(option);
        }
        return result;
    }

    private String shortName(String text) {
        if (text == null || text.isBlank()) return "";
        String normalized = text
                .replace("Запрещено ", "")
                .replace("Запрещены ", "")
                .replace("Запрещена ", "")
                .replace("Запрещено", "")
                .trim();
        int max = 45;
        return normalized.length() <= max ? normalized : normalized.substring(0, max - 1) + "…";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);

        int panelW = 460;
        int panelH = Math.min(height - 30, 610);
        int x = (width - panelW) / 2;
        int y = Math.max(15, height / 2 - panelH / 2);
        ScreenUtil.darkPanel(context, x, y, panelW, panelH);

        ScreenUtil.centerTitle(context, client, "Кого хочешь наказать: §f" + nick, x, y + 14, panelW);
        ScreenUtil.centerMuted(context, client, headerText(), x, y + 38, panelW);
        ScreenUtil.muted(context, client, "Нажми причину или введи свою снизу.", x + 20, y + 58);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private String headerText() {
        return switch (punishment.toLowerCase()) {
            case "warn" -> "Выбери причину предупреждения";
            case "mute" -> "Выбери причину мута";
            case "ban" -> "Выбери причину бана";
            case "ipban" -> "Выбери причину IP-бана";
            default -> "Выбери причину";
        };
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
