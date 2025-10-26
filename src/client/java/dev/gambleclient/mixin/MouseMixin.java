package dev.gambleclient.mixin;

import dev.gambleclient.event.events.MouseButtonEvent;
import dev.gambleclient.event.events.MouseScrolledEvent;
import dev.gambleclient.manager.EventManager;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Mouse.class})
public abstract class MouseMixin {
   @Inject(
      method = {"onMouseButton"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
      if (button != -1) {
         MouseButtonEvent event = new MouseButtonEvent(button, window, action);
         EventManager.b(event);
         if (event.isCancelled()) {
            ci.cancel();
         }

      }
   }

   @Inject(
      method = {"onMouseScroll"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      MouseScrolledEvent event = new MouseScrolledEvent(vertical);
      EventManager.b(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }
}
