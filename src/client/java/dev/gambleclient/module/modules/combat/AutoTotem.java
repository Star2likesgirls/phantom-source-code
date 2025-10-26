package dev.gambleclient.module.modules.combat;

import dev.gambleclient.Gamble;
import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.modules.donut.RtpBaseFinder;
import dev.gambleclient.module.modules.donut.TunnelBaseFinder;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public final class AutoTotem extends Module {
   private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), (double)0.0F, (double)5.0F, (double)1.0F, (double)1.0F);
   private int delayCounter;

   public AutoTotem() {
      super(EncryptedString.of("Auto Totem"), EncryptedString.of("Automatically holds totem in your off hand"), -1, Category.COMBAT);
      this.addSettings(new Setting[]{this.delay});
   }

   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.mc.player != null) {
         Module rtpBaseFinder = Gamble.INSTANCE.MODULE_MANAGER.getModuleByClass(RtpBaseFinder.class);
         if (!rtpBaseFinder.isEnabled() || !((RtpBaseFinder)rtpBaseFinder).isRepairingActive()) {
            Module tunnelBaseFinder = Gamble.INSTANCE.MODULE_MANAGER.getModuleByClass(TunnelBaseFinder.class);
            if (!tunnelBaseFinder.isEnabled() || !((TunnelBaseFinder)tunnelBaseFinder).isDigging()) {
               if (this.mc.player.getInventory().getStack(40).getItem() == Items.TOTEM_OF_UNDYING) {
                  this.delayCounter = this.delay.getIntValue();
               } else if (this.delayCounter > 0) {
                  --this.delayCounter;
               } else {
                  int slot = this.findItemSlot(Items.TOTEM_OF_UNDYING);
                  if (slot != -1) {
                     this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, convertSlotIndex(slot), 40, net.minecraft.screen.slot.SlotActionType.SWAP, this.mc.player);
                     this.delayCounter = this.delay.getIntValue();
                  }
               }
            }
         }
      }
   }

   public int findItemSlot(Item item) {
      if (this.mc.player == null) {
         return -1;
      } else {
         for(int i = 0; i < 36; ++i) {
            if (this.mc.player.getInventory().getStack(i).isOf(item)) {
               return i;
            }
         }

         return -1;
      }
   }

   private static int convertSlotIndex(int slotIndex) {
      return slotIndex < 9 ? 36 + slotIndex : slotIndex;
   }
}
