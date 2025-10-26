package dev.gambleclient.module.modules.combat;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.BlockUtil;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.InventoryUtil;
import dev.gambleclient.utils.KeyUtils;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.block.Blocks;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.component.DataComponentTypes;
import org.lwjgl.glfw.GLFW;

public final class AnchorMacro extends Module {
   private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), (double)0.0F, (double)20.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting glowstoneDelay = new NumberSetting(EncryptedString.of("Glowstone Delay"), (double)0.0F, (double)20.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting explodeDelay = new NumberSetting(EncryptedString.of("Explode Delay"), (double)0.0F, (double)20.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting totemSlot = new NumberSetting(EncryptedString.of("Totem Slot"), (double)1.0F, (double)9.0F, (double)1.0F, (double)1.0F);
   private int keybind = 0;
   private int glowstoneDelayCounter = 0;
   private int explodeDelayCounter = 0;

   public AnchorMacro() {
      super(EncryptedString.of("Anchor Macro"), EncryptedString.of("Automatically blows up respawn anchors for you"), -1, Category.COMBAT);
      this.addSettings(new Setting[]{this.switchDelay, this.glowstoneDelay, this.explodeDelay, this.totemSlot});
   }

   public void onEnable() {
      this.resetCounters();
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onTick(TickEvent tickEvent) {
      if (this.mc.currentScreen == null) {
         if (!this.isShieldOrFoodActive()) {
            if (KeyUtils.isKeyPressed(1)) {
               this.handleAnchorInteraction();
            }

         }
      }
   }

   private boolean isShieldOrFoodActive() {
      boolean isFood = this.mc.player.getMainHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD) || this.mc.player.getOffHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD);
      boolean isShield = this.mc.player.getMainHandStack().getItem() instanceof ShieldItem || this.mc.player.getOffHandStack().getItem() instanceof ShieldItem;
      boolean isRightClickPressed = GLFW.glfwGetMouseButton(this.mc.getWindow().getHandle(), 1) == 1;
      return (isFood || isShield) && isRightClickPressed;
   }

   private void handleAnchorInteraction() {
      HitResult var2 = this.mc.crosshairTarget;
      if (var2 instanceof BlockHitResult blockHitResult) {
         if (BlockUtil.isBlockAtPosition(blockHitResult.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
            this.mc.options.useKey.setPressed(false);
            if (BlockUtil.isRespawnAnchorUncharged(blockHitResult.getBlockPos())) {
               this.placeGlowstone(blockHitResult);
            } else if (BlockUtil.isRespawnAnchorCharged(blockHitResult.getBlockPos())) {
               this.explodeAnchor(blockHitResult);
            }

         }
      }
   }

   private void placeGlowstone(BlockHitResult blockHitResult) {
      if (!this.mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
         if (this.keybind < this.switchDelay.getIntValue()) {
            ++this.keybind;
            return;
         }

         this.keybind = 0;
         InventoryUtil.swap(Items.GLOWSTONE);
      }

      if (this.mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
         if (this.glowstoneDelayCounter < this.glowstoneDelay.getIntValue()) {
            ++this.glowstoneDelayCounter;
            return;
         }

         this.glowstoneDelayCounter = 0;
         BlockUtil.interactWithBlock(blockHitResult, true);
      }

   }

   private void explodeAnchor(BlockHitResult blockHitResult) {
      int selectedSlot = this.totemSlot.getIntValue() - 1;
      if (this.mc.player.getInventory().selectedSlot != selectedSlot) {
         if (this.keybind < this.switchDelay.getIntValue()) {
            ++this.keybind;
            return;
         }

         this.keybind = 0;
         this.mc.player.getInventory().selectedSlot = selectedSlot;
      }

      if (this.mc.player.getInventory().selectedSlot == selectedSlot) {
         if (this.explodeDelayCounter < this.explodeDelay.getIntValue()) {
            ++this.explodeDelayCounter;
            return;
         }

         this.explodeDelayCounter = 0;
         BlockUtil.interactWithBlock(blockHitResult, true);
      }

   }

   private void resetCounters() {
      this.keybind = 0;
      this.glowstoneDelayCounter = 0;
      this.explodeDelayCounter = 0;
   }
}
