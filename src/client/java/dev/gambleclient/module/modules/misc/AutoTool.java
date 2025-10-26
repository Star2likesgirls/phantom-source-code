package dev.gambleclient.module.modules.misc;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.AttackBlockEvent;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.InventoryUtil;
import java.util.function.Predicate;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.block.BambooShootBlock;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;

public final class AutoTool extends Module {
   private final BooleanSetting antiBreak = new BooleanSetting(EncryptedString.of("Anti Break"), true);
   private final NumberSetting antiBreakPercentage = new NumberSetting(EncryptedString.of("Anti Break Percentage"), (double)1.0F, (double)100.0F, (double)5.0F, (double)1.0F);
   private boolean isToolSwapping;
   private int keybindCounter;
   private int selectedToolSlot;

   public AutoTool() {
      super(EncryptedString.of("Auto Tool"), EncryptedString.of("Module that automatically switches to best tool"), -1, Category.MISC);
      this.addSettings(new Setting[]{this.antiBreak, this.antiBreakPercentage});
   }

   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.keybindCounter <= 0 && this.isToolSwapping && this.selectedToolSlot != -1) {
         InventoryUtil.swap(this.selectedToolSlot);
         this.isToolSwapping = false;
      } else {
         --this.keybindCounter;
      }

   }

   @EventListener
   public void handleAttackBlockEvent(AttackBlockEvent attackBlockEvent) {
      BlockState getBlockState = this.mc.world.getBlockState(attackBlockEvent.pos);
      ItemStack getBlockEntity = this.mc.player.getMainHandStack();
      double n = (double)-1.0F;
      this.selectedToolSlot = -1;

      for(int i = 0; i < 9; ++i) {
         double a = calculateToolEfficiency(this.mc.player.getInventory().getStack(i), (net.minecraft.block.BlockState)getBlockState, (itemStack) -> !this.isToolBreakingSoon((ItemStack)itemStack));
         if (a >= (double)0.0F && a > n) {
            this.selectedToolSlot = i;
            n = a;
         }
      }

      if (this.selectedToolSlot != -1 && n > calculateToolEfficiency((net.minecraft.item.ItemStack)getBlockEntity, getBlockState, (itemStack2) -> !this.isToolBreakingSoon((ItemStack)itemStack2)) || this.isToolBreakingSoon(getBlockEntity) || !isToolItemStack(getBlockEntity)) {
         InventoryUtil.swap(this.selectedToolSlot);
      }

      ItemStack method_8322 = this.mc.player.getMainHandStack();
      if (this.isToolBreakingSoon(method_8322) && isToolItemStack(method_8322)) {
         this.mc.options.attackKey.setPressed(false);
         attackBlockEvent.cancel();
      }

   }

   public static double calculateToolEfficiency(ItemStack itemStack, BlockState blockState, Predicate predicate) {
      if (predicate.test(itemStack) && isToolItemStack(itemStack)) {
         return !itemStack.isSuitableFor(blockState) && (!(itemStack.getItem() instanceof SwordItem) || !(blockState.getBlock() instanceof BambooBlock) && !(blockState.getBlock() instanceof BambooShootBlock)) && (!(itemStack.getItem() instanceof ShearsItem) || !(blockState.getBlock() instanceof LeavesBlock)) && !blockState.isIn(BlockTags.WOOL) ? (double)-1.0F : (double)0.0F + (double)(itemStack.getMiningSpeedMultiplier(blockState) * 1000.0F);
      } else {
         return (double)-1.0F;
      }
   }

   public static boolean isToolItemStack(ItemStack itemStack) {
      return isToolItem(itemStack.getItem());
   }

   public static boolean isToolItem(Item item) {
      return item instanceof ToolItem || item instanceof ShearsItem;
   }

   private boolean isToolBreakingSoon(ItemStack itemStack) {
      return this.antiBreak.getValue() && itemStack.getMaxDamage() - itemStack.getDamage() < itemStack.getMaxDamage() * this.antiBreakPercentage.getIntValue() / 100;
   }
}
