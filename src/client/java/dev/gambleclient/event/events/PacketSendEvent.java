package dev.gambleclient.event.events;

import dev.gambleclient.event.CancellableEvent;
import net.minecraft.network.packet.Packet;

public class PacketSendEvent extends CancellableEvent {
   public Packet packet;

   public PacketSendEvent(Packet packet) {
      this.packet = packet;
   }
}
