package dev.gambleclient.module.modules.combat;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.PreItemUseEvent;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BindSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.BlockUtil;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.KeyUtils;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.client.texture.Scaling;

public final class AutoCrystal extends Module {
   private final BindSetting activateKey = (new BindSetting(EncryptedString.of("Activate Key"), 1, false)).setDescription(EncryptedString.of("Key that does the crystalling"));
   private final NumberSetting placeDelay = new NumberSetting(EncryptedString.of("Place Delay"), (double)0.0F, (double)20.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting breakDelay = new NumberSetting(EncryptedString.of("Break Delay"), (double)0.0F, (double)20.0F, (double)0.0F, (double)1.0F);
   private int placeDelayCounter;
   private int breakDelayCounter;
   public boolean isActive;

   public AutoCrystal() {
      super(EncryptedString.of("Auto Crystal"), EncryptedString.of("Automatically crystals fast for you"), -1, Category.COMBAT);
      this.addSettings(new Setting[]{this.activateKey, this.placeDelay, this.breakDelay});
   }

   public void onEnable() {
      this.resetCounters();
      this.isActive = false;
      super.onEnable();
   }

   private void resetCounters() {
      this.placeDelayCounter = 0;
      this.breakDelayCounter = 0;
   }

   @EventListener
   public void onTick(TickEvent tickEvent) {
      if (this.mc.currentScreen == null) {
         this.updateCounters();
         if (!this.mc.player.isUsingItem()) {
            if (this.isKeyActive()) {
               if (this.mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
                  this.handleInteraction();
               }
            }
         }
      }
   }

   private void updateCounters() {
      if (this.placeDelayCounter > 0) {
         --this.placeDelayCounter;
      }

      if (this.breakDelayCounter > 0) {
         --this.breakDelayCounter;
      }

   }

   private boolean isKeyActive() {
      int d = this.activateKey.getValue();
      if (d != -1 && !KeyUtils.isKeyPressed(d)) {
         this.resetCounters();
         return this.isActive = false;
      } else {
         return this.isActive = true;
      }
   }

   private void handleInteraction() {
      HitResult crosshairTarget = this.mc.crosshairTarget;
      if (this.mc.crosshairTarget instanceof BlockHitResult) {
         this.handleBlockInteraction((BlockHitResult)crosshairTarget);
      } else {
         HitResult var3 = this.mc.crosshairTarget;
         if (var3 instanceof EntityHitResult) {
            EntityHitResult entityHitResult = (EntityHitResult)var3;
            this.handleEntityInteraction(entityHitResult);
         }
      }

   }

   private void handleBlockInteraction(BlockHitResult blockHitResult) {
      if (blockHitResult.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
         if (this.placeDelayCounter <= 0) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            if ((BlockUtil.isBlockAtPosition(blockPos, Blocks.OBSIDIAN) || BlockUtil.isBlockAtPosition(blockPos, Blocks.BEDROCK)) && this.isValidCrystalPlacement(blockPos)) {
               BlockUtil.interactWithBlock(blockHitResult, true);
               this.placeDelayCounter = this.placeDelay.getIntValue();
            }

         }
      }
   }

   private void handleEntityInteraction(EntityHitResult entityHitResult) {
      if (this.breakDelayCounter <= 0) {
         Entity entity = entityHitResult.getEntity();
         if (entity instanceof EndCrystalEntity || entity instanceof SlimeEntity) {
            this.mc.interactionManager.attackEntity(this.mc.player, entity);
            this.mc.player.swingHand(Hand.MAIN_HAND);
            this.breakDelayCounter = this.breakDelay.getIntValue();
         }
      }
   }

   @EventListener
   public void onPreItemUse(PreItemUseEvent preItemUseEvent) {
      if (this.mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
         HitResult var3 = this.mc.crosshairTarget;
         if (var3 instanceof BlockHitResult) {
            BlockHitResult blockHitResult = (BlockHitResult)var3;
            if (this.mc.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
               BlockPos blockPos = blockHitResult.getBlockPos();
               if (BlockUtil.isBlockAtPosition(blockPos, Blocks.OBSIDIAN) || BlockUtil.isBlockAtPosition(blockPos, Blocks.BEDROCK)) {
                  preItemUseEvent.cancel();
               }

            }
         }
      }
   }

   private boolean isValidCrystalPlacement(BlockPos blockPos) {
      BlockPos up = blockPos.up();
      if (!this.mc.world.isAir(up)) {
         return false;
      } else {
         int getX = up.getX();
         int getY = up.getY();
         int compareTo = up.getZ();
         return this.mc.world.getOtherEntities((Entity)null, new Box((double)getX, (double)getY, (double)compareTo, (double)getX + (double)1.0F, (double)getY + (double)2.0F, (double)compareTo + (double)1.0F)).isEmpty();
      }
   }
}
