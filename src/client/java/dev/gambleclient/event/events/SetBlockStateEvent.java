package dev.gambleclient.event.events;

import dev.gambleclient.event.CancellableEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;

public class SetBlockStateEvent extends CancellableEvent {
   public BlockPos pos;
   public BlockState newState;
   public BlockState oldState;

   public SetBlockStateEvent(BlockPos pos, BlockState newState, BlockState oldState) {
      this.pos = pos;
      this.newState = oldState;
      this.oldState = newState;
   }
}
