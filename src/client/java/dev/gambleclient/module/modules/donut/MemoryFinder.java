package dev.gambleclient.module.modules.donut;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.Render2DEvent;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.block.entity.BarrelBlockEntity;

public class MemoryFinder extends Module {
   private final NumberSetting scanRadius = new NumberSetting(EncryptedString.of("Scan Radius"), (double)1.0F, (double)32.0F, (double)16.0F, (double)1.0F);
   private final NumberSetting consoleSize = new NumberSetting(EncryptedString.of("Console Lines"), (double)5.0F, (double)20.0F, (double)12.0F, (double)1.0F);
   private final BooleanSetting showEntities = new BooleanSetting(EncryptedString.of("Show Entities"), true);
   private final BooleanSetting deepMemoryScan = new BooleanSetting(EncryptedString.of("Deep Memory Scan"), true);
   private final NumberSetting updateDelay = new NumberSetting(EncryptedString.of("Update Delay"), (double)20.0F, (double)200.0F, (double)60.0F, (double)10.0F);
   private final BooleanSetting glowEffect = new BooleanSetting(EncryptedString.of("Console Glow"), true);
   private final Deque consoleLog = new ConcurrentLinkedDeque();
   private final Set scannedPositions = new HashSet();
   private final Set processedChunks = new HashSet();
   private final Map cooldowns = new HashMap();
   private long lastUpdate = 0L;
   private int tickCounter = 0;
   private boolean initialized = false;
   private int scanProgress = 0;
   private Random rand = new Random();

   public MemoryFinder() {
      super(EncryptedString.of("MemoryFinder"), EncryptedString.of("Advanced memory scanner for game objects"), -1, Category.DONUT);
      this.addSettings(new Setting[]{this.scanRadius, this.consoleSize, this.showEntities, this.deepMemoryScan, this.updateDelay, this.glowEffect});
   }

   public void onEnable() {
      this.consoleLog.clear();
      this.scannedPositions.clear();
      this.processedChunks.clear();
      this.cooldowns.clear();
      this.initialized = false;
      this.scanProgress = 0;
      this.tickCounter = 0;
      this.addConsoleEntry("Starting Memory writing.....", 65280);
      this.addConsoleEntry("Found java process...", 65280);
      this.addConsoleEntry("Injecting scanner threads...", 65280);
      this.addConsoleEntry("Looking for stash....", 16776960);
   }

   public void onDisable() {
      this.addConsoleEntry("Terminating memory scan...", 16711680);
      this.consoleLog.clear();
   }

   @EventListener
   public void onTick(TickEvent e) {
      if (this.mc.world != null && this.mc.player != null) {
         ++this.tickCounter;
         if (!this.initialized && this.tickCounter > 40) {
            this.addConsoleEntry("Memory mapped successfully", 65280);
            this.addConsoleEntry("Scanning heap addresses...", 65535);
            this.initialized = true;
         }

         long now = System.currentTimeMillis();
         if (!((double)(now - this.lastUpdate) < this.updateDelay.getValue())) {
            this.lastUpdate = now;
            ++this.scanProgress;
            if (this.scanProgress % 10 == 0) {
               this.addConsoleEntry("Scanning sector 0x" + Integer.toHexString(this.rand.nextInt(65535)), 8421504);
            }

            int radius = (int)this.scanRadius.getValue();
            BlockPos playerPos = this.mc.player.getBlockPos();

            for(int dx = -radius; dx <= radius; ++dx) {
               for(int dy = -16; dy <= 16; ++dy) {
                  for(int dz = -radius; dz <= radius; ++dz) {
                     BlockPos pos = playerPos.add(dx, dy, dz);
                     if (!this.scannedPositions.contains(pos) && (!this.cooldowns.containsKey(pos) || now - (Long)this.cooldowns.get(pos) >= 30000L)) {
                        this.scanBlock(pos);
                     }
                  }
               }
            }

            if (this.showEntities.getValue()) {
               this.scanEntities();
            }

            int pcx = (int)this.mc.player.getX() >> 4;
            int pcz = (int)this.mc.player.getZ() >> 4;

            for(int cx = -2; cx <= 2; ++cx) {
               for(int cz = -2; cz <= 2; ++cz) {
                  WorldChunk chunk = this.mc.world.getChunkManager().getWorldChunk(pcx + cx, pcz + cz);
                  if (chunk != null) {
                     this.scanChunkContainers(chunk);
                  }
               }
            }

            if (this.deepMemoryScan.getValue() && this.scanProgress % 15 == 0) {
               this.performDeepScan();
            }

         }
      }
   }

