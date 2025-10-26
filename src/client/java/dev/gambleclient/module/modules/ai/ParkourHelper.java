package dev.gambleclient.module.modules.ai;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;

public class ParkourHelper {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private boolean isJumping = false;
   private int jumpCooldown = 0;
   private BlockPos jumpTarget = null;
   private Vec3d jumpStartPos = null;
   private static final int JUMP_COOLDOWN_TICKS = 10;
   private static final double JUMP_DISTANCE_THRESHOLD = (double)1.5F;

   public JumpCheck checkJumpOpportunity(PlayerEntity player, Direction movingDirection) {
      if (this.jumpCooldown <= 0 && player.isOnGround() && !this.isJumping) {
         World world = this.mc.world;
         BlockPos playerPos = player.getBlockPos();
         BlockPos frontPos = playerPos.offset(movingDirection);
         BlockPos frontGround = frontPos.down();
         BlockState frontState = world.getBlockState(frontPos);
         BlockState frontGroundState = world.getBlockState(frontGround);
         if (frontState.isAir() && !frontGroundState.isAir() && frontGroundState.isSolidBlock(world, frontGround)) {
            BlockPos twoDown = frontGround.down();
            if (world.getBlockState(twoDown).isAir()) {
               return new JumpCheck(true, false, (BlockPos)null, "Safe 1-block drop - just walk");
            }
         }

         if (!frontState.isAir() && frontState.isSolidBlock(world, frontPos)) {
            BlockPos aboveFront = frontPos.up();
            BlockState aboveFrontState = world.getBlockState(aboveFront);
            BlockPos twoAboveFront = frontPos.up(2);
            BlockState twoAboveFrontState = world.getBlockState(twoAboveFront);
            if (aboveFrontState.isAir() && twoAboveFrontState.isAir()) {
               BlockPos landingPos = frontPos.offset(movingDirection);
               BlockState landingState = world.getBlockState(landingPos);
               BlockPos landingGround = landingPos.down();
               BlockState landingGroundState = world.getBlockState(landingGround);
               boolean safeLanding = landingState.isAir() && world.getBlockState(landingPos.up()).isAir();
               boolean hasLandingGround = landingGroundState.isSolidBlock(world, landingGround);
               if (!hasLandingGround) {
                  BlockPos landingTwoDown = landingGround.down();
                  hasLandingGround = world.getBlockState(landingTwoDown).isSolidBlock(world, landingTwoDown);
               }

               if (safeLanding && hasLandingGround) {
                  return new JumpCheck(true, true, landingPos, "Safe jump available (may include drop)");
               } else {
                  return aboveFrontState.isAir() && world.getBlockState(aboveFront.up()).isAir() ? new JumpCheck(true, true, aboveFront, "Jump onto block") : new JumpCheck(false, false, (BlockPos)null, "No safe landing");
               }
            } else {
               return new JumpCheck(false, false, (BlockPos)null, "No clearance above obstacle");
            }
         } else {
            return new JumpCheck(true, false, (BlockPos)null, "No obstacle");
         }
      } else {
         return new JumpCheck(false, false, (BlockPos)null, "On cooldown or already jumping");
      }
   }

   public boolean startJump(BlockPos target) {
      if (!this.isJumping && this.jumpCooldown <= 0) {
         PlayerEntity player = this.mc.player;
         if (player != null && player.isOnGround()) {
            this.jumpTarget = target;
            this.jumpStartPos = player.getPos();
            this.isJumping = true;
            player.jump();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void update() {
      if (this.jumpCooldown > 0) {
         --this.jumpCooldown;
      }

      if (this.isJumping && this.mc.player != null) {
         Vec3d currentPos = this.mc.player.getPos();
         if (this.mc.player.isOnGround() && currentPos.distanceTo(this.jumpStartPos) > (double)0.5F) {
            this.completeJump();
         } else if (currentPos.distanceTo(this.jumpStartPos) > (double)1.5F) {
            this.completeJump();
         }
      }

   }

   private void completeJump() {
      this.isJumping = false;
      this.jumpCooldown = 10;
      this.jumpTarget = null;
      this.jumpStartPos = null;
   }

   public boolean isJumping() {
      return this.isJumping;
   }

   public void reset() {
      this.isJumping = false;
      this.jumpCooldown = 0;
      this.jumpTarget = null;
      this.jumpStartPos = null;
   }

   public static class JumpCheck {
      public final boolean canJump;
      public final boolean shouldJump;
      public final BlockPos targetPos;
      public final String reason;

      public JumpCheck(boolean canJump, boolean shouldJump, BlockPos targetPos, String reason) {
         this.canJump = canJump;
         this.shouldJump = shouldJump;
         this.targetPos = targetPos;
         this.reason = reason;
      }
   }
}
