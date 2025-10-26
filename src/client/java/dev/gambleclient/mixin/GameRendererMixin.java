package dev.gambleclient.mixin;

import dev.gambleclient.Gamble;
import dev.gambleclient.event.events.Render3DEvent;
import dev.gambleclient.manager.EventManager;
import dev.gambleclient.module.modules.misc.Freecam;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GameRenderer.class})
public abstract class GameRendererMixin {
   @Shadow
   @Final
   private Camera field_18765;

   @Shadow
   protected abstract double getFov(Camera var1, float var2, boolean var3);

   @Shadow
   public abstract Matrix4f getBasicProjectionMatrix(double var1);

   @Inject(
      method = {"renderWorld"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V",
   ordinal = 1
)}
   )
   private void onWorldRender(RenderTickCounter rtc, CallbackInfo ci) {
      EventManager.b(new Render3DEvent(new MatrixStack(), this.getBasicProjectionMatrix(this.getFov(this.field_18765, rtc.getTickDelta(true), true)), rtc.getTickDelta(true)));
   }

   @Inject(
      method = {"shouldRenderBlockOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onShouldRenderBlockOutline(CallbackInfoReturnable cir) {
      if (Gamble.INSTANCE.getModuleManager().getModuleByClass(Freecam.class).isEnabled()) {
         cir.setReturnValue(false);
      }

   }
}