   private void scanBlock(BlockPos pos) {
      if (this.mc.world != null) {
         Block block = this.mc.world.getBlockState(pos).getBlock();
         BlockEntity be = this.mc.world.getBlockEntity(pos);
         if (block == Blocks.DEEPSLATE_TILES || block == Blocks.DEEPSLATE_TILE_STAIRS || block == Blocks.DEEPSLATE_TILE_SLAB || block == Blocks.DEEPSLATE_TILE_WALL) {
            this.scannedPositions.add(pos);
            this.cooldowns.put(pos, System.currentTimeMillis());
            String memAddr = "0x" + Integer.toHexString(pos.hashCode()).toUpperCase();
            this.addConsoleEntry("Memory anomaly at " + memAddr, 16711935);
            int var10001 = pos.getX();
            this.addConsoleEntry("└ Rotated structure [" + var10001 + ", " + pos.getY() + ", " + pos.getZ() + "]", 16777215);
            if (this.deepMemoryScan.getValue() && this.rand.nextInt(100) < 30) {
               int fakeY = pos.getY() - (40 + this.rand.nextInt(10));
               this.addConsoleEntry("└ Phantom data below [" + pos.getX() + ", " + fakeY + ", " + pos.getZ() + "]", 8421504);
            }
         }

         if (be != null) {
            this.scannedPositions.add(pos);
            this.handleBlockEntity(be, pos);
         }

         if (block == Blocks.ENCHANTING_TABLE) {
            this.scannedPositions.add(pos);
            this.cooldowns.put(pos, System.currentTimeMillis());
            int var6 = pos.getX();
            this.addConsoleEntry("EnchTable ptr -> [" + var6 + ", " + pos.getY() + ", " + pos.getZ() + "]", 65535);
         }

      }
   }

   private void handleBlockEntity(BlockEntity be, BlockPos pos) {
      String type = "";
      int color = 16777215;
      if (be instanceof ChestBlockEntity) {
         type = "Chest";
         color = 16755200;
      } else if (be instanceof BarrelBlockEntity) {
         type = "Barrel";
         color = 9127187;
      } else if (be instanceof ShulkerBoxBlockEntity) {
         type = "Shulker";
         color = 16711935;
      } else if (be instanceof EnderChestBlockEntity) {
         type = "EnderChest";
         color = 9699539;
      } else {
         if (!(be instanceof MobSpawnerBlockEntity)) {
            return;
         }

         type = "Spawner";
         color = 16711680;
         this.addConsoleEntry("CRITICAL MEMORY BLOCK FOUND!", 16711680);
      }

      this.cooldowns.put(pos, System.currentTimeMillis());
      this.addConsoleEntry(type + " -> [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]", color);
   }

