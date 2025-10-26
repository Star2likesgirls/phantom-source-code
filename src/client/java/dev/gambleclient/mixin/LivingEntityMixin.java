package dev.gambleclient.mixin;

import dev.gambleclient.Gamble;
import dev.gambleclient.module.modules.render.SwingSpeed;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public class LivingEntityMixin {
   @Inject(
      method = {"getHandSwingDuration"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getHandSwingDurationInject(CallbackInfoReturnable cir) {
      if (Gamble.INSTANCE != null && Gamble.mc != null) {
         SwingSpeed swingSpeedModule = (SwingSpeed)Gamble.INSTANCE.getModuleManager().getModuleByClass(SwingSpeed.class);
         if (swingSpeedModule != null && swingSpeedModule.isEnabled()) {
            cir.setReturnValue(swingSpeedModule.getSwingSpeed().getIntValue());
         }
      }

   }
}
