package ru.wqkcpf.moderationhelper.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import ru.wqkcpf.moderationhelper.ModerationHelperClient;

public class StatsScreen extends Screen {
    public StatsScreen() {
        super(Text.literal("Moderation Helper Stats"));
    }

    @Override
    protected void init() {
        int x = width / 2 - 200;
        int y = height / 2 - 15;
        int row = 0;
        for (String nick : ModerationHelperClient.RECENT_PLAYERS.getPlayers()) {
            if (row >= 12) break;
            int bx = x + (row % 2) * 160;
            int by = y + (row / 2) * 24;
            addDrawableChild(ButtonWidget.builder(Text.literal(nick), button -> {
                MinecraftClient.getInstance().keyboard.setClipboard(nick);
                client.setScreen(new PunishmentScreen(nick, null));
            }).dimensions(bx, by, 150, 20).build());
            row++;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), button -> client.setScreen(null))
                .dimensions(width / 2 - 60, height - 42, 120, 22).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        int panelW = 500;
        int panelH = 280;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        ScreenUtil.darkPanel(context, x, y, panelW, panelH);

        ScreenUtil.title(context, client, "Панель Moderation Helper", x + 20, y + 16);
        ScreenUtil.muted(context, client, "H открывает только эту панель: без ника, без скрина, без поиска в чате.", x + 20, y + 36);

        int statsX = x + 20;
        int statsY = y + 62;
        ScreenUtil.title(context, client, "Статистика сессии", statsX, statsY);
        ScreenUtil.statLine(context, client, "warn", ModerationHelperClient.STATS.get("warn"), statsX, statsY + 20);
        ScreenUtil.statLine(context, client, "mute", ModerationHelperClient.STATS.get("mute"), statsX, statsY + 36);
        ScreenUtil.statLine(context, client, "ban", ModerationHelperClient.STATS.get("ban"), statsX, statsY + 52);
        ScreenUtil.statLine(context, client, "ipban", ModerationHelperClient.STATS.get("ipban"), statsX, statsY + 68);

        ScreenUtil.title(context, client, "Недавние игроки", x + 20, y + 125);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
