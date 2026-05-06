package ru.wqkcpf.moderationhelper.keybind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import ru.wqkcpf.moderationhelper.ModerationHelperClient;
import ru.wqkcpf.moderationhelper.gui.StatsScreen;

public final class KeybindManager {
    private static KeyBinding statsKey;
    private static KeyBinding stopObsKey;

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of(ModerationHelperClient.MOD_ID, "controls"));

    private KeybindManager() {}

    public static void register() {
        statsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.moderation-helper-gui.stats",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                CATEGORY
        ));

        stopObsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.moderation-helper-gui.stop_obs",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (statsKey.wasPressed()) {
                client.setScreen(new StatsScreen());
            }

            while (stopObsKey.wasPressed()) {
                // Если открыт чат, G не останавливает OBS, чтобы клавиша не мешала набору сообщений.
                if (MinecraftClient.getInstance().currentScreen instanceof ChatScreen) {
                    continue;
                }

                ModerationHelperClient.OBS.stopRecording();
            }
        });
    }
}
