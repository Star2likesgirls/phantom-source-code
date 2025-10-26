package dev.gambleclient.module.modules.render;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.Render3DEvent;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.BlockUtil;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.RenderUtils;
import java.awt.Color;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public class ClusterFinder extends Module {
   private final NumberSetting rescanTicks = new NumberSetting(EncryptedString.of("RescanTicks"), (double)10.0F, (double)2000.0F, (double)200.0F, (double)10.0F);
   private final NumberSetting chunksPerTick = new NumberSetting(EncryptedString.of("ChunksPerTick"), (double)1.0F, (double)200.0F, (double)32.0F, (double)1.0F);
   private final NumberSetting maxDistance = new NumberSetting(EncryptedString.of("MaxDistance"), (double)8.0F, (double)512.0F, (double)160.0F, (double)8.0F);
   private final BooleanSetting small = new BooleanSetting(EncryptedString.of("Small Buds"), true);
   private final BooleanSetting medium = new BooleanSetting(EncryptedString.of("Medium Buds"), true);
   private final BooleanSetting large = new BooleanSetting(EncryptedString.of("Large Buds"), true);
   private final BooleanSetting clusters = new BooleanSetting(EncryptedString.of("Clusters"), true);
   private final NumberSetting minY = new NumberSetting(EncryptedString.of("MinY"), (double)-64.0F, (double)320.0F, (double)-64.0F, (double)1.0F);
   private final NumberSetting maxY = new NumberSetting(EncryptedString.of("MaxY"), (double)-64.0F, (double)320.0F, (double)128.0F, (double)1.0F);
   private final NumberSetting red = new NumberSetting(EncryptedString.of("Red"), (double)0.0F, (double)255.0F, (double)147.0F, (double)1.0F);
   private final NumberSetting green = new NumberSetting(EncryptedString.of("Green"), (double)0.0F, (double)255.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting blue = new NumberSetting(EncryptedString.of("Blue"), (double)0.0F, (double)255.0F, (double)211.0F, (double)1.0F);
   private final NumberSetting alpha = new NumberSetting(EncryptedString.of("Alpha"), (double)1.0F, (double)255.0F, (double)140.0F, (double)1.0F);
   private final BooleanSetting tracers = new BooleanSetting(EncryptedString.of("Tracers"), true);
   private final BooleanSetting outline = new BooleanSetting(EncryptedString.of("Outline"), true);
   private final BooleanSetting fill = new BooleanSetting(EncryptedString.of("Fill"), false);
   private final BooleanSetting pauseLowFPS = new BooleanSetting(EncryptedString.of("PauseLowFPS"), true);
   private final Set amethyst = ConcurrentHashMap.newKeySet();
   private long lastScanTick = 0L;
   private int chunkScanCursor = 0;

   public ClusterFinder() {
      super(EncryptedString.of("ClusterFinder"), EncryptedString.of("ESP for amethyst buds/clusters"), -1, Category.RENDER);
      this.addSettings(new Setting[]{this.rescanTicks, this.chunksPerTick, this.maxDistance, this.small, this.medium, this.large, this.clusters, this.minY, this.maxY, this.red, this.green, this.blue, this.alpha, this.tracers, this.outline, this.fill, this.pauseLowFPS});
   }

   public void onEnable() {
      this.amethyst.clear();
      this.lastScanTick = 0L;
      this.chunkScanCursor = 0;
      super.onEnable();
   }

   public void onDisable() {
      this.amethyst.clear();
      super.onDisable();
   }

   @EventListener
   public void onTick(TickEvent e) {
      if (this.mc.world != null && this.mc.player != null) {
         long gameTime = this.mc.world.getTime();
         if (gameTime - this.lastScanTick >= (long)this.rescanTicks.getValue()) {
            List<WorldChunk> loaded = BlockUtil.getLoadedChunks().toList();
            if (!loaded.isEmpty()) {
               int perTick = this.chunksPerTick.getIntValue();

               for(int processed = 0; processed < perTick && !loaded.isEmpty(); ++processed) {
                  if (this.chunkScanCursor >= loaded.size()) {
                     this.chunkScanCursor = 0;
                     this.lastScanTick = gameTime;
                     break;
                  }

                  WorldChunk chunk = (WorldChunk)loaded.get(this.chunkScanCursor++);
                  this.scanChunk(chunk);
               }

            }
         }
      }
   }

   private void scanChunk(WorldChunk chunk) {
      if (chunk != null && this.mc.world != null) {
         ChunkPos cp = chunk.getPos();
         int startX = cp.getStartX();
         int startZ = cp.getStartZ();
         int minYI = Math.max(chunk.getBottomY(), (int)this.minY.getValue());
         int maxYI = Math.min(chunk.getBottomY() + chunk.getHeight(), (int)this.maxY.getValue());
         this.amethyst.removeIf((p) -> ((net.minecraft.util.math.BlockPos)p).getX() >> 4 == cp.x && ((net.minecraft.util.math.BlockPos)p).getZ() >> 4 == cp.z);

         for(int x = 0; x < 16; ++x) {
            int wx = startX + x;

            for(int z = 0; z < 16; ++z) {
               int wz = startZ + z;

               for(int y = minYI; y <= maxYI; ++y) {
                  BlockPos pos = new BlockPos(wx, y, wz);
                  BlockState st = chunk.getBlockState(pos);
                  if (this.matches(st)) {
                     this.amethyst.add(pos.toImmutable());
                  }
               }
            }
         }

      }
   }

   private boolean matches(BlockState s) {
      if (s == null) {
         return false;
      } else if (s.isOf(Blocks.SMALL_AMETHYST_BUD) && this.small.getValue()) {
         return true;
      } else if (s.isOf(Blocks.MEDIUM_AMETHYST_BUD) && this.medium.getValue()) {
         return true;
      } else if (s.isOf(Blocks.LARGE_AMETHYST_BUD) && this.large.getValue()) {
         return true;
      } else {
         return s.isOf(Blocks.AMETHYST_CLUSTER) && this.clusters.getValue();
      }
   }

   @EventListener
   public void onRender3D(Render3DEvent event) {
      if (this.mc.player != null && this.mc.world != null) {
         if (!this.amethyst.isEmpty()) {
            if (!this.pauseLowFPS.getValue() || this.mc.getCurrentFps() >= 5) {
               Camera cam = RenderUtils.getCamera();
               MatrixStack matrices = event.matrixStack;
               if (cam != null) {
                  Vec3d camPos = RenderUtils.getCameraPos();
                  matrices.push();
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cam.getYaw() + 180.0F));
                  matrices.translate(-camPos.x, -camPos.y, -camPos.z);
               }

               Color lineColor = new Color((int)this.red.getValue(), (int)this.green.getValue(), (int)this.blue.getValue(), 255);
               Color fillColor = new Color((int)this.red.getValue(), (int)this.green.getValue(), (int)this.blue.getValue(), (int)this.alpha.getValue());
               Vec3d eye = this.mc.player.getEyePos();
               double maxDistSq = this.maxDistance.getValue() * this.maxDistance.getValue();
               BlockPos[] snapshot = (BlockPos[])this.amethyst.toArray(new BlockPos[0]);

               for(BlockPos p : snapshot) {
                  if (p != null) {
                     double distSq = this.mc.player.squaredDistanceTo((double)p.getX() + (double)0.5F, (double)p.getY() + (double)0.5F, (double)p.getZ() + (double)0.5F);
                     if (!(distSq > maxDistSq)) {
                        if (this.fill.getValue()) {
                           RenderUtils.renderFilledBox(matrices, (float)p.getX(), (float)p.getY(), (float)p.getZ(), (float)(p.getX() + 1), (float)(p.getY() + 1), (float)(p.getZ() + 1), fillColor);
                        }

                        if (this.outline.getValue()) {
                           this.drawWireCube(matrices, p, lineColor);
                        }

                        if (this.tracers.getValue()) {
                           RenderUtils.renderLine(matrices, lineColor, eye, Vec3d.ofCenter(p));
                        }
                     }
                  }
               }

               if (cam != null) {
                  matrices.pop();
               }

            }
         }
      }
   }

   private void drawWireCube(MatrixStack matrices, BlockPos pos, Color c) {
      double x1 = (double)pos.getX();
      double y1 = (double)pos.getY();
      double z1 = (double)pos.getZ();
      double x2 = x1 + (double)1.0F;
      double y2 = y1 + (double)1.0F;
      double z2 = z1 + (double)1.0F;
      Vec3d A = new Vec3d(x1, y1, z1);
      Vec3d B = new Vec3d(x2, y1, z1);
      Vec3d C = new Vec3d(x2, y1, z2);
      Vec3d D = new Vec3d(x1, y1, z2);
      Vec3d E = new Vec3d(x1, y2, z1);
      Vec3d F = new Vec3d(x2, y2, z1);
      Vec3d G = new Vec3d(x2, y2, z2);
      Vec3d H = new Vec3d(x1, y2, z2);
      RenderUtils.renderLine(matrices, c, A, B);
      RenderUtils.renderLine(matrices, c, B, C);
      RenderUtils.renderLine(matrices, c, C, D);
      RenderUtils.renderLine(matrices, c, D, A);
      RenderUtils.renderLine(matrices, c, E, F);
      RenderUtils.renderLine(matrices, c, F, G);
      RenderUtils.renderLine(matrices, c, G, H);
      RenderUtils.renderLine(matrices, c, H, E);
      RenderUtils.renderLine(matrices, c, A, E);
      RenderUtils.renderLine(matrices, c, B, F);
      RenderUtils.renderLine(matrices, c, C, G);
      RenderUtils.renderLine(matrices, c, D, H);
   }

   public String getInfo() {
      return String.valueOf(this.amethyst.size());
   }
}