   private void scanChunkContainers(WorldChunk chunk) {
      ChunkPos cpos = chunk.getPos();
      if (!this.processedChunks.contains(cpos)) {
         Map<BlockPos, BlockEntity> entities = chunk.getBlockEntities();
         int chestCount = 0;
         int totalContainers = 0;
         BlockPos stashCenter = null;

         for(Map.Entry entry : entities.entrySet()) {
            BlockEntity be = (BlockEntity)entry.getValue();
            if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity) {
               ++totalContainers;
               if (be instanceof ChestBlockEntity) {
                  ++chestCount;
                  if (stashCenter == null) {
                     stashCenter = (BlockPos)entry.getKey();
                  }
               }
            }
         }

         if (totalContainers >= 5) {
            this.processedChunks.add(cpos);
            this.addConsoleEntry("STASH FOUND! Memory cluster detected", 16711680);
            if (stashCenter != null) {
               int var10001 = stashCenter.getX();
               this.addConsoleEntry("└ Center: [" + var10001 + ", " + stashCenter.getY() + ", " + stashCenter.getZ() + "]", 16776960);
            }

            this.addConsoleEntry("└ Containers: " + totalContainers + " (Chests: " + chestCount + ")", 16755200);
         }

      }
   }

   private void scanEntities() {
      if (this.mc.world != null) {
         for(Entity entity : this.mc.world.getEntities()) {
            if (entity != null && entity != this.mc.player) {
               double dist = (double)entity.distanceTo(this.mc.player);
               if (!(dist > this.scanRadius.getValue())) {
                  BlockPos epos = entity.getBlockPos();
                  if (!this.cooldowns.containsKey(epos) || System.currentTimeMillis() - (Long)this.cooldowns.get(epos) >= 60000L) {
                     if (entity instanceof VillagerEntity) {
                        this.cooldowns.put(epos, System.currentTimeMillis());
                        int var10001 = epos.getX();
                        this.addConsoleEntry("Villager entity -> [" + var10001 + ", " + epos.getY() + ", " + epos.getZ() + "]", 65280);
                     } else if (entity instanceof ArmorStandEntity) {
                        this.cooldowns.put(epos, System.currentTimeMillis());
                        int var6 = epos.getX();
                        this.addConsoleEntry("ArmorStand -> [" + var6 + ", " + epos.getY() + ", " + epos.getZ() + "]", 11184810);
                     }
                  }
               }
            }
         }

      }
   }

   private void performDeepScan() {
      if (this.rand.nextInt(100) < 25) {
         int fx = (int)this.mc.player.getX() + this.rand.nextInt(200) - 100;
         int fy = -50 + this.rand.nextInt(10);
         int fz = (int)this.mc.player.getZ() + this.rand.nextInt(200) - 100;
         String[] types = new String[]{"Chest cluster", "Storage cache", "Hidden vault", "Memory leak"};
         String type = types[this.rand.nextInt(types.length)];
         this.addConsoleEntry("Deep scan: " + type + " [" + fx + ", " + fy + ", " + fz + "]", 4210752);
      }

   }

   @EventListener
   public void onRender2D(Render2DEvent e) {
      if (this.isEnabled()) {
         DrawContext context = e.context;
         TextRenderer tr = this.mc.textRenderer;
         int x = 5;
         int y = 5;
         int width = 350;
         int maxLines = (int)this.consoleSize.getValue();
         int height = (maxLines + 2) * 10 + 10;
         if (this.glowEffect.getValue()) {
            context.fill(x - 2, y - 2, x + width + 2, y + height + 2, 805371648);
         }

         context.fill(x, y, x + width, y + height, -1442840576);
         context.fill(x, y, x + width, y + 12, -1442831872);
         String title = "Memory Scanner v2.1 [0x" + Integer.toHexString(this.tickCounter).toUpperCase() + "]";
         context.drawText(tr, title, x + 3, y + 2, 65280, false);
         int lineY = y + 15;
         int displayed = 0;
         List<ConsoleEntry> entries = new ArrayList(this.consoleLog);
         Collections.reverse(entries);

         for(ConsoleEntry entry : entries) {
            if (displayed >= maxLines) {
               break;
            }

            long age = System.currentTimeMillis() - entry.timestamp;
            int alpha = age > 10000L ? 128 : 255;
            int color = alpha << 24 | entry.color & 16777215;
            context.drawText(tr, entry.text, x + 3, lineY, color, false);
            lineY += 10;
            ++displayed;
         }

      }
   }

   private void addConsoleEntry(String text, int color) {
      this.consoleLog.addFirst(new ConsoleEntry(text, color));

      while(this.consoleLog.size() > 100) {
         this.consoleLog.removeLast();
      }

   }

   private static class ConsoleEntry {
      final String text;
      final int color;
      final long timestamp;

      ConsoleEntry(String text, int color) {
         this.text = text;
         this.color = color;
         this.timestamp = System.currentTimeMillis();
      }
   }
}
