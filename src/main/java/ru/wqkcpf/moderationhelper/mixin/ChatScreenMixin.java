package ru.wqkcpf.moderationhelper.mixin;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wqkcpf.moderationhelper.chat.ClickedChatHandler;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mhg$onMiddleClick(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click != null && click.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            boolean handled = ClickedChatHandler.handleMiddleClick(click.x(), click.y());
            if (handled) {
                cir.setReturnValue(true);
            }
        }
    }
}
