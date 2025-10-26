package dev.gambleclient.mixin;

import dev.gambleclient.event.events.ChunkMarkClosedEvent;
import dev.gambleclient.manager.EventManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChunkOcclusionDataBuilder.class})
public abstract class ChunkOcclusionDataBuilderMixin {
   @Inject(
      method = {"markClosed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onMarkClosed(BlockPos pos, CallbackInfo ci) {
      ChunkMarkClosedEvent event = new ChunkMarkClosedEvent();
      EventManager.b(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }
}
