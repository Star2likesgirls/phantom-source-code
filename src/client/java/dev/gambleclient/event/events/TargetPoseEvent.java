package dev.gambleclient.event.events;

import dev.gambleclient.event.CancellableEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class TargetPoseEvent extends CancellableEvent {
   public Entity entity;
   public CallbackInfoReturnable cir;

   public TargetPoseEvent(Entity entity, CallbackInfoReturnable cir) {
      this.entity = entity;
      this.cir = cir;
   }
}
