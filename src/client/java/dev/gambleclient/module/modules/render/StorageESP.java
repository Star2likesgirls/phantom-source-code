package dev.gambleclient.module.modules.render;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.Render3DEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.BlockUtil;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.RenderUtils;
import java.awt.Color;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public final class StorageESP extends Module {
   private final NumberSetting alpha = new NumberSetting(EncryptedString.of("Alpha"), (double)1.0F, (double)255.0F, (double)125.0F, (double)1.0F);
   private final BooleanSetting tracers = (new BooleanSetting(EncryptedString.of("Tracers"), false)).setDescription(EncryptedString.of("Draws a line from your player to the storage block"));
   private final BooleanSetting chests = new BooleanSetting(EncryptedString.of("Chests"), true);
   private final BooleanSetting enderChests = new BooleanSetting(EncryptedString.of("Ender Chests"), true);
   private final BooleanSetting spawners = new BooleanSetting(EncryptedString.of("Spawners"), true);
   private final BooleanSetting shulkerBoxes = new BooleanSetting(EncryptedString.of("Shulker Boxes"), true);
   private final BooleanSetting furnaces = new BooleanSetting(EncryptedString.of("Furnaces"), true);
   private final BooleanSetting barrels = new BooleanSetting(EncryptedString.of("Barrels"), true);
   private final BooleanSetting enchant = new BooleanSetting(EncryptedString.of("Enchanting Tables"), true);
   private final BooleanSetting pistons = new BooleanSetting(EncryptedString.of("Pistons"), true);

   public StorageESP() {
      super(EncryptedString.of("Storage ESP"), EncryptedString.of("Renders storage blocks through walls"), -1, Category.RENDER);
      this.addSettings(new Setting[]{this.alpha, this.tracers, this.chests, this.enderChests, this.spawners, this.shulkerBoxes, this.furnaces, this.barrels, this.enchant, this.pistons});
   }

   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onRender3D(Render3DEvent render3DEvent) {
      this.renderStorages(render3DEvent);
   }

   private Color getBlockEntityColor(BlockEntity blockEntity, int a) {
      if (blockEntity instanceof TrappedChestBlockEntity && this.chests.getValue()) {
         return new Color(200, 91, 0, a);
      } else if (blockEntity instanceof ChestBlockEntity && this.chests.getValue()) {
         return new Color(156, 91, 0, a);
      } else if (blockEntity instanceof EnderChestBlockEntity && this.enderChests.getValue()) {
         return new Color(117, 0, 255, a);
      } else if (blockEntity instanceof MobSpawnerBlockEntity && this.spawners.getValue()) {
         return new Color(138, 126, 166, a);
      } else if (blockEntity instanceof ShulkerBoxBlockEntity && this.shulkerBoxes.getValue()) {
         return new Color(134, 0, 158, a);
      } else if (blockEntity instanceof FurnaceBlockEntity && this.furnaces.getValue()) {
         return new Color(125, 125, 125, a);
      } else if (blockEntity instanceof BarrelBlockEntity && this.barrels.getValue()) {
         return new Color(255, 140, 140, a);
      } else if (blockEntity instanceof EnchantingTableBlockEntity && this.enchant.getValue()) {
         return new Color(80, 80, 255, a);
      } else {
         return blockEntity instanceof PistonBlockEntity && this.pistons.getValue() ? new Color(35, 226, 0, a) : new Color(255, 255, 255, 0);
      }
   }

   private void renderStorages(Render3DEvent event) {
      Camera cam = RenderUtils.getCamera();
      if (cam != null) {
         Vec3d camPos = RenderUtils.getCameraPos();
         MatrixStack matrices = event.matrixStack;
         matrices.push();
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cam.getYaw() + 180.0F));
         matrices.translate(-camPos.x, -camPos.y, -camPos.z);
      }

      for(Object chunkObj : BlockUtil.getLoadedChunks().toList()) {
         net.minecraft.world.chunk.WorldChunk chunk = (net.minecraft.world.chunk.WorldChunk)chunkObj;
         for(BlockPos blockPos : chunk.getBlockEntityPositions()) {
            BlockEntity blockEntity = this.mc.world.getBlockEntity(blockPos);
            RenderUtils.renderFilledBox(event.matrixStack, (float)blockPos.getX() + 0.1F, (float)blockPos.getY() + 0.05F, (float)blockPos.getZ() + 0.1F, (float)blockPos.getX() + 0.9F, (float)blockPos.getY() + 0.85F, (float)blockPos.getZ() + 0.9F, this.getBlockEntityColor(blockEntity, this.alpha.getIntValue()));
            if (this.tracers.getValue()) {
               RenderUtils.renderLine(event.matrixStack, this.getBlockEntityColor(blockEntity, 255), this.mc.crosshairTarget.getPos(), new Vec3d((double)blockPos.getX() + (double)0.5F, (double)blockPos.getY() + (double)0.5F, (double)blockPos.getZ() + (double)0.5F));
            }
         }
      }

      MatrixStack matrixStack = event.matrixStack;
      matrixStack.pop();
   }
}
