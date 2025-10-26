package dev.gambleclient.module.modules.combat;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BindSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.BlockUtil;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.InventoryUtil;
import dev.gambleclient.utils.KeyUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;

public final class DoubleAnchor extends Module {
   private final BindSetting activateKey = (new BindSetting(EncryptedString.of("Activate Key"), 71, false)).setDescription(EncryptedString.of("Key that starts double anchoring"));
   private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), (double)0.0F, (double)20.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting totemSlot = new NumberSetting(EncryptedString.of("Totem Slot"), (double)1.0F, (double)9.0F, (double)1.0F, (double)1.0F);
   private int delayCounter = 0;
   private int step = 0;
   private boolean isAnchoring = false;

   public DoubleAnchor() {
      super(EncryptedString.of("Double Anchor"), EncryptedString.of("Automatically Places 2 anchors"), -1, Category.COMBAT);
      this.addSettings(new Setting[]{this.switchDelay, this.totemSlot, this.activateKey});
   }

   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.mc.currentScreen == null) {
         if (this.mc.player != null) {
            if (this.hasRequiredItems()) {
               if (this.isAnchoring || this.checkActivationKey()) {
                  HitResult crosshairTarget = this.mc.crosshairTarget;
                  if (this.mc.crosshairTarget instanceof BlockHitResult && !BlockUtil.isBlockAtPosition(((BlockHitResult)crosshairTarget).getBlockPos(), Blocks.AIR)) {
                     if (this.delayCounter < this.switchDelay.getIntValue()) {
                        ++this.delayCounter;
                     } else {
                        if (this.step == 0) {
                           InventoryUtil.swap(Items.RESPAWN_ANCHOR);
                        } else if (this.step == 1) {
                           BlockUtil.interactWithBlock((BlockHitResult)crosshairTarget, true);
                        } else if (this.step == 2) {
                           InventoryUtil.swap(Items.GLOWSTONE);
                        } else if (this.step == 3) {
                           BlockUtil.interactWithBlock((BlockHitResult)crosshairTarget, true);
                        } else if (this.step == 4) {
                           InventoryUtil.swap(Items.RESPAWN_ANCHOR);
                        } else if (this.step == 5) {
                           BlockUtil.interactWithBlock((BlockHitResult)crosshairTarget, true);
                           BlockUtil.interactWithBlock((BlockHitResult)crosshairTarget, true);
                        } else if (this.step == 6) {
                           InventoryUtil.swap(Items.GLOWSTONE);
                        } else if (this.step == 7) {
                           BlockUtil.interactWithBlock((BlockHitResult)crosshairTarget, true);
                        } else if (this.step == 8) {
                           InventoryUtil.swap(this.totemSlot.getIntValue() - 1);
                        } else if (this.step == 9) {
                           BlockUtil.interactWithBlock((BlockHitResult)crosshairTarget, true);
                        } else if (this.step == 10) {
                           this.isAnchoring = false;
                           this.step = 0;
                           this.resetState();
                           return;
                        }

                        ++this.step;
                     }
                  } else {
                     this.isAnchoring = false;
                     this.resetState();
                  }
               }
            }
         }
      }
   }

   private boolean hasRequiredItems() {
      boolean b = false;
      boolean b2 = false;

      for(int i = 0; i < 9; ++i) {
         ItemStack getStack = this.mc.player.getInventory().getStack(i);
         if (getStack.getItem().equals(Items.RESPAWN_ANCHOR)) {
            b = true;
         }

         if (getStack.getItem().equals(Items.GLOWSTONE)) {
            b2 = true;
         }
      }

      return b && b2;
   }

   private boolean checkActivationKey() {
      int d = this.activateKey.getValue();
      if (d != -1 && KeyUtils.isKeyPressed(d)) {
         return this.isAnchoring = true;
      } else {
         this.resetState();
         return false;
      }
   }

   private void resetState() {
      this.delayCounter = 0;
   }

   public boolean isAnchoringActive() {
      return this.isAnchoring;
   }
}
