package ru.wqkcpf.moderationhelper.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import ru.wqkcpf.moderationhelper.ModerationHelperClient;

import java.nio.file.Path;

public class PunishmentScreen extends Screen {
    private final String nick;
    private final Path tempScreenshot;

    public PunishmentScreen(String nick, Path tempScreenshot) {
        super(Text.literal("Moderation Helper GUI"));
        this.nick = nick;
        this.tempScreenshot = tempScreenshot;
    }

    @Override
    protected void init() {
        int buttonW = 420;
        int buttonH = 24;
        int centerX = width / 2;
        int panelH = Math.min(height - 40, 430);
        int startY = Math.max(20, (height - panelH) / 2);

        addDrawableChild(ButtonWidget.builder(Text.literal("1. Предупреждение /warn 2.1"), button ->
                        client.setScreen(new ReasonScreen(nick, "warn", tempScreenshot)))
                .dimensions(centerX - buttonW / 2, startY + 54, buttonW, buttonH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("2. Мут"), button ->
                        client.setScreen(new ReasonScreen(nick, "mute", tempScreenshot)))
                .dimensions(centerX - buttonW / 2, startY + 84, buttonW, buttonH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("3. Бан"), button ->
                        client.setScreen(new ReasonScreen(nick, "ban", tempScreenshot)))
                .dimensions(centerX - buttonW / 2, startY + 114, buttonW, buttonH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("4. Бан по IP"), button ->
                        client.setScreen(new ReasonScreen(nick, "ipban", tempScreenshot)))
                .dimensions(centerX - buttonW / 2, startY + 144, buttonW, buttonH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("5. Вызвать на проверку /check"), button -> callCheck())
                .dimensions(centerX - buttonW / 2, startY + 174, buttonW, buttonH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("6. Снять с проверки"), button -> uncheck())
                .dimensions(centerX - 115, startY + 204, 230, buttonH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), button -> client.setScreen(null))
                .dimensions(centerX - 90, startY + 258, 180, buttonH).build());
    }

    private void callCheck() {
        ModerationHelperClient.RECENT_PLAYERS.add(nick);
        sendCommand(ModerationHelperClient.CONFIG.checkCommandTemplate.replace("{nick}", nick));
        sendCommand(ModerationHelperClient.CONFIG.checkTellTemplate.replace("{nick}", nick));
        ModerationHelperClient.OBS.startRecording();
        ModerationHelperClient.clientMessage("Игрок " + nick + " вызван на проверку. Запись OBS запущена, если OBS доступен.");
        client.setScreen(null);
    }

    private void uncheck() {
        sendCommand("/uncheck " + nick);
        ModerationHelperClient.clientMessage("Отправлена команда снятия с проверки для " + nick + ".");
        client.setScreen(null);
    }

    private void sendCommand(String command) {
        if (client == null || client.getNetworkHandler() == null || command == null || command.isBlank()) return;
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        client.getNetworkHandler().sendChatCommand(normalized.trim());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);

        int panelW = 460;
        int panelH = Math.min(height - 40, 430);
        int x = (width - panelW) / 2;
        int y = Math.max(20, (height - panelH) / 2);
        ScreenUtil.darkPanel(context, x, y, panelW, panelH);

        ScreenUtil.centerTitle(context, client, "Кого хочешь наказать: §f" + nick, x, y + 16, panelW);
        ScreenUtil.centerMuted(context, client, "Выберите действие", x, y + 40, panelW);
        ScreenUtil.muted(context, client, "Скрин: " + (tempScreenshot == null ? "не создан" : "temp/" + tempScreenshot.getFileName()), x + 20, y + 302);

        int statsX = x + 20;
        int statsY = y + 322;
        ScreenUtil.muted(context, client,
                "Сессия: warn " + ModerationHelperClient.STATS.get("warn")
                        + " | mute " + ModerationHelperClient.STATS.get("mute")
                        + " | ban " + ModerationHelperClient.STATS.get("ban")
                        + " | ipban " + ModerationHelperClient.STATS.get("ipban"),
                statsX, statsY);

        renderRecentPlayers(context, x + 20, y + 348, panelW - 40);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void renderRecentPlayers(DrawContext context, int x, int y, int w) {
        ScreenUtil.muted(context, client, "Недавние игроки: клик по нику копирует его", x, y);
        int row = 0;
        for (String recent : ModerationHelperClient.RECENT_PLAYERS.getPlayers()) {
            if (row >= 6) break;
            int bx = x + (row % 3) * 138;
            int by = y + 14 + (row / 3) * 22;
            context.drawTextWithShadow(client.textRenderer, Text.literal("§f" + recent), bx, by, 0xFFFFFFFF);
            row++;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelW = 460;
        int panelH = Math.min(height - 40, 430);
        int x = (width - panelW) / 2 + 20;
        int y = Math.max(20, (height - panelH) / 2) + 348;
        int row = 0;
        for (String recent : ModerationHelperClient.RECENT_PLAYERS.getPlayers()) {
            if (row >= 6) break;
            int bx = x + (row % 3) * 138;
            int by = y + 14 + (row / 3) * 22;
            int bw = 128;
            int bh = 14;
            if (mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh) {
                MinecraftClient.getInstance().keyboard.setClipboard(recent);
                client.setScreen(new PunishmentScreen(recent, null));
                return true;
            }
            row++;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
