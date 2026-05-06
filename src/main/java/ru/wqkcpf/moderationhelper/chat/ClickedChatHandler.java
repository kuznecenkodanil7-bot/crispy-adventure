package ru.wqkcpf.moderationhelper.chat;

import net.minecraft.client.MinecraftClient;
import ru.wqkcpf.moderationhelper.ModerationHelperClient;
import ru.wqkcpf.moderationhelper.gui.PunishmentScreen;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public final class ClickedChatHandler {
    private static final String[] NO_SCREENSHOT_MARKERS = {
            "Tick Speed", "Reach", "Fighting suspiciously", "Block Interaction"
    };

    private ClickedChatHandler() {}

    public static boolean handleMiddleClick(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;

        String message = ChatMessageTracker.guessMessageByMouseY(mouseY);
        Optional<String> nickOpt = ChatNicknameParser.parse(message);
        if (nickOpt.isEmpty()) {
            ModerationHelperClient.clientMessage("Ник не найден в сообщении чата.");
            return false;
        }

        String nick = nickOpt.get();
        ModerationHelperClient.RECENT_PLAYERS.add(nick);

        Path tempScreenshot = null;
        if (!hasNoScreenshotMarker(message)) {
            tempScreenshot = ModerationHelperClient.SCREENSHOTS.captureTemp(nick);
        }

        Path finalTempScreenshot = tempScreenshot;
        client.setScreen(new PunishmentScreen(nick, finalTempScreenshot));
        return true;
    }

    private static boolean hasNoScreenshotMarker(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase(Locale.ROOT);
        for (String marker : NO_SCREENSHOT_MARKERS) {
            if (lower.contains(marker.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
