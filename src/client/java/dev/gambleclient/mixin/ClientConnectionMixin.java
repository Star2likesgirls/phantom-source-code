package dev.gambleclient.mixin;

import dev.gambleclient.event.events.PacketReceiveEvent;
import dev.gambleclient.event.events.PacketSendEvent;
import dev.gambleclient.manager.EventManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientConnection.class})
public class ClientConnectionMixin {
   @Inject(
      method = {"handlePacket"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void onPacketReceive(Packet packet, PacketListener listener, CallbackInfo ci) {
      PacketReceiveEvent event = new PacketReceiveEvent(packet);
      EventManager.b(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"send(Lnet/minecraft/network/packet/Packet;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onPacketSend(Packet packet, CallbackInfo ci) {
      PacketSendEvent event = new PacketSendEvent(packet);
      EventManager.b(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }
}
