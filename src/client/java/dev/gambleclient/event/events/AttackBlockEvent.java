package dev.gambleclient.event.events;

import dev.gambleclient.event.CancellableEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AttackBlockEvent extends CancellableEvent {
   public BlockPos pos;
   public Direction direction;

   public AttackBlockEvent(BlockPos pos, Direction direction) {
      this.pos = pos;
      this.direction = direction;
   }
}
