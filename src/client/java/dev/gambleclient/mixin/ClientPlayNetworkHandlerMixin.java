package dev.gambleclient.mixin;

import dev.gambleclient.event.events.ChunkDataEvent;
import dev.gambleclient.event.events.EntitySpawnEvent;
import dev.gambleclient.manager.EventManager;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayNetworkHandler.class})
public abstract class ClientPlayNetworkHandlerMixin {
   @Inject(
      method = {"onChunkData"},
      at = {@At("TAIL")}
   )
   private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
      EventManager.b(new ChunkDataEvent(packet));
   }

   @Inject(
      method = {"onEntitySpawn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo ci) {
      EntitySpawnEvent event = new EntitySpawnEvent(packet);
      EventManager.b(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }
}
