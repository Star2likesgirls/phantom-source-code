package dev.gambleclient.module.modules.combat;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.mixin.HandledScreenMixin;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.Items;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

public final class HoverTotem extends Module {
   private final NumberSetting tickDelay = (new NumberSetting(EncryptedString.of("Tick Delay"), (double)0.0F, (double)20.0F, (double)0.0F, (double)1.0F)).getValue(EncryptedString.of("Ticks to wait between operations"));
   private final BooleanSetting hotbarTotem = (new BooleanSetting(EncryptedString.of("Hotbar Totem"), true)).setDescription(EncryptedString.of("Also places a totem in your preferred hotbar slot"));
   private final NumberSetting hotbarSlot = (new NumberSetting(EncryptedString.of("Hotbar Slot"), (double)1.0F, (double)9.0F, (double)1.0F, (double)1.0F)).getValue(EncryptedString.of("Your preferred hotbar slot for totem (1-9)"));
   private final BooleanSetting autoSwitchToTotem = (new BooleanSetting(EncryptedString.of("Auto Switch To Totem"), false)).setDescription(EncryptedString.of("Automatically switches to totem slot when inventory is opened"));
   private int remainingDelay;

   public HoverTotem() {
      super(EncryptedString.of("Hover Totem"), EncryptedString.of("Equips a totem in offhand and optionally hotbar when hovering over one in inventory"), -1, Category.COMBAT);
      this.addSettings(new Setting[]{this.tickDelay, this.hotbarTotem, this.hotbarSlot, this.autoSwitchToTotem});
   }

   public void onEnable() {
      this.resetDelay();
      super.onEnable();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.mc.player != null) {
         Screen currentScreen = this.mc.currentScreen;
         if (!(this.mc.currentScreen instanceof InventoryScreen)) {
            this.resetDelay();
         } else {
            Slot focusedSlot = ((HandledScreenMixin)currentScreen).getFocusedSlot();
            if (focusedSlot != null && focusedSlot.getIndex() <= 35) {
               if (this.autoSwitchToTotem.getValue()) {
                  this.mc.player.getInventory().selectedSlot = this.hotbarSlot.getIntValue() - 1;
               }

               if (focusedSlot.getStack().getItem() == Items.TOTEM_OF_UNDYING) {
                  if (this.remainingDelay > 0) {
                     --this.remainingDelay;
                  } else {
                     int index = focusedSlot.getIndex();
                     int syncId = ((PlayerScreenHandler)((InventoryScreen)currentScreen).getScreenHandler()).syncId;
                     if (!this.mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                        this.equipOffhandTotem(syncId, index);
                     } else {
                        if (this.hotbarTotem.getValue()) {
                           int n = this.hotbarSlot.getIntValue() - 1;
                           if (!this.mc.player.getInventory().getStack(n).isOf(Items.TOTEM_OF_UNDYING)) {
                              this.equipHotbarTotem(syncId, index, n);
                           }
                        }

                     }
                  }
               }
            }
         }
      }
   }

   private void equipOffhandTotem(int n, int n2) {
      this.mc.interactionManager.clickSlot(n, n2, 40, net.minecraft.screen.slot.SlotActionType.SWAP, this.mc.player);
      this.resetDelay();
   }

   private void equipHotbarTotem(int n, int n2, int n3) {
      this.mc.interactionManager.clickSlot(n, n2, n3, net.minecraft.screen.slot.SlotActionType.SWAP, this.mc.player);
      this.resetDelay();
   }

   private void resetDelay() {
      this.remainingDelay = this.tickDelay.getIntValue();
   }
}
