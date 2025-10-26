package dev.gambleclient.module.modules.combat;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.AttackEvent;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EnchantmentUtil;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.InventoryUtil;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.enchantment.Enchantments;

public final class MaceSwap extends Module {
   private final BooleanSetting enableWindBurst = new BooleanSetting(EncryptedString.of("Wind Burst"), true);
   private final BooleanSetting enableBreach = new BooleanSetting(EncryptedString.of("Breach"), true);
   private final BooleanSetting onlySword = new BooleanSetting(EncryptedString.of("Only Sword"), false);
   private final BooleanSetting onlyAxe = new BooleanSetting(EncryptedString.of("Only Axe"), false);
   private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true);
   private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), (double)0.0F, (double)20.0F, (double)0.0F, (double)1.0F);
   private boolean isSwitching;
   private int previousSlot;
   private int currentSwitchDelay;

   public MaceSwap() {
      super(EncryptedString.of("Mace Swap"), EncryptedString.of("Switches to a mace when attacking."), -1, Category.COMBAT);
      this.addSettings(new Setting[]{this.enableWindBurst, this.enableBreach, this.onlySword, this.onlyAxe, this.switchBack, this.switchDelay});
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
         if (this.mc.player != null) {
            if (this.isSwitching) {
               if (this.switchBack.getValue()) {
                  this.performSwitchBack();
               } else {
                  this.resetState();
               }
            }

         }
      }
   }

   @EventListener
   public void onAttack(AttackEvent attackEvent) {
      if (this.mc.player != null) {
         if (this.isValidWeapon()) {
            if (this.previousSlot == -1) {
               this.previousSlot = this.mc.player.getInventory().selectedSlot;
            }

            if ((!this.enableWindBurst.getValue() || !this.enableBreach.getValue()) && (this.enableWindBurst.getValue() || this.enableBreach.getValue())) {
               if (this.enableWindBurst.getValue()) {
                  InventoryUtil.swapStack((itemStack) -> {
                     return EnchantmentUtil.hasEnchantment((net.minecraft.item.ItemStack)itemStack, Enchantments.WIND_BURST);
                  });
               }

               if (this.enableBreach.getValue()) {
                  InventoryUtil.swapStack((itemStack2) -> {
                     return EnchantmentUtil.hasEnchantment((net.minecraft.item.ItemStack)itemStack2, Enchantments.BREACH);
                  });
               }
            } else {
               InventoryUtil.swap(Items.MACE);
            }

            this.isSwitching = true;
         }
      }
   }

   private boolean isValidWeapon() {
      Item item = this.mc.player.getMainHandStack().getItem();
      if (this.onlySword.getValue() && this.onlyAxe.getValue()) {
         return item instanceof SwordItem || item instanceof AxeItem;
      } else {
         return (!this.onlySword.getValue() || item instanceof SwordItem) && (!this.onlyAxe.getValue() || item instanceof AxeItem);
      }
   }

   private void performSwitchBack() {
      if (this.currentSwitchDelay < this.switchDelay.getIntValue()) {
         ++this.currentSwitchDelay;
      } else {
         InventoryUtil.swap(this.previousSlot);
         this.resetState();
      }
   }

   private void resetState() {
      this.previousSlot = -1;
      this.currentSwitchDelay = 0;
      this.isSwitching = false;
   }
}
