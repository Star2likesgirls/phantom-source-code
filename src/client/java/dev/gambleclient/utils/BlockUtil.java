package dev.gambleclient.utils;

import dev.gambleclient.Gamble;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.block.RespawnAnchorBlock;

public final class BlockUtil {
   public static Stream getLoadedChunks() {
      int radius = Math.max(2, Gamble.mc.options.getClampedViewDistance()) + 3;
      int diameter = radius * 2 + 1;
      ChunkPos center = Gamble.mc.player.getChunkPos();
      ChunkPos min = new ChunkPos(center.x - radius, center.z - radius);
      ChunkPos max = new ChunkPos(center.x + radius, center.z + radius);
      return Stream.iterate(min, (pos) -> {
         int x = pos.x;
         int z = pos.z;
         ++x;
         if (x > max.x) {
            x = min.x;
            ++z;
         }

         if (z > max.z) {
            throw new IllegalStateException("Stream limit didn't work.");
         } else {
            return new ChunkPos(x, z);
         }
      }).limit((long)diameter * (long)diameter).filter((c) -> Gamble.mc.world.isChunkLoaded(c.x, c.z)).map((c) -> Gamble.mc.world.getChunk(c.x, c.z)).filter(Objects::nonNull);
   }

   public static boolean isBlockAtPosition(BlockPos blockPos, Block block) {
      return Gamble.mc.world.getBlockState(blockPos).getBlock() == block;
   }

   public static boolean isRespawnAnchorCharged(BlockPos blockPos) {
      return isBlockAtPosition(blockPos, Blocks.RESPAWN_ANCHOR) && (Integer)Gamble.mc.world.getBlockState(blockPos).get(RespawnAnchorBlock.CHARGES) != 0;
   }

   public static boolean isRespawnAnchorUncharged(BlockPos blockPos) {
      return isBlockAtPosition(blockPos, Blocks.RESPAWN_ANCHOR) && (Integer)Gamble.mc.world.getBlockState(blockPos).get(RespawnAnchorBlock.CHARGES) == 0;
   }

   public static void interactWithBlock(BlockHitResult blockHitResult, boolean shouldSwingHand) {
      ActionResult result = Gamble.mc.interactionManager.interactBlock(Gamble.mc.player, Hand.MAIN_HAND, blockHitResult);
      if (result.isAccepted() && result.shouldSwingHand() && shouldSwingHand) {
         Gamble.mc.player.swingHand(Hand.MAIN_HAND);
      }

   }
}
