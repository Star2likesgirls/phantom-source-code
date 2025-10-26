package dev.gambleclient.mixin;

import dev.gambleclient.event.events.SetBlockStateEvent;
import dev.gambleclient.manager.EventManager;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({WorldChunk.class})
public class WorldChunkMixin {
   @Shadow
   @Final
   World field_12858;

   @Inject(
      method = {"setBlockState"},
      at = {@At("TAIL")}
   )
   private void onSetBlockState(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable cir) {
      if (this.field_12858.isClient) {
         EventManager.b(new SetBlockStateEvent(pos, (BlockState)cir.getReturnValue(), state));
      }

   }
}
