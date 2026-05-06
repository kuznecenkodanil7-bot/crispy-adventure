package ru.wqkcpf.moderationhelper;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wqkcpf.moderationhelper.config.ModConfig;
import ru.wqkcpf.moderationhelper.keybind.KeybindManager;
import ru.wqkcpf.moderationhelper.obs.ObsController;
import ru.wqkcpf.moderationhelper.recent.RecentPlayersManager;
import ru.wqkcpf.moderationhelper.screenshot.ScreenshotManager;
import ru.wqkcpf.moderationhelper.stats.SessionStats;
import ru.wqkcpf.moderationhelper.timer.RecordingTimer;

public class ModerationHelperClient implements ClientModInitializer {
    public static final String MOD_ID = "moderation-helper-gui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ModConfig CONFIG;
    public static SessionStats STATS;
    public static RecentPlayersManager RECENT_PLAYERS;
    public static ScreenshotManager SCREENSHOTS;
    public static ObsController OBS;
    public static RecordingTimer RECORDING_TIMER;

    @Override
    public void onInitializeClient() {
        CONFIG = ModConfig.load();
        STATS = new SessionStats();
        RECENT_PLAYERS = new RecentPlayersManager(CONFIG);
        SCREENSHOTS = new ScreenshotManager(CONFIG);
        OBS = new ObsController(CONFIG);
        RECORDING_TIMER = new RecordingTimer();

        SCREENSHOTS.prepareDirectories();
        SCREENSHOTS.cleanupOldScreenshots();
        RECENT_PLAYERS.load();

        KeybindManager.register();
        registerHudTimer();

        LOGGER.info("Moderation Helper GUI loaded");
    }

    private void registerHudTimer() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.of(MOD_ID, "recording_timer"),
                (DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) -> {
                    if (RECORDING_TIMER == null || !RECORDING_TIMER.isRunning()) return;
                    MinecraftClient client = MinecraftClient.getInstance();
                    String text = "Идёт запись: " + RECORDING_TIMER.getFormattedElapsed();
                    int width = client.getWindow().getScaledWidth();
                    int y = client.getWindow().getScaledHeight() - 64;
                    int textWidth = client.textRenderer.getWidth(text);
                    int x = (width - textWidth) / 2;
                    context.fill(x - 8, y - 5, x + textWidth + 8, y + 14, 0x99000000);
                    context.drawTextWithShadow(client.textRenderer, Text.literal(text), x, y, 0xFFFF5555);
                }
        );
    }

    public static void clientMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§6[ModerationHelper] §f" + message), false);
            }
        });
    }
}
