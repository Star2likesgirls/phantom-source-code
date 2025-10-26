package dev.gambleclient.module.modules.ai;

import java.io.PrintStream;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluids;

public class SafetyValidator {
   private Vec3d lastPosition;
   private int stuckTicks = 0;
   private int jumpCooldown = 0;
   private int unstuckAttempts = 0;
   private boolean needsModuleReset = false;
   private double lastYPosition = (double)0.0F;
   private int verticalStuckTicks = 0;
   private int miningDownAttempts = 0;
   private Vec3d lastMiningDownPosition;
   private boolean jumpedWhileMoving = false;
   private int jumpFollowUpTicks = 0;
   private int recoveryGracePeriod = 0;
   private double recoveryStartY = (double)0.0F;
   private boolean inRecovery = false;
   private static final int RECOVERY_GRACE_TICKS = 40;
   private static final int STUCK_THRESHOLD = 60;
   private static final int MINING_DOWN_STUCK_THRESHOLD = 40;
   private static final int JUMP_COOLDOWN_TICKS = 20;
   private static final double MIN_DURABILITY_PERCENT = 0.1;
   private static final double MOVEMENT_THRESHOLD = (double)1.0F;
   private static final double VERTICAL_MOVEMENT_THRESHOLD = (double)0.5F;
   private Vec3d[] positionHistory = new Vec3d[20];
   private int historyIndex = 0;

   public boolean canContinue(PlayerEntity player, int maxY) {
      if (player.getY() > (double)maxY) {
         return false;
      } else {
         if (!player.isOnGround()) {
            boolean hasGroundNearby = false;
            int groundDistance = 0;
            int i = 1;

            while(true) {
               if (i <= 4) {
                  BlockPos checkPos = player.getBlockPos().down(i);
                  if (player.getWorld().getBlockState(checkPos).isAir()) {
                     ++i;
                     continue;
                  }

                  hasGroundNearby = true;
                  groundDistance = i;
               }

               if (hasGroundNearby && groundDistance <= 2) {
                  return true;
               }

               if (!hasGroundNearby && player.getVelocity().y < (double)-0.5F) {
                  return false;
               }

               BlockPos belowPos = player.getBlockPos().down();
               BlockState belowState = player.getWorld().getBlockState(belowPos);
               if (belowState.getFluidState().getFluid() == Fluids.LAVA || belowState.getFluidState().getFluid() == Fluids.WATER || belowState.getFluidState().getFluid() == Fluids.FLOWING_LAVA || belowState.getFluidState().getFluid() == Fluids.FLOWING_WATER) {
                  return false;
               }
               break;
            }
         }

         ItemStack mainHand = player.getMainHandStack();
         if (this.isMiningTool(mainHand)) {
            int currentDamage = mainHand.getDamage();
            int maxDamage = mainHand.getMaxDamage();
            if (maxDamage > 0) {
               int remainingDurability = maxDamage - currentDamage;
               double durabilityPercent = (double)remainingDurability / (double)maxDamage;
               if (durabilityPercent <= 0.1) {
                  System.out.println("WARNING: Tool durability critically low!");
                  System.out.println("  Current durability: " + remainingDurability + "/" + maxDamage);
                  PrintStream var15 = System.out;
                  Object[] var16 = new Object[]{durabilityPercent * (double)100.0F};
                  var15.println("  Percentage: " + String.format("%.1f%%", var16));
                  return false;
               }

               if (durabilityPercent <= 0.15 && durabilityPercent > 0.1) {
                  PrintStream var10000 = System.out;
                  Object[] var10002 = new Object[]{durabilityPercent * (double)100.0F};
                  var10000.println("CAUTION: Tool durability at " + String.format("%.1f%%", var10002) + " - will stop at 10%");
               }
            }

            return true;
         } else {
            System.out.println("WARNING: No mining tool in main hand!");
            return false;
         }
      }
   }

