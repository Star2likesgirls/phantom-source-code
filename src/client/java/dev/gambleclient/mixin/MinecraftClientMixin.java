package dev.gambleclient.mixin;

import dev.gambleclient.Gamble;
import dev.gambleclient.event.events.AttackEvent;
import dev.gambleclient.event.events.BlockBreakingEvent;
import dev.gambleclient.event.events.PostItemUseEvent;
import dev.gambleclient.event.events.PreItemUseEvent;
import dev.gambleclient.event.events.ResolutionChangedEvent;
import dev.gambleclient.event.events.SetScreenEvent;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.manager.EventManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MinecraftClient.class})
public class MinecraftClientMixin {
   @Shadow
   public @Nullable ClientWorld field_1687;
   @Shadow
   @Final
   private Window field_1704;
   @Shadow
   private int field_1752;

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void onTick(CallbackInfo ci) {
      if (this.field_1687 != null) {
         EventManager.b(new TickEvent());
      }

   }

   @Inject(
      method = {"onResolutionChanged"},
      at = {@At("HEAD")}
   )
   private void onResolutionChanged(CallbackInfo ci) {
      EventManager.b(new ResolutionChangedEvent(this.field_1704));
   }

   @Inject(
      method = {"doItemUse"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void onItemUseReturn(CallbackInfo ci) {
      PostItemUseEvent event = new PostItemUseEvent(this.field_1752);
      EventManager.b(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

      this.field_1752 = event.cooldown;
   }

   @Inject(
      method = {"doItemUse"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onItemUseHead(CallbackInfo ci) {
      PreItemUseEvent event = new PreItemUseEvent(this.field_1752);
      EventManager.b(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

      this.field_1752 = event.cooldown;
   }

   @Inject(
      method = {"doAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onAttack(CallbackInfoReturnable cir) {
      AttackEvent event = new AttackEvent();
      EventManager.b(event);
      if (event.isCancelled()) {
         cir.setReturnValue(false);
      }

   }

   @Inject(
      method = {"handleBlockBreaking"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onBlockBreaking(boolean breaking, CallbackInfo ci) {
      BlockBreakingEvent event = new BlockBreakingEvent();
      EventManager.b(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"setScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSetScreen(Screen screen, CallbackInfo ci) {
      SetScreenEvent event = new SetScreenEvent(screen);
      EventManager.b(event);
      if (event.isCancelled()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"stop"},
      at = {@At("HEAD")}
   )
   private void onClose(CallbackInfo callbackInfo) {
      Gamble.INSTANCE.getConfigManager().shutdown();
   }
}
