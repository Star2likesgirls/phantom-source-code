package dev.gambleclient.mixin;

import dev.gambleclient.event.events.KeyEvent;
import dev.gambleclient.manager.EventManager;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {Keyboard.class}, priority = 1100)
public class KeyboardMixin {
    @Inject(
            method = {"onKey"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/InputUtil;fromKeyCode(II)Lnet/minecraft/client/util/InputUtil$Key;",
                    shift = At.Shift.AFTER
            )}
    )
    private void onPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (key != -1 && action == 1) {
            KeyEvent event = new KeyEvent(key, window, action);
            EventManager.b(event);
        }
    }
}