package dev.gambleclient.module.modules.donut;
import net.minecraft.client.util.InputUtil;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.ModeSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Items;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.Scaling;

public final class BoneDropper extends Module {
   private final ModeSetting dropMode;
   private final NumberSetting dropDelay;
   private final NumberSetting pageSwitchDelay;
   private int delayCounter;
   private boolean isPageSwitching;

   public BoneDropper() {
      super(EncryptedString.of("Bone Dropper"), EncryptedString.of("Automatically drops bones from spawner"), -1, Category.DONUT);
      this.dropMode = new ModeSetting(EncryptedString.of("Mode"), BoneDropper.Mode.SPAWNER, Mode.class);
      this.dropDelay = (new NumberSetting(EncryptedString.of("Drop Delay"), (double)0.0F, (double)120.0F, (double)30.0F, (double)1.0F)).getValue(EncryptedString.of("How often it should start dropping bones in minutes"));
      this.pageSwitchDelay = (new NumberSetting(EncryptedString.of("Page Switch Delay"), (double)0.0F, (double)720.0F, (double)4.0F, (double)1.0F)).getValue(EncryptedString.of("How often it should switch pages in seconds"));
      this.addSettings(new Setting[]{this.dropMode, this.dropDelay, this.pageSwitchDelay});
   }

   public void onEnable() {
      super.onEnable();
      this.delayCounter = 20;
      this.isPageSwitching = false;
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.delayCounter > 0) {
         --this.delayCounter;
      } else if (this.mc.player != null) {
         if (this.dropMode.isMode(BoneDropper.Mode.SPAWNER)) {
            if (!(this.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
               KeyBinding.onKeyPressed(InputUtil.Type.MOUSE.createFromCode(1));
               this.delayCounter = 20;
               return;
            }

            if (this.mc.player.currentScreenHandler.getSlot(13).getStack().isOf(Items.SKELETON_SKULL)) {
               this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 11, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
               this.delayCounter = 20;
               return;
            }

            if (!this.mc.player.currentScreenHandler.getSlot(53).getStack().isOf(Items.GOLD_INGOT)) {
               this.mc.player.closeHandledScreen();
               this.delayCounter = 20;
               return;
            }

            if (this.mc.player.currentScreenHandler.getSlot(48).getStack().isOf(Items.ARROW)) {
               this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 48, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
               this.delayCounter = 20;
               return;
            }

            boolean allSlotsAreBones = true;

            for(int i = 0; i < 45; ++i) {
               if (!this.mc.player.currentScreenHandler.getSlot(i).getStack().isOf(Items.BONE)) {
                  allSlotsAreBones = false;
                  break;
               }
            }

            if (allSlotsAreBones) {
               this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 52, 1, net.minecraft.screen.slot.SlotActionType.THROW, this.mc.player);
               this.isPageSwitching = true;
               this.delayCounter = this.pageSwitchDelay.getIntValue() * 20;
            } else if (this.isPageSwitching) {
               this.isPageSwitching = false;
               this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 50, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
               this.delayCounter = 20;
            } else {
               this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 45, 1, net.minecraft.screen.slot.SlotActionType.THROW, this.mc.player);
               this.isPageSwitching = false;
               this.delayCounter = 1200 * this.dropDelay.getIntValue();
            }
         } else {
            ScreenHandler currentScreenHandler = this.mc.player.currentScreenHandler;
            if (!(this.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
               this.mc.getNetworkHandler().sendChatCommand("order");
               this.delayCounter = 20;
               return;
            }

            if (((GenericContainerScreenHandler)currentScreenHandler).getRows() == 6) {
               if (currentScreenHandler.getSlot(49).getStack().isOf(Items.MAP)) {
                  this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 51, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                  this.delayCounter = 20;
                  return;
               }

               for(int slotIndex = 0; slotIndex < 45; ++slotIndex) {
                  if (currentScreenHandler.getSlot(slotIndex).getStack().isOf(Items.BONE)) {
                     this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, slotIndex, 1, net.minecraft.screen.slot.SlotActionType.THROW, this.mc.player);
                     this.delayCounter = this.dropDelay.getIntValue();
                     return;
                  }
               }

               int targetSlot;
               if (this.isPageSwitching) {
                  targetSlot = 45;
               } else {
                  targetSlot = 53;
               }

               this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, targetSlot, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
               this.isPageSwitching = !this.isPageSwitching;
               this.delayCounter = this.pageSwitchDelay.getIntValue();
            } else if (((GenericContainerScreenHandler)currentScreenHandler).getRows() == 3) {
               if (this.mc.currentScreen == null) {
                  return;
               }

               if (this.mc.currentScreen.getTitle().getString().contains("Your Orders")) {
                  for(int slotIndex = 0; slotIndex < 26; ++slotIndex) {
                     if (currentScreenHandler.getSlot(slotIndex).getStack().isOf(Items.BONE)) {
                        this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, slotIndex, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                        this.delayCounter = 20;
                        return;
                     }
                  }

                  this.delayCounter = 200;
                  return;
               }

               if (this.mc.currentScreen.getTitle().getString().contains("Edit Order")) {
                  if (currentScreenHandler.getSlot(13).getStack().isOf(Items.CHEST)) {
                     this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 13, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                     this.delayCounter = 20;
                     return;
                  }

                  if (currentScreenHandler.getSlot(15).getStack().isOf(Items.CHEST)) {
                     this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 15, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                     this.delayCounter = 20;
                     return;
                  }
               }

               this.delayCounter = 200;
            }
         }

      }
   }

   static enum Mode {
      SPAWNER("Spawner", 0),
      ORDER("Order", 1);

      private Mode(final String name, final int ordinal) {
      }

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{SPAWNER, ORDER};
      }
   }
}
