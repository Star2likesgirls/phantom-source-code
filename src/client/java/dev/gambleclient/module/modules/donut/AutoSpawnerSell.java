package dev.gambleclient.module.modules.donut;
import net.minecraft.client.util.InputUtil;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.InventoryUtil;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.client.texture.Scaling;

public final class AutoSpawnerSell extends Module {
   private final NumberSetting dropDelay = (new NumberSetting(EncryptedString.of("Drop Delay"), (double)0.0F, (double)120.0F, (double)30.0F, (double)1.0F)).getValue(EncryptedString.of("How often it should start dropping bones in minutes"));
   private final NumberSetting pageAmount = (new NumberSetting(EncryptedString.of("Page Amount"), (double)1.0F, (double)10.0F, (double)2.0F, (double)1.0F)).getValue(EncryptedString.of("How many pages should it drop before selling"));
   private final NumberSetting pageSwitchDelay = (new NumberSetting(EncryptedString.of("Page Switch Delay"), (double)0.0F, (double)720.0F, (double)4.0F, (double)1.0F)).getValue(EncryptedString.of("How often it should switch pages in seconds"));
   private final NumberSetting delay = (new NumberSetting(EncryptedString.of("delay"), (double)0.0F, (double)20.0F, (double)1.0F, (double)1.0F)).getValue(EncryptedString.of("What should be delay in ticks"));
   private int delayCounter;
   private int pageCounter;
   private boolean isProcessing;
   private boolean isSelling;
   private boolean isPageSwitching;

   public AutoSpawnerSell() {
      super(EncryptedString.of("Auto Spawner Sell"), EncryptedString.of("Automatically drops bones from spawner and sells them"), -1, Category.DONUT);
      this.addSettings(new Setting[]{this.dropDelay, this.pageAmount, this.pageSwitchDelay, this.delay});
   }

   public void onEnable() {
      super.onEnable();
      this.delayCounter = 20;
      this.isProcessing = false;
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.delayCounter > 0) {
         --this.delayCounter;
      } else if (this.mc.player != null) {
         if (this.pageCounter >= this.pageAmount.getIntValue()) {
            this.isSelling = true;
            this.pageCounter = 0;
            this.delayCounter = 40;
         } else {
            if (!this.isSelling) {
               ScreenHandler fishHook = this.mc.player.currentScreenHandler;
               if (!(this.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
                  KeyBinding.onKeyPressed(InputUtil.Type.MOUSE.createFromCode(1));
                  this.delayCounter = 20;
                  return;
               }

               if (fishHook.getSlot(15).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
                  this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 15, 1, net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, this.mc.player);
                  this.delayCounter = 10;
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

               boolean b2 = true;

               for(int k = 0; k < 45; ++k) {
                  if (!this.mc.player.currentScreenHandler.getSlot(k).getStack().isOf(Items.BONE)) {
                     b2 = false;
                     break;
                  }
               }

               if (b2) {
                  this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 52, 1, net.minecraft.screen.slot.SlotActionType.THROW, this.mc.player);
                  this.isProcessing = true;
                  this.delayCounter = this.pageSwitchDelay.getIntValue() * 20;
                  ++this.pageCounter;
               } else if (this.isProcessing) {
                  this.isProcessing = false;
                  this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 50, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                  this.delayCounter = 20;
               } else {
                  this.isProcessing = false;
                  if (this.pageCounter != 0) {
                     this.pageCounter = 0;
                     this.isSelling = true;
                     this.delayCounter = 40;
                     return;
                  }

                  this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 45, 1, net.minecraft.screen.slot.SlotActionType.THROW, this.mc.player);
                  this.delayCounter = 1200 * this.dropDelay.getIntValue();
               }
            } else {
               ScreenHandler currentScreenHandler = this.mc.player.currentScreenHandler;
               if (!(this.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
                  this.mc.getNetworkHandler().sendChatCommand("order " + this.getOrderCommand());
                  this.delayCounter = 20;
                  return;
               }

               if (((GenericContainerScreenHandler)currentScreenHandler).getRows() != 6) {
                  if (((GenericContainerScreenHandler)currentScreenHandler).getRows() == 4) {
                     int emptySlotCount = InventoryUtil.getSlot(Items.AIR);
                     if (emptySlotCount <= 0) {
                        this.mc.player.closeHandledScreen();
                        this.delayCounter = 10;
                        return;
                     }

                     if (this.isPageSwitching && emptySlotCount == 36) {
                        this.isPageSwitching = false;
                        this.mc.player.closeHandledScreen();
                        return;
                     }

                     Item targetItem = this.getInventoryItem();

                     while(true) {
                        int slotIndex = 36;
                        Item item = ((ItemStack)this.mc.player.currentScreenHandler.getStacks().get(36)).getItem();
                        if (item != Items.AIR && item == targetItem) {
                           this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 36, 1, net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, this.mc.player);
                           this.delayCounter = this.delay.getIntValue();
                           if (this.delay.getIntValue() != 0) {
                              break;
                           }
                        }
                     }
                  } else if (((GenericContainerScreenHandler)currentScreenHandler).getRows() == 3) {
                     this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 15, 1, net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, this.mc.player);
                     this.isPageSwitching = true;
                     this.delayCounter = 10;
                  }
               } else {
                  ItemStack stack = currentScreenHandler.getSlot(47).getStack();
                  if (stack.isOf(Items.AIR)) {
                     this.delayCounter = 2;
                     this.mc.player.closeHandledScreen();
                     return;
                  }

                  for(Object next : stack.getTooltip(Item.TooltipContext.create(this.mc.world), this.mc.player, net.minecraft.item.tooltip.TooltipType.BASIC)) {
                     String string = next.toString();
                     if (string.contains("Most Money Per Item") && (((Text)next).getStyle().toString().contains("white") || string.contains("white"))) {
                        this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 47, 1, net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, this.mc.player);
                        this.delayCounter = 5;
                        return;
                     }
                  }

                  for(int i = 0; i < 44; ++i) {
                     if (currentScreenHandler.getSlot(i).getStack().isOf(this.getInventoryItem())) {
                        this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, i, 1, net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, this.mc.player);
                        this.delayCounter = 10;
                        return;
                     }
                  }

                  this.delayCounter = 40;
                  this.mc.player.closeHandledScreen();
               }
            }

         }
      }
   }

   private Item getInventoryItem() {
      for(int i = 0; i < 35; ++i) {
         ItemStack stack = this.mc.player.getInventory().getStack(i);
         if (!stack.isOf(Items.AIR)) {
            return stack.getItem();
         }
      }

      return Items.AIR;
   }

   private String getOrderCommand() {
      Item j = this.getInventoryItem();
      return j.equals(Items.BONE) ? "Bones" : j.getName().getString();
   }
}
