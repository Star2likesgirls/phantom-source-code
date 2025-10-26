package dev.gambleclient.module.modules.ai;

import net.minecraft.util.math.BlockPos;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;

public class SimpleSneakCentering {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private static final double TOLERANCE = 0.15;
   private boolean active = false;
   private BlockPos targetBlock = null;
   private double targetX;
   private double targetZ;
   private int tickCount = 0;
   private double lastDistanceToTarget = (double)0.0F;

   public boolean startCentering() {
      if (this.mc.player == null) {
         return false;
      } else {
         this.targetBlock = this.mc.player.getBlockPos();
         this.targetX = (double)this.targetBlock.getX() + (double)0.5F;
         this.targetZ = (double)this.targetBlock.getZ() + (double)0.5F;
         double offsetX = Math.abs(this.mc.player.getX() - this.targetX);
         double offsetZ = Math.abs(this.mc.player.getZ() - this.targetZ);
         if (offsetX <= 0.15 && offsetZ <= 0.15) {
            return false;
         } else {
            this.active = true;
            this.tickCount = 0;
            this.lastDistanceToTarget = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
            return true;
         }
      }
   }

   public boolean tick() {
      if (this.active && this.mc.player != null) {
         ++this.tickCount;
         double worldOffsetX = this.mc.player.getX() - this.targetX;
         double worldOffsetZ = this.mc.player.getZ() - this.targetZ;
         double currentDistance = Math.sqrt(worldOffsetX * worldOffsetX + worldOffsetZ * worldOffsetZ);
         if (currentDistance > this.lastDistanceToTarget + 0.01) {
         }

         this.lastDistanceToTarget = currentDistance;
         if (Math.abs(worldOffsetX) <= 0.15 && Math.abs(worldOffsetZ) <= 0.15) {
            this.stopCentering();
            return false;
         } else {
            this.releaseAllKeys();
            this.setPressed(this.mc.options.sneakKey, true);
            float yaw = this.mc.player.getYaw();
            double yawRad = Math.toRadians((double)yaw);
            double moveX = -worldOffsetX;
            double moveZ = -worldOffsetZ;
            double relativeForward = moveX * -Math.sin(yawRad) + moveZ * Math.cos(yawRad);
            double relativeStrafe = moveX * -Math.cos(yawRad) + moveZ * -Math.sin(yawRad);
            if (Math.abs(relativeForward) > 0.075) {
               if (relativeForward > (double)0.0F) {
                  this.setPressed(this.mc.options.forwardKey, true);
               } else {
                  this.setPressed(this.mc.options.backKey, true);
               }
            }

            if (Math.abs(relativeStrafe) > 0.075) {
               if (relativeStrafe > (double)0.0F) {
                  this.setPressed(this.mc.options.rightKey, true);
               } else {
                  this.setPressed(this.mc.options.leftKey, true);
               }
            }

            if (this.tickCount > 100) {
               this.stopCentering();
               return false;
            } else {
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public void stopCentering() {
      this.active = false;
      this.targetBlock = null;
      this.releaseAllKeys();
      this.tickCount = 0;
   }

   private void releaseAllKeys() {
      this.setPressed(this.mc.options.forwardKey, false);
      this.setPressed(this.mc.options.backKey, false);
      this.setPressed(this.mc.options.leftKey, false);
      this.setPressed(this.mc.options.rightKey, false);
      this.setPressed(this.mc.options.sneakKey, false);
   }

   private void setPressed(KeyBinding key, boolean pressed) {
      key.setPressed(pressed);
   }

   public boolean isCentering() {
      return this.active;
   }

   public boolean isDone() {
      if (this.active && this.mc.player != null) {
         double offsetX = Math.abs(this.mc.player.getX() - this.targetX);
         double offsetZ = Math.abs(this.mc.player.getZ() - this.targetZ);
         return offsetX <= 0.15 && offsetZ <= 0.15;
      } else {
         return true;
      }
   }
}
