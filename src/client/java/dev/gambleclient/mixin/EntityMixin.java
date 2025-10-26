package dev.gambleclient.mixin;

import dev.gambleclient.Gamble;
import dev.gambleclient.event.events.TargetMarginEvent;
import dev.gambleclient.event.events.TargetPoseEvent;
import dev.gambleclient.manager.EventManager;
import dev.gambleclient.module.modules.misc.Freecam;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public class EntityMixin {
   @Inject(
      method = {"getTargetingMargin"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSendMovementPackets(CallbackInfoReturnable cir) {
      EventManager.b(new TargetMarginEvent((Entity)Entity.class.cast(this), cir));
   }

   @Inject(
      method = {"getPose"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetPose(CallbackInfoReturnable cir) {
      EventManager.b(new TargetPoseEvent((Entity)Entity.class.cast(this), cir));
   }

   @Inject(
      method = {"changeLookDirection"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void updateChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
      if (Entity.class.cast(this) == Gamble.mc.player) {
         Freecam freecam = (Freecam)Gamble.INSTANCE.MODULE_MANAGER.getModuleByClass(Freecam.class);
         if (freecam.isEnabled()) {
            freecam.updateRotation(cursorDeltaX * 0.15, cursorDeltaY * 0.15);
            ci.cancel();
         }

      }
   }
}
