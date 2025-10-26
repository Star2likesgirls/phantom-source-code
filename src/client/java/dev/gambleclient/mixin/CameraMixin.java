package dev.gambleclient.mixin;

import dev.gambleclient.Gamble;
import dev.gambleclient.module.modules.misc.Freecam;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({Camera.class})
public class CameraMixin {
   @Unique
   private float tickDelta;

   @Inject(
      method = {"update"},
      at = {@At("HEAD")}
   )
   private void onUpdateHead(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
      this.tickDelta = tickDelta;
   }

   @ModifyArgs(
      method = {"update"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"
)
   )
   private void update(Args args) {
      Freecam freecam = (Freecam)Gamble.INSTANCE.MODULE_MANAGER.getModuleByClass(Freecam.class);
      if (freecam.isEnabled()) {
         args.set(0, freecam.getInterpolatedX(this.tickDelta));
         args.set(1, freecam.getInterpolatedY(this.tickDelta));
         args.set(2, freecam.getInterpolatedZ(this.tickDelta));
      }

   }

   @ModifyArgs(
      method = {"update"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"
)
   )
   private void onUpdateSetRotationArgs(Args args) {
      Freecam freecam = (Freecam)Gamble.INSTANCE.MODULE_MANAGER.getModuleByClass(Freecam.class);
      if (freecam.isEnabled()) {
         args.set(0, (float)freecam.getInterpolatedYaw(this.tickDelta));
         args.set(1, (float)freecam.getInterpolatedPitch(this.tickDelta));
      }

   }
}
