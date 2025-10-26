package dev.gambleclient.module.modules.render;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.Render3DEvent;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.RenderUtils;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.KelpBlock;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.IntProperty;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public final class KelpESP extends Module {
   private final BooleanSetting requireAge = (new BooleanSetting(EncryptedString.of("RequireAge25"), true)).setDescription(EncryptedString.of("Column top kelp must have AGE >= 25"));
   private final BooleanSetting useMinHeight = new BooleanSetting(EncryptedString.of("UseHeight"), true);
   private final NumberSetting minHeight = new NumberSetting(EncryptedString.of("MinHeight"), (double)1.0F, (double)64.0F, (double)12.0F, (double)1.0F);
   private final NumberSetting neededColumns = (NumberSetting)(new NumberSetting(EncryptedString.of("NeededColumns"), (double)1.0F, (double)64.0F, (double)2.0F, (double)1.0F)).setDescription(EncryptedString.of("Kelp columns required to mark chunk"));
   private final NumberSetting chunkRadius = new NumberSetting(EncryptedString.of("ChunkRadius"), (double)1.0F, (double)24.0F, (double)8.0F, (double)1.0F);
   private final NumberSetting minYScan = new NumberSetting(EncryptedString.of("MinYScan"), (double)-64.0F, (double)320.0F, (double)30.0F, (double)1.0F);
   private final NumberSetting maxYScan = new NumberSetting(EncryptedString.of("MaxYScan"), (double)-64.0F, (double)320.0F, (double)80.0F, (double)1.0F);
   private final NumberSetting rescanDelay = new NumberSetting(EncryptedString.of("RescanDelayTicks"), (double)20.0F, (double)2000.0F, (double)300.0F, (double)20.0F);
   private final NumberSetting chunksPerTick = new NumberSetting(EncryptedString.of("ChunksPerTick"), (double)1.0F, (double)200.0F, (double)18.0F, (double)1.0F);
   private final NumberSetting plateY = new NumberSetting(EncryptedString.of("PlateY"), (double)-64.0F, (double)320.0F, (double)62.0F, (double)1.0F);
   private final NumberSetting plateThickness = new NumberSetting(EncryptedString.of("PlateThickness"), 0.01, (double)2.0F, 0.12, 0.01);
   private final BooleanSetting fill = new BooleanSetting(EncryptedString.of("Fill"), true);
   private final BooleanSetting outline = new BooleanSetting(EncryptedString.of("Outline"), true);
   private final BooleanSetting tracers = new BooleanSetting(EncryptedString.of("Tracers"), true);
   private final NumberSetting baseR = new NumberSetting(EncryptedString.of("BaseR"), (double)0.0F, (double)255.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting baseG = new NumberSetting(EncryptedString.of("BaseG"), (double)0.0F, (double)255.0F, (double)200.0F, (double)1.0F);
   private final NumberSetting baseB = new NumberSetting(EncryptedString.of("BaseB"), (double)0.0F, (double)255.0F, (double)120.0F, (double)1.0F);
   private final NumberSetting baseA = new NumberSetting(EncryptedString.of("BaseA"), (double)0.0F, (double)255.0F, (double)90.0F, (double)1.0F);
   private final BooleanSetting gradient = (new BooleanSetting(EncryptedString.of("Gradient"), true)).setDescription(EncryptedString.of("Brighten color as columns approach requirement"));
   private final NumberSetting maxBoost = new NumberSetting(EncryptedString.of("MaxBoost"), (double)0.0F, (double)255.0F, (double)110.0F, (double)5.0F);
   private final BooleanSetting notifyChat = new BooleanSetting(EncryptedString.of("ChatNotify"), true);
   private final BooleanSetting notifySound = new BooleanSetting(EncryptedString.of("Sound"), true);
   private static final IntProperty AGE_PROP;
   private long tickCounter;
   private final Map chunkCache = new HashMap();
   private final Deque workQueue = new ArrayDeque();

   public KelpESP() {
      super(EncryptedString.of("KelpChunkESP"), EncryptedString.of("Highlights chunks with fully grown kelp"), -1, Category.RENDER);
      this.addSettings(new Setting[]{this.requireAge, this.useMinHeight, this.minHeight, this.neededColumns, this.chunkRadius, this.minYScan, this.maxYScan, this.rescanDelay, this.chunksPerTick, this.plateY, this.plateThickness, this.fill, this.outline, this.tracers, this.baseR, this.baseG, this.baseB, this.baseA, this.gradient, this.maxBoost, this.notifyChat, this.notifySound});
   }

   public void onEnable() {
      super.onEnable();
      this.chunkCache.clear();
      this.workQueue.clear();
      this.tickCounter = 0L;
   }

   public void onDisable() {
      this.chunkCache.clear();
      this.workQueue.clear();
      super.onDisable();
   }

   @EventListener
   private void onTick(TickEvent e) {
      if (this.mc != null && this.mc.world != null && this.mc.player != null) {
         ++this.tickCounter;
         if (this.tickCounter % 25L == 0L || this.workQueue.isEmpty()) {
            this.rebuildWorkQueue();
         }

         int perTick = Math.max(1, this.chunksPerTick.getIntValue());

         for(int i = 0; i < perTick && !this.workQueue.isEmpty(); ++i) {
            ChunkPos cp = (ChunkPos)this.workQueue.pollFirst();
            if (cp != null) {
               this.scanChunk(cp);
            }
         }

      }
   }

   private void rebuildWorkQueue() {
      this.workQueue.clear();
      if (this.mc.player != null) {
         ChunkPos center = new ChunkPos(this.mc.player.getBlockPos());
         int radius = this.chunkRadius.getIntValue();
         long delay = (long)this.rescanDelay.getIntValue();

         for(int dx = -radius; dx <= radius; ++dx) {
            for(int dz = -radius; dz <= radius; ++dz) {
               ChunkPos cp = new ChunkPos(center.x + dx, center.z + dz);
               long key = cp.toLong();
               ChunkInfo info = (ChunkInfo)this.chunkCache.get(key);
               if (info == null || this.tickCounter - info.lastScanTick >= delay) {
                  this.workQueue.addLast(cp);
               }
            }
         }

      }
   }

   private void scanChunk(ChunkPos cp) {
      if (this.mc.world != null) {
         WorldChunk chunk = this.mc.world.getChunkManager().getWorldChunk(cp.x, cp.z);
         if (chunk != null) {
            int minYv = (int)Math.min(this.minYScan.getValue(), this.maxYScan.getValue());
            int maxYv = (int)Math.max(this.minYScan.getValue(), this.maxYScan.getValue());
            minYv = Math.max(minYv, chunk.getBottomY());
            maxYv = Math.min(maxYv, chunk.getBottomY() + chunk.getHeight() - 1);
            int needed = this.neededColumns.getIntValue();
            int found = 0;
            BlockPos.Mutable m = new BlockPos.Mutable();

            for(int lx = 0; lx < 16 && found < needed; ++lx) {
               int wx = cp.getStartX() + lx;

               for(int lz = 0; lz < 16 && found < needed; ++lz) {
                  int wz = cp.getStartZ() + lz;
                  boolean countedThisXZ = false;

                  for(int y = minYv; y <= maxYv && !countedThisXZ; ++y) {
                     m.set(wx, y, wz);
                     BlockState state = this.mc.world.getBlockState(m);
                     if (state.getBlock() instanceof KelpBlock) {
                        BlockPos.Mutable base = (new BlockPos.Mutable()).set(m);

                        while(base.getY() > minYv) {
                           BlockState below = this.mc.world.getBlockState(base.down());
                           if (!(below.getBlock() instanceof KelpBlock)) {
                              break;
                           }

                           base.move(0, -1, 0);
                        }

                        if (this.mc.world.getBlockState(base).getBlock() instanceof KelpBlock) {
                           int height = 0;
                           BlockState topState = null;
                           BlockPos.Mutable climb = (new BlockPos.Mutable()).set(base);

                           while(climb.getY() <= maxYv) {
                              BlockState cs = this.mc.world.getBlockState(climb);
                              if (!(cs.getBlock() instanceof KelpBlock)) {
                                 break;
                              }

                              ++height;
                              topState = cs;
                              if (height > 128) {
                                 break;
                              }

                              climb.move(0, 1, 0);
                           }

                           if (height != 0 && this.columnQualifies(topState, height)) {
                              ++found;
                              countedThisXZ = true;
                           }
                        }
                     }
                  }
               }
            }

            ChunkInfo info = (ChunkInfo)this.chunkCache.computeIfAbsent(cp.toLong(), (k) -> new ChunkInfo());
            info.columnCount = found;
            info.lastScanTick = this.tickCounter;
            if (found >= needed) {
               if (!info.notified) {
                  if (this.notifyChat.getValue() && this.mc.player != null) {
                     this.mc.player.sendMessage(Text.literal("[KelpChunkESP] Chunk " + cp.x + "," + cp.z + " kelp=" + found), false);
                  }

                  if (this.notifySound.getValue() && this.mc.player != null) {
                     try {
                        this.mc.player.playSound((SoundEvent)SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.0F, 1.0F);
                     } catch (Throwable var20) {
                     }
                  }

                  info.notified = true;
               }
            } else {
               info.notified = false;
            }

         }
      }
   }

   private boolean columnQualifies(BlockState topState, int height) {
      boolean passAge = !this.requireAge.getValue();
      if (this.requireAge.getValue()) {
         if (topState != null && topState.contains(AGE_PROP)) {
            passAge = (Integer)topState.get(AGE_PROP) >= 25;
         } else {
            passAge = false;
         }
      }

      boolean passHeight = !this.useMinHeight.getValue() || height >= this.minHeight.getIntValue();
      return passAge && passHeight;
   }

   @EventListener
   private void onRender3D(Render3DEvent event) {
      if (this.mc != null && this.mc.world != null && this.mc.player != null) {
         if (!this.chunkCache.isEmpty()) {
            Camera cam = RenderUtils.getCamera();
            MatrixStack matrices = event.matrixStack;
            if (cam != null) {
               Vec3d camPos = RenderUtils.getCameraPos();
               matrices.push();
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cam.getYaw() + 180.0F));
               matrices.translate(-camPos.x, -camPos.y, -camPos.z);
            }

            int needed = this.neededColumns.getIntValue();
            double y = this.plateY.getValue();
            double h = this.plateThickness.getValue();

            for(Object entryObj : this.chunkCache.entrySet()) {
               Map.Entry<?, ?> entry = (Map.Entry<?, ?>)entryObj;
               ChunkInfo info = (ChunkInfo)entry.getValue();
               if (info.columnCount >= needed) {
                  ChunkPos cp = new ChunkPos((Long)entry.getKey());
                  int startX = cp.getStartX();
                  int startZ = cp.getStartZ();
                  int r = (int)this.baseR.getValue();
                  int g = (int)this.baseG.getValue();
                  int b = (int)this.baseB.getValue();
                  int a = (int)this.baseA.getValue();
                  if (this.gradient.getValue()) {
                     double ratio = Math.min((double)1.0F, (double)info.columnCount / (double)needed);
                     int boost = (int)Math.min((double)255.0F, ratio * this.maxBoost.getValue());
                     r = this.clamp(r + boost);
                     g = this.clamp(g + boost / 2);
                     b = this.clamp(b + boost / 3);
                  }

                  Color fillColor = new Color(r, g, b, a);
                  Color lineColor = new Color(r, g, b, 255);
                  if (this.fill.getValue()) {
                     RenderUtils.renderFilledBox(matrices, (float)startX, (float)y, (float)startZ, (float)(startX + 16), (float)(y + h), (float)(startZ + 16), fillColor);
                  }

                  if (this.outline.getValue()) {
                     this.drawChunkOutline(matrices, startX, startZ, y, h, lineColor);
                  }

                  if (this.tracers.getValue()) {
                     Vec3d from = this.mc.crosshairTarget != null ? this.mc.crosshairTarget.getPos() : this.mc.player.getEyePos();
                     Vec3d to = new Vec3d((double)startX + (double)8.0F, y + h / (double)2.0F, (double)startZ + (double)8.0F);
                     RenderUtils.renderLine(matrices, lineColor, from, to);
                  }
               }
            }

            if (cam != null) {
               matrices.pop();
            }

         }
      }
   }

   private void drawChunkOutline(MatrixStack matrices, int startX, int startZ, double y, double h, Color c) {
      double x1 = (double)startX;
      double z1 = (double)startZ;
      double x2 = (double)(startX + 16);
      double z2 = (double)(startZ + 16);
      double y2 = y + h;
      Vec3d v000 = new Vec3d(x1, y, z1);
      Vec3d v001 = new Vec3d(x1, y, z2);
      Vec3d v010 = new Vec3d(x1, y2, z1);
      Vec3d v011 = new Vec3d(x1, y2, z2);
      Vec3d v100 = new Vec3d(x2, y, z1);
      Vec3d v101 = new Vec3d(x2, y, z2);
      Vec3d v110 = new Vec3d(x2, y2, z1);
      Vec3d v111 = new Vec3d(x2, y2, z2);
      RenderUtils.renderLine(matrices, c, v000, v100);
      RenderUtils.renderLine(matrices, c, v100, v101);
      RenderUtils.renderLine(matrices, c, v101, v001);
      RenderUtils.renderLine(matrices, c, v001, v000);
      RenderUtils.renderLine(matrices, c, v010, v110);
      RenderUtils.renderLine(matrices, c, v110, v111);
      RenderUtils.renderLine(matrices, c, v111, v011);
      RenderUtils.renderLine(matrices, c, v011, v010);
      RenderUtils.renderLine(matrices, c, v000, v010);
      RenderUtils.renderLine(matrices, c, v100, v110);
      RenderUtils.renderLine(matrices, c, v101, v111);
      RenderUtils.renderLine(matrices, c, v001, v011);
   }

   private int clamp(int v) {
      return Math.max(0, Math.min(255, v));
   }

   public void handleBlockUpdate(BlockPos pos) {
      ChunkPos cp = new ChunkPos(pos);
      ChunkInfo info = (ChunkInfo)this.chunkCache.get(cp.toLong());
      if (info != null) {
         info.lastScanTick = 0L;
      }

      if (!this.workQueue.contains(cp)) {
         this.workQueue.addLast(cp);
      }

   }

   public void handleChunkLoad(ChunkPos cp) {
      ChunkInfo info = (ChunkInfo)this.chunkCache.get(cp.toLong());
      if (info != null) {
         info.lastScanTick = 0L;
      }

      if (!this.workQueue.contains(cp)) {
         this.workQueue.addLast(cp);
      }

   }

   public void handleChunkUnload(ChunkPos cp) {
      this.chunkCache.remove(cp.toLong());
      this.workQueue.remove(cp);
   }

   public int getColumnCount(ChunkPos cp) {
      ChunkInfo info = (ChunkInfo)this.chunkCache.get(cp.toLong());
      return info == null ? 0 : info.columnCount;
   }

   static {
      AGE_PROP = KelpBlock.AGE;
   }

   private static final class ChunkInfo {
      int columnCount;
      long lastScanTick;
      boolean notified;
   }
}
