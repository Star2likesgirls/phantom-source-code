package dev.gambleclient.mixin;

import dev.gambleclient.gui.ClickGUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Screen.class})
public class ScreenMixin {
   @Shadow
   protected @NotNull MinecraftClient field_22787;

   @Inject(
      method = {"renderBackground"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void dontRenderBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (this.field_22787.currentScreen instanceof ClickGUI) {
         ci.cancel();
      }

   }
}
