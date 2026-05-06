package ru.wqkcpf.moderationhelper.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class ScreenUtil {
    private ScreenUtil() {}

    public static void darkPanel(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, 0xCC10131A);
        context.fill(x, y, x + w, y + 1, 0xFF5865F2);
        context.fill(x, y + h - 1, x + w, y + h, 0x66000000);
    }

    public static void title(DrawContext context, MinecraftClient client, String text, int x, int y) {
        context.drawTextWithShadow(client.textRenderer, Text.literal(text), x, y, 0xFFFFFFFF);
    }

    public static void muted(DrawContext context, MinecraftClient client, String text, int x, int y) {
        context.drawTextWithShadow(client.textRenderer, Text.literal(text), x, y, 0xFFB8C0CC);
    }

    public static void centerTitle(DrawContext context, MinecraftClient client, String text, int x, int y, int w) {
        int textW = client.textRenderer.getWidth(Text.literal(text));
        context.drawTextWithShadow(client.textRenderer, Text.literal(text), x + (w - textW) / 2, y, 0xFFFFFFFF);
    }

    public static void centerMuted(DrawContext context, MinecraftClient client, String text, int x, int y, int w) {
        int textW = client.textRenderer.getWidth(Text.literal(text));
        context.drawTextWithShadow(client.textRenderer, Text.literal(text), x + (w - textW) / 2, y, 0xFFB8C0CC);
    }

    public static void statLine(DrawContext context, MinecraftClient client, String key, int value, int x, int y) {
        context.drawTextWithShadow(client.textRenderer, Text.literal(key + ": §f" + value), x, y, 0xFFB8C0CC);
    }
}
