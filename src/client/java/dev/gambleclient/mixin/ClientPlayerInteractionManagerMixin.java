package dev.gambleclient.mixin;

import dev.gambleclient.event.events.AttackBlockEvent;
import dev.gambleclient.manager.EventManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientPlayerInteractionManager.class})
public class ClientPlayerInteractionManagerMixin {
   @Inject(
      method = {"attackBlock"},
      at = {@At("HEAD")}
   )
   private void onAttackBlock(BlockPos pos, Direction dir, CallbackInfoReturnable cir) {
      EventManager.b(new AttackBlockEvent(pos, dir));
   }
}
