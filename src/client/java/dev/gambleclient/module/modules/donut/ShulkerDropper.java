package dev.gambleclient.module.modules.donut;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.client.realms.gui.RealmsWorldSlotButton;

public final class ShulkerDropper extends Module {
   private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), (double)0.0F, (double)20.0F, (double)1.0F, (double)1.0F);
   private int delayCounter = 0;

   public ShulkerDropper() {
      super(EncryptedString.of("Shulker Dropper"), EncryptedString.of("Goes to shop buys shulkers and drops automatically"), -1, Category.DONUT);
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
         if (this.delayCounter > 0) {
            --this.delayCounter;
         } else {
            ScreenHandler currentScreenHandler = this.mc.player.currentScreenHandler;
            if (!(this.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
               this.mc.getNetworkHandler().sendChatCommand("shop");
               this.delayCounter = 20;
            } else if (((GenericContainerScreenHandler)currentScreenHandler).getRows() == 3) {
               if (currentScreenHandler.getSlot(11).getStack().isOf(Items.END_STONE) && currentScreenHandler.getSlot(11).getStack().getCount() == 1) {
                  this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 11, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                  this.delayCounter = 20;
               } else if (currentScreenHandler.getSlot(17).getStack().isOf(Items.SHULKER_BOX)) {
                  this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 17, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                  this.delayCounter = 20;
               } else {
                  if (currentScreenHandler.getSlot(13).getStack().isOf(Items.SHULKER_BOX)) {
                     this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 23, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                     this.delayCounter = this.delay.getIntValue();
                     this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.DROP_ALL_ITEMS, BlockPos.ORIGIN, Direction.DOWN));
                  }

               }
            }
         }
      }
   }
}