   private boolean isMiningTool(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return false;
      } else {
         return itemStack.getItem() instanceof MiningToolItem || itemStack.getItem() instanceof ShearsItem || itemStack.getItem() instanceof PickaxeItem;
      }
   }

   public StuckRecoveryAction checkAndHandleStuck(PlayerEntity player, MiningMode mode) {
      if (this.jumpCooldown > 0) {
         --this.jumpCooldown;
      }

      if (this.recoveryGracePeriod > 0) {
         --this.recoveryGracePeriod;
         if (mode == SafetyValidator.MiningMode.MINING_DOWN && this.recoveryGracePeriod == 1) {
            double currentY = player.getY();
            double progressMade = Math.abs(this.recoveryStartY - currentY);
            if (progressMade < (double)1.0F) {
               int var10001 = this.miningDownAttempts;
               System.out.println("Recovery attempt " + var10001 + " failed (moved only " + String.format("%.2f", progressMade) + " blocks)");
               this.inRecovery = false;
            } else {
               PrintStream var10000 = System.out;
               Object[] var10002 = new Object[]{progressMade};
               var10000.println("Recovery successful! Moved " + String.format("%.2f", var10002) + " blocks");
               this.miningDownAttempts = 0;
               this.inRecovery = false;
            }
         }

         return SafetyValidator.StuckRecoveryAction.NONE;
      } else {
         this.inRecovery = false;
         switch (mode.ordinal()) {
            case 0:
            default:
               return this.checkMovementStuck(player, true);
            case 1:
               return this.checkMiningDownStuck(player);
            case 2:
               return this.checkMovementStuck(player, false);
         }
      }
   }

   private StuckRecoveryAction checkMiningDownStuck(PlayerEntity player) {
      double currentY = player.getY();
      if (this.lastMiningDownPosition == null) {
         this.lastMiningDownPosition = player.getPos();
         this.lastYPosition = currentY;
         return SafetyValidator.StuckRecoveryAction.NONE;
      } else {
         double verticalDistance = Math.abs(currentY - this.lastYPosition);
         double horizontalDistance = Math.sqrt(Math.pow(player.getX() - this.lastMiningDownPosition.x, (double)2.0F) + Math.pow(player.getZ() - this.lastMiningDownPosition.z, (double)2.0F));
         boolean isStuck = verticalDistance < (double)0.5F || horizontalDistance > (double)2.0F;
         if (isStuck) {
            ++this.verticalStuckTicks;
            PrintStream var10000 = System.out;
            String var10001 = String.format("%.3f", verticalDistance);
            var10000.println("MINING DOWN STUCK: Y-movement=" + var10001 + ", H-movement=" + String.format("%.3f", horizontalDistance) + ", Ticks=" + this.verticalStuckTicks + ", Attempts=" + this.miningDownAttempts);
            if (this.verticalStuckTicks >= 40) {
               ++this.miningDownAttempts;
               this.verticalStuckTicks = 0;
               this.recoveryStartY = currentY;
               this.inRecovery = true;
               this.recoveryGracePeriod = 40;
               this.lastYPosition = currentY;
               this.lastMiningDownPosition = player.getPos();
               if (this.miningDownAttempts == 1) {
                  System.out.println("=== MINING DOWN RECOVERY: ATTEMPT 1 - RETOGGLE KEYS ===");
                  return SafetyValidator.StuckRecoveryAction.RETOGGLE_KEYS;
               }

               if (this.miningDownAttempts == 2) {
                  System.out.println("=== MINING DOWN RECOVERY: ATTEMPT 2 - MOVE AND RETRY ===");
                  return SafetyValidator.StuckRecoveryAction.MOVE_AND_RETRY;
               }

               if (this.miningDownAttempts == 3) {
                  System.out.println("=== MINING DOWN RECOVERY: ATTEMPT 3 - FIND NEW SPOT ===");
                  return SafetyValidator.StuckRecoveryAction.FIND_NEW_SPOT;
               }

               if (this.miningDownAttempts >= 4) {
                  System.out.println("=== MINING DOWN RECOVERY: FINAL - REQUEST MODULE RESET ===");
                  this.needsModuleReset = true;
                  return SafetyValidator.StuckRecoveryAction.NEEDS_RESET;
               }
            }
         } else {
            if (this.verticalStuckTicks > 0) {
               PrintStream var9 = System.out;
               Object[] var10002 = new Object[]{verticalDistance};
               var9.println("Vertical movement detected (" + String.format("%.2f", var10002) + " blocks), resetting stuck counter");
            }

            this.verticalStuckTicks = 0;
            if (verticalDistance > (double)1.0F) {
               this.miningDownAttempts = 0;
            }

            this.lastYPosition = currentY;
            this.lastMiningDownPosition = player.getPos();
         }

         return SafetyValidator.StuckRecoveryAction.NONE;
      }
   }

   private StuckRecoveryAction checkMovementStuck(PlayerEntity player, boolean allowJumping) {
      Vec3d currentPos = new Vec3d(player.getX(), player.getY(), player.getZ());
      this.positionHistory[this.historyIndex] = currentPos;
      this.historyIndex = (this.historyIndex + 1) % this.positionHistory.length;
      if (this.lastPosition == null) {
         this.lastPosition = currentPos;
         return SafetyValidator.StuckRecoveryAction.NONE;
      } else {
         double horizontalDistance = Math.sqrt(Math.pow(currentPos.x - this.lastPosition.x, (double)2.0F) + Math.pow(currentPos.z - this.lastPosition.z, (double)2.0F));
         Vec3d oldPos = this.positionHistory[(this.historyIndex + 1) % this.positionHistory.length];
         double longerTermMovement = (double)0.0F;
         if (oldPos != null) {
            longerTermMovement = Math.sqrt(Math.pow(currentPos.x - oldPos.x, (double)2.0F) + Math.pow(currentPos.z - oldPos.z, (double)2.0F));
         }

         if (this.jumpedWhileMoving && this.jumpFollowUpTicks > 0) {
            --this.jumpFollowUpTicks;
            if (horizontalDistance > (double)1.0F) {
               System.out.println("Movement detected after jump+forward, stopping movement");
               this.jumpedWhileMoving = false;
               this.jumpFollowUpTicks = 0;
               return SafetyValidator.StuckRecoveryAction.STOP_MOVEMENT;
            }

            if (this.jumpFollowUpTicks == 0) {
               this.jumpedWhileMoving = false;
               System.out.println("No movement after jump+forward, proceeding to next attempt");
            }
         }

         boolean isStuck = horizontalDistance < (double)1.0F && (oldPos == null || longerTermMovement < (double)0.5F);
         if (isStuck) {
            ++this.stuckTicks;
            int var10001 = this.stuckTicks;
            System.out.println("MOVEMENT STUCK: Ticks=" + var10001 + ", Attempts=" + this.unstuckAttempts + ", Movement=" + String.format("%.3f", horizontalDistance) + ", AllowJump=" + allowJumping);
            if (this.stuckTicks >= 60 && this.jumpCooldown <= 0) {
               ++this.unstuckAttempts;
               this.stuckTicks = 0;
               this.lastPosition = currentPos;
               if (this.unstuckAttempts == 1) {
                  if (allowJumping && player.isOnGround()) {
                     System.out.println("=== STUCK RECOVERY: ATTEMPT 1 - JUMP WHILE MOVING FORWARD ===");
                     this.jumpedWhileMoving = true;
                     this.jumpFollowUpTicks = 20;
                     this.jumpCooldown = 20;
                     return SafetyValidator.StuckRecoveryAction.JUMP_FORWARD;
                  }

                  System.out.println("=== STUCK RECOVERY: ATTEMPT 1 - RECALCULATE PATH (no jump) ===");
                  return SafetyValidator.StuckRecoveryAction.RECALCULATE_PATH;
               }

               if (this.unstuckAttempts == 2) {
                  if (!allowJumping) {
                     System.out.println("=== STUCK RECOVERY: ATTEMPT 2 - FIND NEW SPOT ===");
                     return SafetyValidator.StuckRecoveryAction.FIND_NEW_SPOT;
                  }

                  System.out.println("=== STUCK RECOVERY: ATTEMPT 2 - RECALCULATE PATH ===");
                  return SafetyValidator.StuckRecoveryAction.RECALCULATE_PATH;
               }

               if (this.unstuckAttempts == 3) {
                  System.out.println("=== STUCK RECOVERY: ATTEMPT 3 - PATH BLOCKED, TRIGGER RTP ===");
                  return SafetyValidator.StuckRecoveryAction.PATH_BLOCKED_RTP;
               }

               if (this.unstuckAttempts >= 4) {
                  System.out.println("=== STUCK RECOVERY: FINAL - REQUESTING MODULE RESET ===");
                  this.needsModuleReset = true;
                  this.jumpCooldown = 40;
                  return SafetyValidator.StuckRecoveryAction.NEEDS_RESET;
               }
            }
         } else {
            if (this.stuckTicks > 0) {
               System.out.println("Movement detected, resetting stuck counter");
            }

            this.stuckTicks = 0;
            this.unstuckAttempts = 0;
            this.needsModuleReset = false;
            this.jumpedWhileMoving = false;
            this.jumpFollowUpTicks = 0;
            this.lastPosition = currentPos;
         }

         return SafetyValidator.StuckRecoveryAction.NONE;
      }
   }

   public void resetMiningDown() {
      this.verticalStuckTicks = 0;
      this.miningDownAttempts = 0;
      this.lastMiningDownPosition = null;
      this.lastYPosition = (double)0.0F;
      this.recoveryGracePeriod = 0;
      this.inRecovery = false;
      this.recoveryStartY = (double)0.0F;
   }

   public boolean needsModuleReset() {
      return this.needsModuleReset;
   }

   public void acknowledgeReset() {
      this.needsModuleReset = false;
      this.unstuckAttempts = 0;
      this.stuckTicks = 0;
      this.miningDownAttempts = 0;
      this.verticalStuckTicks = 0;
      this.recoveryGracePeriod = 0;
      this.inRecovery = false;
   }

   public void reset() {
      this.stuckTicks = 0;
      this.jumpCooldown = 0;
      this.unstuckAttempts = 0;
      this.needsModuleReset = false;
      this.lastPosition = null;
      this.positionHistory = new Vec3d[20];
      this.historyIndex = 0;
      this.resetMiningDown();
   }

   public boolean isStuck() {
      return this.stuckTicks >= 60 || this.verticalStuckTicks >= 40;
   }

   public int getUnstuckAttempts() {
      return Math.max(this.unstuckAttempts, this.miningDownAttempts);
   }

   public boolean isInRecovery() {
      return this.inRecovery || this.recoveryGracePeriod > 0;
   }

   public static double getToolDurabilityPercent(ItemStack tool) {
      if (!tool.isEmpty() && tool.getMaxDamage() != 0) {
         int remainingDurability = tool.getMaxDamage() - tool.getDamage();
         return (double)remainingDurability / (double)tool.getMaxDamage();
      } else {
         return (double)1.0F;
      }
   }

   public static enum MiningMode {
      NORMAL,
      MINING_DOWN,
      MOVING_TO_TARGET;

      // $FF: synthetic method
      private static MiningMode[] $values() {
         return new MiningMode[]{NORMAL, MINING_DOWN, MOVING_TO_TARGET};
      }
   }

   public static enum StuckRecoveryAction {
      NONE,
      JUMP_FORWARD,
      STOP_MOVEMENT,
      PATH_BLOCKED_RTP,
      RETOGGLE_KEYS,
      MOVE_AND_RETRY,
      FIND_NEW_SPOT,
      RECALCULATE_PATH,
      NEEDS_RESET;

      // $FF: synthetic method
      private static StuckRecoveryAction[] $values() {
         return new StuckRecoveryAction[]{NONE, JUMP_FORWARD, STOP_MOVEMENT, PATH_BLOCKED_RTP, RETOGGLE_KEYS, MOVE_AND_RETRY, FIND_NEW_SPOT, RECALCULATE_PATH, NEEDS_RESET};
      }
   }
}
