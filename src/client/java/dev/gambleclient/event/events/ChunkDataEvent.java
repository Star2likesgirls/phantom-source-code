package dev.gambleclient.event.events;

import dev.gambleclient.event.CancellableEvent;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;

public class ChunkDataEvent extends CancellableEvent {
   public ChunkDataS2CPacket packet;

   public ChunkDataEvent(ChunkDataS2CPacket packet) {
      this.packet = packet;
   }
}
