package ru.wqkcpf.moderationhelper.chat;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class ChatMessageTracker {
    private static final LinkedList<String> RECENT_LINES = new LinkedList<>();
    private static final int LIMIT = 80;

    private ChatMessageTracker() {}

    public static void remember(String message) {
        if (message == null || message.isBlank()) return;
        synchronized (RECENT_LINES) {
            if (!RECENT_LINES.isEmpty() && RECENT_LINES.getFirst().equals(message)) return;
            RECENT_LINES.addFirst(message);
            while (RECENT_LINES.size() > LIMIT) RECENT_LINES.removeLast();
        }
    }

    public static List<String> snapshot() {
        synchronized (RECENT_LINES) {
            return Collections.unmodifiableList(new ArrayList<>(RECENT_LINES));
        }
    }

    public static String guessMessageByMouseY(double mouseY) {
        List<String> lines = snapshot();
        if (lines.isEmpty()) return "";

        MinecraftClient client = MinecraftClient.getInstance();
        int height = client.getWindow().getScaledHeight();

        // При открытом чате строки идут снизу вверх. Формула приближённая, но хорошо работает
        // для стандартного расположения чата. Если клик вне зоны — берём самое свежее сообщение.
        int chatBottom = height - 40;
        int lineHeight = 9;
        int index = (chatBottom - (int) mouseY) / lineHeight;
        if (index < 0 || index >= lines.size()) index = 0;
        return lines.get(index);
    }
}
