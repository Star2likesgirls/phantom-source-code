package dev.gambleclient.module.modules.combat;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.FakeInvScreen;
import java.util.function.Predicate;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

public final class AutoInventoryTotem extends Module {
   private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), (double)0.0F, (double)20.0F, (double)0.0F, (double)1.0F);
   private final BooleanSetting hotbar = (new BooleanSetting(EncryptedString.of("Hotbar"), true)).setDescription(EncryptedString.of("Puts a totem in your hotbar as well, if enabled (Setting below will work if this is enabled)"));
   private final NumberSetting totemSlot = (new NumberSetting(EncryptedString.of("Totem Slot"), (double)1.0F, (double)9.0F, (double)1.0F, (double)1.0F)).getValue(EncryptedString.of("Your preferred totem slot"));
   private final BooleanSetting autoSwitch = (new BooleanSetting(EncryptedString.of("Auto Switch"), false)).setDescription(EncryptedString.of("Switches to totem slot when going inside the inventory"));
   private final BooleanSetting forceTotem = (new BooleanSetting(EncryptedString.of("Force Totem"), false)).setDescription(EncryptedString.of("Puts the totem in the slot, regardless if its space is taken up by something else"));
   private final BooleanSetting autoOpen = (new BooleanSetting(EncryptedString.of("Auto Open"), false)).setDescription(EncryptedString.of("Automatically opens and closes the inventory for you"));
   private final NumberSetting stayOpenDuration = new NumberSetting(EncryptedString.of("Stay Open For"), (double)0.0F, (double)20.0F, (double)0.0F, (double)1.0F);
   int delayCounter = -1;
   int stayOpenCounter = -1;

   public AutoInventoryTotem() {
      super(EncryptedString.of("Auto Inv Totem"), EncryptedString.of("Automatically equips a totem in your offhand and main hand if empty"), -1, Category.COMBAT);
      this.addSettings(new Setting[]{this.delay, this.hotbar, this.totemSlot, this.autoSwitch, this.forceTotem, this.autoOpen, this.stayOpenDuration});
   }

   public void onEnable() {
      this.delayCounter = -1;
      this.stayOpenCounter = -1;
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.shouldOpenInventory() && this.autoOpen.getValue()) {
         this.mc.setScreen(new FakeInvScreen(this.mc.player));
      }

      if (!(this.mc.currentScreen instanceof InventoryScreen) && !(this.mc.currentScreen instanceof FakeInvScreen)) {
         this.delayCounter = -1;
         this.stayOpenCounter = -1;
      } else {
         if (this.delayCounter == -1) {
            this.delayCounter = this.delay.getIntValue();
         }

         if (this.stayOpenCounter == -1) {
            this.stayOpenCounter = this.stayOpenDuration.getIntValue();
         }

         if (this.delayCounter > 0) {
            --this.delayCounter;
         }

         PlayerInventory getInventory = this.mc.player.getInventory();
         if (this.autoSwitch.getValue()) {
            getInventory.selectedSlot = this.totemSlot.getIntValue() - 1;
         }

         if (this.delayCounter <= 0) {
            if (((ItemStack)getInventory.offHand.get(0)).getItem() != Items.TOTEM_OF_UNDYING) {
               int l = this.findTotemSlot();
               if (l != -1) {
                  this.mc.interactionManager.clickSlot(((PlayerScreenHandler)((InventoryScreen)this.mc.currentScreen).getScreenHandler()).syncId, l, 40, net.minecraft.screen.slot.SlotActionType.SWAP, this.mc.player);
                  return;
               }
            }

            if (this.hotbar.getValue()) {
               ItemStack getAbilities = this.mc.player.getMainHandStack();
               if (getAbilities.isEmpty() || this.forceTotem.getValue() && getAbilities.getItem() != Items.TOTEM_OF_UNDYING) {
                  int i = this.findTotemSlot();
                  if (i != -1) {
                     this.mc.interactionManager.clickSlot(((PlayerScreenHandler)((InventoryScreen)this.mc.currentScreen).getScreenHandler()).syncId, i, getInventory.selectedSlot, net.minecraft.screen.slot.SlotActionType.SWAP, this.mc.player);
                     return;
                  }
               }
            }

            if (this.isTotemEquipped() && this.autoOpen.getValue()) {
               if (this.stayOpenCounter != 0) {
                  --this.stayOpenCounter;
                  return;
               }

               this.mc.currentScreen.close();
               this.stayOpenCounter = this.stayOpenDuration.getIntValue();
            }
         }

      }
   }

   public boolean isTotemEquipped() {
      if (this.hotbar.getValue()) {
         return this.mc.player.getInventory().getStack(this.totemSlot.getIntValue() - 1).getItem() == Items.TOTEM_OF_UNDYING && this.mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING && this.mc.currentScreen instanceof FakeInvScreen;
      } else {
         return this.mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING && this.mc.currentScreen instanceof FakeInvScreen;
      }
   }

   public boolean shouldOpenInventory() {
      if (this.hotbar.getValue()) {
         return (this.mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING || this.mc.player.getInventory().getStack(this.totemSlot.getIntValue() - 1).getItem() != Items.TOTEM_OF_UNDYING) && !(this.mc.currentScreen instanceof FakeInvScreen) && this.countTotems((item) -> item == Items.TOTEM_OF_UNDYING) != 0;
      } else {
         return this.mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING && !(this.mc.currentScreen instanceof FakeInvScreen) && this.countTotems((item2) -> item2 == Items.TOTEM_OF_UNDYING) != 0;
      }
   }

   private int findTotemSlot() {
      PlayerInventory inventory = this.mc.player.getInventory();

      for(int i = 0; i < inventory.main.size(); ++i) {
         if (((ItemStack)inventory.main.get(i)).getItem() == Items.TOTEM_OF_UNDYING) {
            return i;
         }
      }

      return -1;
   }

   private int countTotems(Predicate predicate) {
      int count = 0;
      PlayerInventory inventory = this.mc.player.getInventory();

      for(int i = 0; i < inventory.main.size(); ++i) {
         ItemStack stack = (ItemStack)inventory.main.get(i);
         if (predicate.test(stack.getItem())) {
            count += stack.getCount();
         }
      }

      return count;
   }
}
