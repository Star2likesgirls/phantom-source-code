package dev.gambleclient.module.modules.misc;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BindSetting;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.InventoryUtil;
import dev.gambleclient.utils.KeyUtils;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.item.Items;

public final class KeyPearl extends Module {
   private final BindSetting activateKey = new BindSetting(EncryptedString.of("Activate Key"), -1, false);
   private final NumberSetting throwDelay = new NumberSetting(EncryptedString.of("Delay"), (double)0.0F, (double)20.0F, (double)0.0F, (double)1.0F);
   private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true);
   private final NumberSetting switchBackDelay = (new NumberSetting(EncryptedString.of("Switch Delay"), (double)0.0F, (double)20.0F, (double)0.0F, (double)1.0F)).getValue(EncryptedString.of("Delay after throwing pearl before switching back"));
   private boolean isActivated;
   private boolean hasThrown;
   private int currentThrowDelay;
   private int previousSlot;
   private int currentSwitchBackDelay;

   public KeyPearl() {
      super(EncryptedString.of("Key Pearl"), EncryptedString.of("Switches to an ender pearl and throws it when you press a bind"), -1, Category.MISC);
      this.addSettings(new Setting[]{this.activateKey, this.throwDelay, this.switchBack, this.switchBackDelay});
   }

   public void onEnable() {
      this.resetState();
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.mc.currentScreen == null) {
         if (KeyUtils.isKeyPressed(this.activateKey.getValue())) {
            this.isActivated = true;
         }

         if (this.isActivated) {
            if (this.previousSlot == -1) {
               this.previousSlot = this.mc.player.getInventory().selectedSlot;
            }

            InventoryUtil.swap(Items.ENDER_PEARL);
            if (this.currentThrowDelay < this.throwDelay.getIntValue()) {
               ++this.currentThrowDelay;
               return;
            }

            if (!this.hasThrown) {
               ActionResult interactItem = this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
               if (interactItem.isAccepted() && interactItem.shouldSwingHand()) {
                  this.mc.player.swingHand(Hand.MAIN_HAND);
               }

               this.hasThrown = true;
            }

            if (this.switchBack.getValue()) {
               this.handleSwitchBack();
            } else {
               this.resetState();
            }
         }

      }
   }

   private void handleSwitchBack() {
      if (this.currentSwitchBackDelay < this.switchBackDelay.getIntValue()) {
         ++this.currentSwitchBackDelay;
      } else {
         InventoryUtil.swap(this.previousSlot);
         this.resetState();
      }
   }

   private void resetState() {
      this.previousSlot = -1;
      this.currentThrowDelay = 0;
      this.currentSwitchBackDelay = 0;
      this.isActivated = false;
      this.hasThrown = false;
   }
}
