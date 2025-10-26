package dev.gambleclient.module.modules.misc;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.PostItemUseEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.component.DataComponentTypes;

public class FastPlace extends Module {
   private final BooleanSetting onlyXP = new BooleanSetting("Only XP", false);
   private final BooleanSetting allowBlocks = new BooleanSetting("Blocks", true);
   private final BooleanSetting allowItems = new BooleanSetting("Items", true);
   private final NumberSetting useDelay = new NumberSetting("Delay", (double)0.0F, (double)10.0F, (double)0.0F, (double)1.0F);

   public FastPlace() {
      super(EncryptedString.of("Fast Place"), EncryptedString.of("Spams use net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action."), -1, Category.MISC);
      this.addSettings(new Setting[]{this.onlyXP, this.allowBlocks, this.allowItems, this.useDelay});
   }

   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onPostItemUse(PostItemUseEvent postItemUseEvent) {
      ItemStack getMainHandStack = this.mc.player.getMainHandStack();
      ItemStack getItemUseTime = this.mc.player.getOffHandStack();
      Item item = getMainHandStack.getItem();
      Item item2 = this.mc.player.getOffHandStack().getItem();
      if (getMainHandStack.isOf(Items.EXPERIENCE_BOTTLE) || getItemUseTime.isOf(Items.EXPERIENCE_BOTTLE) || !this.onlyXP.getValue()) {
         if (!this.onlyXP.getValue()) {
            if (!(item instanceof BlockItem) && !(item2 instanceof BlockItem)) {
               if (!this.allowItems.getValue()) {
                  return;
               }
            } else if (!this.allowBlocks.getValue()) {
               return;
            }
         }

         if (item.getComponents().get(DataComponentTypes.FOOD) == null) {
            if (item2.getComponents().get(DataComponentTypes.FOOD) == null) {
               if (!getMainHandStack.isOf(Items.RESPAWN_ANCHOR) && !getMainHandStack.isOf(Items.GLOWSTONE) && !getItemUseTime.isOf(Items.RESPAWN_ANCHOR) && !getItemUseTime.isOf(Items.GLOWSTONE)) {
                  if (!(item instanceof RangedWeaponItem) && !(item2 instanceof RangedWeaponItem)) {
                     postItemUseEvent.cooldown = this.useDelay.getIntValue();
                  }
               }
            }
         }
      }
   }
}
