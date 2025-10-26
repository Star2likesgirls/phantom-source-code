package dev.gambleclient.event.events;

import dev.gambleclient.event.CancellableEvent;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class TargetMarginEvent extends CancellableEvent {
   public Entity entity;
   public CallbackInfoReturnable cir;

   public TargetMarginEvent(Entity entity, CallbackInfoReturnable cir) {
      this.entity = entity;
      this.cir = cir;
   }
}
