package dev.gambleclient.module.modules.donut;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.datafixer.fix.ChunkPalettedStorageFix;

public class ChunkFinder extends Module {
   private final BooleanSetting detectDeepslate = new BooleanSetting(EncryptedString.of("Detect Deepslate"), true);
   private final BooleanSetting detectCobbled = new BooleanSetting(EncryptedString.of("Detect Cobbled"), true);
   private final BooleanSetting detectRotated = new BooleanSetting(EncryptedString.of("Detect Rotated"), true);
   private final BooleanSetting detectAirPockets = new BooleanSetting(EncryptedString.of("Detect Air Pockets"), false);
   private final BooleanSetting detectEnclosedOres = new BooleanSetting(EncryptedString.of("Detect Enclosed Ores"), false);
   private final NumberSetting deepslateThreshold = new NumberSetting(EncryptedString.of("Deepslate Thresh"), (double)1.0F, (double)50.0F, (double)3.0F, (double)1.0F);
   private final NumberSetting cobbledThreshold = new NumberSetting(EncryptedString.of("Cobbled Thresh"), (double)1.0F, (double)30.0F, (double)1.0F, (double)1.0F);
   private final NumberSetting rotatedThreshold = new NumberSetting(EncryptedString.of("Rotated Thresh"), (double)1.0F, (double)30.0F, (double)2.0F, (double)1.0F);
   private final NumberSetting airPocketThreshold = new NumberSetting(EncryptedString.of("AirPocket Thresh"), (double)1.0F, (double)16.0F, (double)1.0F, (double)1.0F);
   private final NumberSetting enclosedOreThreshold = new NumberSetting(EncryptedString.of("Encl Ore Thresh"), (double)1.0F, (double)30.0F, (double)2.0F, (double)1.0F);
   private final NumberSetting minScanY = new NumberSetting(EncryptedString.of("Min Scan Y"), (double)-64.0F, (double)320.0F, (double)-20.0F, (double)1.0F);
   private final NumberSetting maxScanY = new NumberSetting(EncryptedString.of("Max Scan Y"), (double)-64.0F, (double)320.0F, (double)25.0F, (double)1.0F);
   private final NumberSetting renderPlateY = new NumberSetting(EncryptedString.of("Render Y"), (double)-64.0F, (double)320.0F, (double)64.0F, (double)1.0F);
   private final NumberSetting plateThickness = new NumberSetting(EncryptedString.of("Plate Thickness"), (double)1.0F, (double)32.0F, (double)3.0F, (double)1.0F);
   private final BooleanSetting drawTracers = new BooleanSetting(EncryptedString.of("Tracers"), false);
   private final BooleanSetting highlightBlocks = new BooleanSetting(EncryptedString.of("Highlight Blocks"), true);
   private final NumberSetting chunkR = new NumberSetting(EncryptedString.of("Chunk R"), (double)0.0F, (double)255.0F, (double)255.0F, (double)1.0F);
   private final NumberSetting chunkG = new NumberSetting(EncryptedString.of("Chunk G"), (double)0.0F, (double)255.0F, (double)215.0F, (double)1.0F);
   private final NumberSetting chunkB = new NumberSetting(EncryptedString.of("Chunk B"), (double)0.0F, (double)255.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting chunkA = new NumberSetting(EncryptedString.of("Chunk A"), (double)0.0F, (double)255.0F, (double)120.0F, (double)1.0F);
   private final NumberSetting deepR = new NumberSetting(EncryptedString.of("Deep R"), (double)0.0F, (double)255.0F, (double)100.0F, (double)1.0F);
   private final NumberSetting deepG = new NumberSetting(EncryptedString.of("Deep G"), (double)0.0F, (double)255.0F, (double)100.0F, (double)1.0F);
   private final NumberSetting deepB = new NumberSetting(EncryptedString.of("Deep B"), (double)0.0F, (double)255.0F, (double)100.0F, (double)1.0F);
   private final NumberSetting cobR = new NumberSetting(EncryptedString.of("Cob R"), (double)0.0F, (double)255.0F, (double)80.0F, (double)1.0F);
   private final NumberSetting cobG = new NumberSetting(EncryptedString.of("Cob G"), (double)0.0F, (double)255.0F, (double)80.0F, (double)1.0F);
   private final NumberSetting cobB = new NumberSetting(EncryptedString.of("Cob B"), (double)0.0F, (double)255.0F, (double)80.0F, (double)1.0F);
   private final NumberSetting rotR = new NumberSetting(EncryptedString.of("Rot R"), (double)0.0F, (double)255.0F, (double)120.0F, (double)1.0F);
   private final NumberSetting rotG = new NumberSetting(EncryptedString.of("Rot G"), (double)0.0F, (double)255.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting rotB = new NumberSetting(EncryptedString.of("Rot B"), (double)0.0F, (double)255.0F, (double)120.0F, (double)1.0F);
   private final NumberSetting airR = new NumberSetting(EncryptedString.of("Air R"), (double)0.0F, (double)255.0F, (double)255.0F, (double)1.0F);
   private final NumberSetting airG = new NumberSetting(EncryptedString.of("Air G"), (double)0.0F, (double)255.0F, (double)255.0F, (double)1.0F);
   private final NumberSetting airB = new NumberSetting(EncryptedString.of("Air B"), (double)0.0F, (double)255.0F, (double)255.0F, (double)1.0F);
   private final NumberSetting oreR = new NumberSetting(EncryptedString.of("Ore R"), (double)0.0F, (double)255.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting oreG = new NumberSetting(EncryptedString.of("Ore G"), (double)0.0F, (double)255.0F, (double)255.0F, (double)1.0F);
   private final NumberSetting oreB = new NumberSetting(EncryptedString.of("Ore B"), (double)0.0F, (double)255.0F, (double)0.0F, (double)1.0F);
   private final BooleanSetting threading = new BooleanSetting(EncryptedString.of("Threading"), true);
   private final NumberSetting threadCount = new NumberSetting(EncryptedString.of("Threads"), (double)1.0F, (double)8.0F, Math.max((double)1.0F, (double)Runtime.getRuntime().availableProcessors() / (double)2.0F), (double)1.0F);
   private final NumberSetting scanDelayMs = new NumberSetting(EncryptedString.of("Scan Delay ms"), (double)10.0F, (double)5000.0F, (double)100.0F, (double)10.0F);
   private final NumberSetting maxConcurrent = new NumberSetting(EncryptedString.of("Max Concurrent"), (double)1.0F, (double)16.0F, (double)3.0F, (double)1.0F);
   private final NumberSetting cleanupSec = new NumberSetting(EncryptedString.of("Cleanup s"), (double)5.0F, (double)600.0F, (double)30.0F, (double)5.0F);
   private final BooleanSetting soundAlert = new BooleanSetting(EncryptedString.of("Sound Alert"), true);
   private final BooleanSetting chatAlert = new BooleanSetting(EncryptedString.of("Chat Alert"), true);
   private final NumberSetting alertsPerMinute = new NumberSetting(EncryptedString.of("Alerts / Min"), (double)1.0F, (double)60.0F, (double)5.0F, (double)1.0F);
   private final Set flagged = ConcurrentHashMap.newKeySet();
   private final Set scanned = ConcurrentHashMap.newKeySet();
   private final Map statsMap = new ConcurrentHashMap();
   private final Map lastNotified = new ConcurrentHashMap();
   private final Queue recentAlerts = new ConcurrentLinkedQueue();
   private final Map suspicious = new ConcurrentHashMap();
   private final AtomicLong activeScans = new AtomicLong(0L);
   private ExecutorService pool;
   private volatile boolean running = false;
   private long lastCleanup = 0L;

   public ChunkFinder() {
      super(EncryptedString.of("ChunkFinder"), EncryptedString.of("Multi-thread suspicious chunk analyzer"), -1, Category.DONUT);
      this.addSettings(new Setting[]{this.detectDeepslate, this.detectCobbled, this.detectRotated, this.detectAirPockets, this.detectEnclosedOres, this.deepslateThreshold, this.cobbledThreshold, this.rotatedThreshold, this.airPocketThreshold, this.enclosedOreThreshold, this.minScanY, this.maxScanY, this.renderPlateY, this.plateThickness, this.drawTracers, this.highlightBlocks, this.chunkR, this.chunkG, this.chunkB, this.chunkA, this.deepR, this.deepG, this.deepB, this.cobR, this.cobG, this.cobB, this.rotR, this.rotG, this.rotB, this.airR, this.airG, this.airB, this.oreR, this.oreG, this.oreB, this.threading, this.threadCount, this.scanDelayMs, this.maxConcurrent, this.cleanupSec, this.soundAlert, this.chatAlert, this.alertsPerMinute});
   }

   public void onEnable() {
      this.clearAll();
      this.running = true;
      this.lastCleanup = System.currentTimeMillis();
      if (this.threading.getValue()) {
         this.pool = Executors.newFixedThreadPool((int)this.threadCount.getValue(), (r) -> {
            Thread t = new Thread(r, "Phantom-ChunkFinder");
            t.setDaemon(true);
            return t;
         });
      }

      this.startInitialScan();
      this.info("ChunkFinder started.");
   }

   public void onDisable() {
      this.running = false;
      if (this.pool != null) {
         this.pool.shutdownNow();
         this.pool = null;
      }

      this.clearAll();
      this.info("ChunkFinder stopped.");
   }

   private void clearAll() {
      this.flagged.clear();
      this.scanned.clear();
      this.statsMap.clear();
      this.lastNotified.clear();
      this.recentAlerts.clear();
      this.suspicious.clear();
      this.activeScans.set(0L);
   }

   @EventListener
   public void onTick(TickEvent e) {
      if (this.running && this.mc.player != null) {
         long now = System.currentTimeMillis();

         while(!this.recentAlerts.isEmpty() && now - (Long)this.recentAlerts.peek() > 60000L) {
            this.recentAlerts.poll();
         }

         if ((double)(now - this.lastCleanup) > this.cleanupSec.getValue() * (double)1000.0F) {
            this.cleanup();
            this.lastCleanup = now;
         }

         if (this.mc.world != null && this.mc.world.getTime() % 40L == 0L) {
            for(Object wcObj : this.approximateLoadedChunks()) {
               WorldChunk wc = (WorldChunk)wcObj;
               if (!this.running) {
                  break;
               }

               this.scheduleScan(wc);
            }
         }

      }
   }

   @EventListener
   public void onRender3D(Render3DEvent event) {
      if (this.running && this.mc.player != null && this.mc.world != null) {
         if (this.mc.getCurrentFps() >= 5 || this.mc.player.age <= 100) {
            Camera cam = RenderUtils.getCamera();
            MatrixStack matrices = event.matrixStack;
            boolean pushed = false;
            if (cam != null) {
               Vec3d camPos = RenderUtils.getCameraPos();
               matrices.push();
               pushed = true;
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cam.getYaw() + 180.0F));
               matrices.translate(-camPos.x, -camPos.y, -camPos.z);
            }

            ChunkPos[] snapshot = (ChunkPos[])this.flagged.toArray(new ChunkPos[0]);
            Color plateColor = new Color((int)this.chunkR.getValue(), (int)this.chunkG.getValue(), (int)this.chunkB.getValue(), (int)this.chunkA.getValue());
            int rendered = 0;
            int cap = 256;

            for(ChunkPos cp : snapshot) {
               if (cp != null) {
                  if (rendered++ >= cap) {
                     break;
                  }

                  this.drawChunkPlateAndOutline(matrices, cp, plateColor);
               }
            }

            if (this.highlightBlocks.getValue()) {
               this.drawSuspiciousBlocks(matrices);
            }

            if (pushed) {
               matrices.pop();
            }

         }
      }
   }

   private void drawChunkPlateAndOutline(MatrixStack matrices, ChunkPos cp, Color c) {
      int startX = cp.getStartX();
      int startZ = cp.getStartZ();
      double y = this.renderPlateY.getValue();
      double h = Math.max(0.05, this.plateThickness.getValue());
      RenderUtils.renderFilledBox(matrices, (float)startX, (float)y, (float)startZ, (float)(startX + 16), (float)(y + h), (float)(startZ + 16), c);
      this.drawChunkOutlineLines(matrices, startX, startZ, y, h, new Color(c.getRed(), c.getGreen(), c.getBlue(), 255));
      if (this.drawTracers.getValue() && this.mc.player != null) {
         Vec3d from = this.mc.player.getEyePos();
         Vec3d to = new Vec3d((double)startX + (double)8.0F, y + h / (double)2.0F, (double)startZ + (double)8.0F);
         RenderUtils.renderLine(matrices, new Color(c.getRed(), c.getGreen(), c.getBlue(), 255), from, to);
      }

   }

   private void drawChunkOutlineLines(MatrixStack matrices, int startX, int startZ, double y, double h, Color c) {
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

   private void drawSuspiciousBlocks(MatrixStack matrices) {
      if (this.mc.player != null) {
         int cap = 750;
         int rendered = 0;
         Map.Entry<BlockPos, Suspicious>[] snap = (Map.Entry[])this.suspicious.entrySet().toArray(new Map.Entry[0]);
         int viewRadius = (Integer)this.mc.options.getViewDistance().getValue() * 16 + 64;
         double maxDistSq = (double)viewRadius * (double)viewRadius;

         for(Map.Entry e : snap) {
            if (rendered >= cap) {
               break;
            }

            BlockPos p = (BlockPos)e.getKey();
            double distSq = this.mc.player.squaredDistanceTo((double)p.getX() + (double)0.5F, (double)p.getY() + (double)0.5F, (double)p.getZ() + (double)0.5F);
            if (!(distSq > maxDistSq)) {
               Color var10000;
               switch (((Suspicious)e.getValue()).type.ordinal()) {
                  case 0 -> var10000 = new Color((int)this.deepR.getValue(), (int)this.deepG.getValue(), (int)this.deepB.getValue(), 200);
                  case 1 -> var10000 = new Color((int)this.cobR.getValue(), (int)this.cobG.getValue(), (int)this.cobB.getValue(), 200);
                  case 2 -> var10000 = new Color((int)this.rotR.getValue(), (int)this.rotG.getValue(), (int)this.rotB.getValue(), 220);
                  case 3 -> var10000 = new Color((int)this.airR.getValue(), (int)this.airG.getValue(), (int)this.airB.getValue(), 160);
                  case 4 -> var10000 = new Color((int)this.oreR.getValue(), (int)this.oreG.getValue(), (int)this.oreB.getValue(), 230);
                  default -> throw new MatchException((String)null, (Throwable)null);
               }

               Color bc = var10000;
               this.drawBlockWireCube(matrices, p, bc);
               ++rendered;
            }
         }

      }
   }

   private void drawBlockWireCube(MatrixStack matrices, BlockPos pos, Color c) {
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

   private void startInitialScan() {
      Runnable r = () -> {
         for(Object wcObj : this.approximateLoadedChunks()) {
            WorldChunk wc = (WorldChunk)wcObj;
            if (!this.running) {
               break;
            }

            this.scheduleScan(wc);

            try {
               Thread.sleep((long)this.scanDelayMs.getValue());
            } catch (InterruptedException var5) {
               Thread.currentThread().interrupt();
               break;
            }
         }

      };
      if (this.threading.getValue() && this.pool != null) {
         this.pool.submit(r);
      } else {
         (new Thread(r, "CF-Initial")).start();
      }

   }

   private void scheduleScan(WorldChunk chunk) {
      if (this.running) {
         ChunkPos cp = chunk.getPos();
         if (!this.scanned.contains(cp)) {
            if (this.activeScans.get() < (long)this.maxConcurrent.getValue()) {
               Runnable task = () -> this.analyzeChunk(chunk);
               if (this.threading.getValue() && this.pool != null) {
                  this.pool.submit(task);
               } else {
                  (new Thread(task, "CF-Scan")).start();
               }

            }
         }
      }
   }

   private void analyzeChunk(WorldChunk chunk) {
      if (this.running && chunk != null) {
         ChunkPos cp = chunk.getPos();
         if (this.scanned.add(cp)) {
            this.activeScans.incrementAndGet();

            try {
               int minY = (int)Math.max((double)chunk.getBottomY(), this.minScanY.getValue());
               int maxY = (int)Math.min((double)(chunk.getBottomY() + chunk.getHeight()), this.maxScanY.getValue());
               Stats s = new Stats();
               this.scanChunkBlocks(chunk, s, minY, maxY);
               this.statsMap.put(cp, s);
               this.evaluateChunk(cp, s);
            } finally {
               this.activeScans.decrementAndGet();
            }

         }
      }
   }

   private void scanChunkBlocks(WorldChunk chunk, Stats s, int minY, int maxY) {
      ChunkPos cp = chunk.getPos();
      int baseX = cp.getStartX();
      int baseZ = cp.getStartZ();

      for(int y = minY; y <= maxY; ++y) {
         for(int x = 0; x < 16; ++x) {
            for(int z = 0; z < 16; ++z) {
               if (!this.running) {
                  return;
               }

               BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
               BlockState state = chunk.getBlockState(pos);
               this.analyzeBlock(pos, state, y, s);
            }
         }
      }

   }

   private void analyzeBlock(BlockPos pos, BlockState state, int worldY, Stats s) {
      SuspiciousType type = null;
      if (this.detectDeepslate.getValue() && state.getBlock() == Blocks.DEEPSLATE && worldY >= 8) {
         ++s.deepslate;
         type = ChunkFinder.SuspiciousType.DEEPSLATE;
      }

      if (this.detectCobbled.getValue() && state.getBlock() == Blocks.COBBLED_DEEPSLATE) {
         ++s.cobbled;
         type = ChunkFinder.SuspiciousType.COBBLED;
      }

      if (this.detectRotated.getValue() && this.isRotated(state)) {
         ++s.rotated;
         type = ChunkFinder.SuspiciousType.ROTATED;
      }

      if (this.activeScans.get() <= 2L) {
         if (this.detectAirPockets.getValue() && this.isAirPocket(pos)) {
            ++s.airPockets;
            type = ChunkFinder.SuspiciousType.AIR_POCKET;
         }

         if (this.detectEnclosedOres.getValue() && this.isEnclosedOre(state.getBlock(), pos)) {
            ++s.enclosedOres;
            type = ChunkFinder.SuspiciousType.ENCLOSED_ORE;
         }
      }

      if (type != null && this.highlightBlocks.getValue()) {
         this.suspicious.put(pos, new Suspicious(type, System.currentTimeMillis()));
      }

   }

   private boolean isRotated(BlockState st) {
      if (!st.contains(Properties.AXIS)) {
         return false;
      } else {
         Direction.Axis axis = (Direction.Axis)st.get(Properties.AXIS);
         if (axis == Direction.Axis.Y) {
            return false;
         } else {
            Block b = st.getBlock();
            return b == Blocks.DEEPSLATE || b == Blocks.POLISHED_DEEPSLATE || b == Blocks.DEEPSLATE_BRICKS || b == Blocks.DEEPSLATE_TILES || b == Blocks.CHISELED_DEEPSLATE;
         }
      }
   }

   private boolean isAirPocket(BlockPos pos) {
      ClientWorld w = this.mc.world;
      if (w == null) {
         return false;
      } else if (w.getBlockState(pos).getBlock() != Blocks.AIR) {
         return false;
      } else if (pos.getY() > 2 && !((double)pos.getY() > this.maxScanY.getValue())) {
         int solid = 0;

         for(Direction d : Direction.values()) {
            BlockState n = w.getBlockState(pos.offset(d));
            if (n.isSolidBlock(w, pos.offset(d))) {
               ++solid;
            }
         }

         if (solid < 6) {
            return false;
         } else {
            for(int dx = -1; dx <= 1; ++dx) {
               for(int dy = -1; dy <= 1; ++dy) {
                  for(int dz = -1; dz <= 1; ++dz) {
                     if ((dx != 0 || dy != 0 || dz != 0) && w.getBlockState(pos.add(dx, dy, dz)).isAir()) {
                        return false;
                     }
                  }
               }
            }

            int natural = 0;
            int total = 0;

            for(int dx = -2; dx <= 2; ++dx) {
               for(int dy = -2; dy <= 2; ++dy) {
                  for(int dz = -2; dz <= 2; ++dz) {
                     if (Math.abs(dx) > 1 || Math.abs(dy) > 1 || Math.abs(dz) > 1) {
                        Block b = w.getBlockState(pos.add(dx, dy, dz)).getBlock();
                        ++total;
                        if (b == Blocks.STONE || b == Blocks.DEEPSLATE || b == Blocks.COBBLESTONE || b == Blocks.COBBLED_DEEPSLATE) {
                           ++natural;
                        }
                     }
                  }
               }
            }

            return total > 0 && natural * 100 / total >= 80;
         }
      } else {
         return false;
      }
   }

   private boolean isEnclosedOre(Block b, BlockPos pos) {
      if (b != Blocks.DIAMOND_ORE && b != Blocks.DEEPSLATE_DIAMOND_ORE && b != Blocks.EMERALD_ORE && b != Blocks.DEEPSLATE_EMERALD_ORE && b != Blocks.GOLD_ORE && b != Blocks.DEEPSLATE_GOLD_ORE && b != Blocks.IRON_ORE && b != Blocks.DEEPSLATE_IRON_ORE && b != Blocks.ANCIENT_DEBRIS) {
         return false;
      } else {
         ClientWorld w = this.mc.world;
         if (w == null) {
            return false;
         } else {
            for(Direction d : Direction.values()) {
               BlockState adj = w.getBlockState(pos.offset(d));
               if (adj.isAir() || !adj.isOpaque()) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   private void evaluateChunk(ChunkPos cp, Stats s) {
      boolean suspiciousChunk = false;
      StringBuilder reasons = new StringBuilder();
      if (this.detectDeepslate.getValue() && (double)s.deepslate >= this.deepslateThreshold.getValue()) {
         suspiciousChunk = true;
         reasons.append("Deepslate[").append(s.deepslate).append("] ");
      }

      if (this.detectCobbled.getValue() && (double)s.cobbled >= this.cobbledThreshold.getValue()) {
         suspiciousChunk = true;
         reasons.append("Cobbled[").append(s.cobbled).append("] ");
      }

      if (this.detectRotated.getValue() && (double)s.rotated >= this.rotatedThreshold.getValue()) {
         suspiciousChunk = true;
         reasons.append("Rotated[").append(s.rotated).append("] ");
      }

      if (this.detectAirPockets.getValue() && (double)s.airPockets >= this.airPocketThreshold.getValue()) {
         suspiciousChunk = true;
         reasons.append("AirPockets[").append(s.airPockets).append("] ");
      }

      if (this.detectEnclosedOres.getValue() && (double)s.enclosedOres >= this.enclosedOreThreshold.getValue()) {
         suspiciousChunk = true;
         reasons.append("EnclosedOres[").append(s.enclosedOres).append("] ");
      }

      if (suspiciousChunk) {
         if (this.flagged.add(cp)) {
            this.notifyChunk(cp, reasons.toString().trim());
         }
      } else {
         this.flagged.remove(cp);
         this.lastNotified.remove(cp);
      }

   }

   private void notifyChunk(ChunkPos cp, String detail) {
      long now = System.currentTimeMillis();
      if (!((double)this.recentAlerts.size() >= this.alertsPerMinute.getValue())) {
         Long prev = (Long)this.lastNotified.get(cp);
         if (prev == null || now - prev >= 45000L) {
            this.mc.execute(() -> {
               if (this.chatAlert.getValue() && this.mc.player != null) {
                  this.mc.player.sendMessage(Text.literal("§6[ChunkFinder] §eSuspicious chunk " + cp.x + "," + cp.z + " " + detail), false);
               }

               if (this.soundAlert.getValue() && this.mc.player != null) {
                  this.mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5F, 1.2F);
               }

               this.recentAlerts.offer(now);
               this.lastNotified.put(cp, now);
            });
         }
      }
   }

   private void cleanup() {
      if (this.mc.player != null) {
         int view = (Integer)this.mc.options.getViewDistance().getValue();
         int px = (int)this.mc.player.getX() >> 4;
         int pz = (int)this.mc.player.getZ() >> 4;
         this.flagged.removeIf((cp) -> {
            int dx = Math.abs(((net.minecraft.util.math.ChunkPos)cp).x - px);
            int dz = Math.abs(((net.minecraft.util.math.ChunkPos)cp).z - pz);
            boolean far = dx > view + 6 || dz > view + 6;
            if (far) {
               this.statsMap.remove(cp);
               this.lastNotified.remove(cp);
            }

            return far;
         });
         this.scanned.removeIf((cp) -> {
            int dx = Math.abs(((net.minecraft.util.math.ChunkPos)cp).x - px);
            int dz = Math.abs(((net.minecraft.util.math.ChunkPos)cp).z - pz);
            return dx > view + 4 || dz > view + 4;
         });
         this.suspicious.entrySet().removeIf((e) -> this.mc.player.getPos().distanceTo(Vec3d.ofCenter((Vec3i)((java.util.Map.Entry)e).getKey())) > (double)(view * 16 + 96));
      }
   }

   private List approximateLoadedChunks() {
      ClientWorld world = this.mc.world;
      ClientPlayerEntity player = this.mc.player;
      if (world != null && player != null) {
         int vd = (Integer)this.mc.options.getViewDistance().getValue();
         int pcx = (int)player.getX() >> 4;
         int pcz = (int)player.getZ() >> 4;
         List<WorldChunk> list = new ArrayList();

         for(int dx = -vd - 1; dx <= vd + 1; ++dx) {
            for(int dz = -vd - 1; dz <= vd + 1; ++dz) {
               WorldChunk wc = world.getChunkManager().getWorldChunk(pcx + dx, pcz + dz);
               if (wc != null) {
                  list.add(wc);
               }
            }
         }

         return list;
      } else {
         return List.of();
      }
   }

   public String getInfo() {
      return this.highlightBlocks.getValue() ? "C:" + this.flagged.size() + " B:" + this.suspicious.size() : String.valueOf(this.flagged.size());
   }

   private void info(String s) {
      if (this.mc.player != null) {
         this.mc.player.sendMessage(Text.literal("§a[CF] " + s), false);
      }

   }

   private void warning(String s) {
      if (this.mc.player != null) {
         this.mc.player.sendMessage(Text.literal("§6[CF] " + s), false);
      }

   }

   private void error(String s) {
      if (this.mc.player != null) {
         this.mc.player.sendMessage(Text.literal("§c[CF] " + s), false);
      }

   }

   private static class Stats {
      int deepslate;
      int cobbled;
      int rotated;
      int airPockets;
      int enclosedOres;
   }

   private static record Suspicious(SuspiciousType type, long time) {
   }

   private static enum SuspiciousType {
      DEEPSLATE,
      COBBLED,
      ROTATED,
      AIR_POCKET,
      ENCLOSED_ORE;

      // $FF: synthetic method
      private static SuspiciousType[] $values() {
         return new SuspiciousType[]{DEEPSLATE, COBBLED, ROTATED, AIR_POCKET, ENCLOSED_ORE};
      }
   }
}
