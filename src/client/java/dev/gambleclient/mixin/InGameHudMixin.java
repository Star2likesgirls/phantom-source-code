package dev.gambleclient.mixin;

import dev.gambleclient.event.events.Render2DEvent;
import dev.gambleclient.manager.EventManager;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InGameHud.class})
public class InGameHudMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void onRenderHud(DrawContext ctx, RenderTickCounter rtc, CallbackInfo ci) {
      EventManager.b(new Render2DEvent(ctx, rtc.getTickDelta(true)));
   }
}
