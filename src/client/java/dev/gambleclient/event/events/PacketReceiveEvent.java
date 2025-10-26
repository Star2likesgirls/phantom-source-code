package dev.gambleclient.event.events;

import dev.gambleclient.event.CancellableEvent;
import net.minecraft.network.packet.Packet;

public class PacketReceiveEvent extends CancellableEvent {
   public Packet packet;

   public PacketReceiveEvent(Packet packet) {
      this.packet = packet;
   }
}
