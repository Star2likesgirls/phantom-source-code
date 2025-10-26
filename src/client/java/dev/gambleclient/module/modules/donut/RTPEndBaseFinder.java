package dev.gambleclient.module.modules.donut;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.module.setting.StringSetting;
import dev.gambleclient.utils.BlockUtil;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.embed.DiscordWebhook;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.block.entity.BarrelBlockEntity;

public final class RTPEndBaseFinder extends Module {
   private final NumberSetting minimumStorageCount = (NumberSetting)(new NumberSetting(EncryptedString.of("Minimum Storage Count"), (double)1.0F, (double)100.0F, (double)4.0F, (double)1.0F)).setDescription(EncryptedString.of("The minimum amount of storage blocks in a chunk to record the chunk (spawners ignore this limit)"));
   private final BooleanSetting criticalSpawner = (new BooleanSetting(EncryptedString.of("Critical Spawner"), true)).setDescription(EncryptedString.of("Mark chunk as stash even if only a single spawner is found"));
   private final NumberSetting rtpInterval = (NumberSetting)(new NumberSetting(EncryptedString.of("RTP Interval"), (double)5.0F, (double)60.0F, (double)10.0F, (double)1.0F)).setDescription(EncryptedString.of("Interval between RTP commands in seconds"));
   private final BooleanSetting disconnectOnBaseFind = (new BooleanSetting(EncryptedString.of("Disconnect on Base Find"), true)).setDescription(EncryptedString.of("Automatically disconnect when a base is found"));
   private final BooleanSetting lookDown = (new BooleanSetting(EncryptedString.of("Look Down"), true)).setDescription(EncryptedString.of("Automatically look down to avoid enderman aggro"));
   private final BooleanSetting sendNotifications = (new BooleanSetting(EncryptedString.of("Notifications"), true)).setDescription(EncryptedString.of("Sends Minecraft notifications when new stashes are found"));
   private final BooleanSetting enableWebhook = (new BooleanSetting(EncryptedString.of("Webhook"), false)).setDescription(EncryptedString.of("Send webhook notifications when stashes are found"));
   private final StringSetting webhookUrl = (new StringSetting(EncryptedString.of("Webhook URL"), "")).setDescription(EncryptedString.of("Discord webhook URL"));
   private final BooleanSetting selfPing = (new BooleanSetting(EncryptedString.of("Self Ping"), false)).setDescription(EncryptedString.of("Ping yourself in the webhook message"));
   private final StringSetting discordId = (new StringSetting(EncryptedString.of("Discord ID"), "")).setDescription(EncryptedString.of("Your Discord user ID for pinging"));
   private final NumberSetting scanInterval = (NumberSetting)(new NumberSetting(EncryptedString.of("Scan Interval"), (double)1.0F, (double)20.0F, (double)5.0F, (double)1.0F)).setDescription(EncryptedString.of("Interval between chunk scans in ticks (higher = less lag)"));
   private final NumberSetting maxChunksPerScan = (NumberSetting)(new NumberSetting(EncryptedString.of("Max Chunks Per Scan"), (double)1.0F, (double)10.0F, (double)3.0F, (double)1.0F)).setDescription(EncryptedString.of("Maximum chunks to scan per tick (lower = less lag)"));
   private final BooleanSetting enableSpawnerCheck = (new BooleanSetting(EncryptedString.of("Enable Spawner Check"), true)).setDescription(EncryptedString.of("Check for spawners (disable for maximum performance)"));
   public List foundStashes = new ArrayList();
   private final Set processedChunks = new HashSet();
   private long lastRtpTime = 0L;
   private Float originalPitch = null;
   private int scanTickCounter = 0;
   private final Queue chunksToScan = new LinkedList();
   private final Set chunksInQueue = new HashSet();
   private long lastPerformanceCheck = 0L;
   private int chunksScannedThisSecond = 0;

   public RTPEndBaseFinder() {
      super(EncryptedString.of("RTP End Base Finder"), EncryptedString.of("Continuously RTPs to the End and searches for stashes"), -1, Category.DONUT);
      this.addSettings(new Setting[]{this.minimumStorageCount, this.criticalSpawner, this.rtpInterval, this.disconnectOnBaseFind, this.lookDown, this.sendNotifications, this.enableWebhook, this.webhookUrl, this.selfPing, this.discordId, this.scanInterval, this.maxChunksPerScan, this.enableSpawnerCheck});
   }

   public void onEnable() {
      this.foundStashes.clear();
      this.processedChunks.clear();
      this.lastRtpTime = 0L;
      this.scanTickCounter = 0;
      this.chunksToScan.clear();
      this.chunksInQueue.clear();
      if (this.lookDown.getValue() && this.mc.player != null) {
         this.originalPitch = this.mc.player.getPitch();
      }

      System.out.println("Started RTP End Base Finder");
   }

   public void onDisable() {
      this.processedChunks.clear();
      this.chunksToScan.clear();
      this.chunksInQueue.clear();
      if (this.originalPitch != null && this.mc.player != null) {
         this.mc.player.setPitch(this.originalPitch);
         this.originalPitch = null;
      }

   }

   @EventListener
   public void onTick(TickEvent tickEvent) {
      if (this.mc.player != null && this.mc.world != null) {
         if (this.lookDown.getValue()) {
            this.mc.player.setPitch(90.0F);
         }

         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastRtpTime >= (long)this.rtpInterval.getIntValue() * 1000L) {
            this.mc.player.networkHandler.sendChatCommand("rtp end");
            this.lastRtpTime = currentTime;
         }

         ++this.scanTickCounter;
         if (this.scanTickCounter >= this.scanInterval.getIntValue()) {
            this.scanTickCounter = 0;
            this.updateChunkQueue();
            this.scanQueuedChunks();
         }

      } else {
         if (this.isEnabled()) {
            this.toggle();
         }

      }
   }

   private void updateChunkQueue() {
      if (this.mc.player != null && this.mc.world != null) {
         for(Object chunkObj : BlockUtil.getLoadedChunks().toList()) {
            WorldChunk worldChunk = (WorldChunk)chunkObj;
            ChunkPos chunkPos = worldChunk.getPos();
            if (!this.processedChunks.contains(chunkPos) && !this.chunksInQueue.contains(chunkPos)) {
               this.chunksToScan.offer(chunkPos);
               this.chunksInQueue.add(chunkPos);
            }
         }

      }
   }

   private void scanQueuedChunks() {
      int chunksScanned = 0;

      for(int maxChunks = this.maxChunksPerScan.getIntValue(); !this.chunksToScan.isEmpty() && chunksScanned < maxChunks; ++this.chunksScannedThisSecond) {
         ChunkPos chunkPos = (ChunkPos)this.chunksToScan.poll();
         this.chunksInQueue.remove(chunkPos);
         if (chunkPos != null && this.mc.world.isChunkLoaded(chunkPos.x, chunkPos.z)) {
            WorldChunk worldChunk = this.mc.world.getChunk(chunkPos.x, chunkPos.z);
            if (worldChunk != null) {
               this.scanSingleChunk(worldChunk);
            }
         }

         ++chunksScanned;
      }

      long currentTime = System.currentTimeMillis();
      if (currentTime - this.lastPerformanceCheck >= 1000L) {
         if (this.chunksScannedThisSecond > 0) {
            System.out.println("[RTPEndBaseFinder] Performance: " + this.chunksScannedThisSecond + " chunks scanned in last second");
         }

         this.chunksScannedThisSecond = 0;
         this.lastPerformanceCheck = currentTime;
      }

   }

   private void scanSingleChunk(WorldChunk worldChunk) {
      ChunkPos chunkPos = worldChunk.getPos();
      if (!this.processedChunks.contains(chunkPos)) {
         EndStashChunk chunk = new EndStashChunk(chunkPos);
         boolean hasSpawner = false;
         if (this.enableSpawnerCheck.getValue()) {
            for(BlockEntity blockEntity : worldChunk.getBlockEntities().values()) {
               if (blockEntity instanceof MobSpawnerBlockEntity) {
                  ++chunk.spawners;
                  hasSpawner = true;
               }
            }

            if (!hasSpawner && this.criticalSpawner.getValue()) {
               hasSpawner = this.quickSpawnerCheck(worldChunk);
               if (hasSpawner) {
                  ++chunk.spawners;
               }
            }
         }

         for(BlockEntity blockEntity : worldChunk.getBlockEntities().values()) {
            BlockEntityType<?> type = blockEntity.getType();
            if (this.isStorageBlock(type)) {
               if (blockEntity instanceof ChestBlockEntity) {
                  ++chunk.chests;
               } else if (blockEntity instanceof BarrelBlockEntity) {
                  ++chunk.barrels;
               } else if (blockEntity instanceof ShulkerBoxBlockEntity) {
                  ++chunk.shulkers;
               } else if (blockEntity instanceof EnderChestBlockEntity) {
                  ++chunk.enderChests;
               } else if (blockEntity instanceof AbstractFurnaceBlockEntity) {
                  ++chunk.furnaces;
               } else if (blockEntity instanceof DispenserBlockEntity) {
                  ++chunk.dispensersDroppers;
               } else if (blockEntity instanceof HopperBlockEntity) {
                  ++chunk.hoppers;
               }
            }
         }

         boolean isStash = false;
         boolean isCriticalSpawner = false;
         String detectionReason = "";
         if (this.criticalSpawner.getValue() && hasSpawner) {
            isStash = true;
            isCriticalSpawner = true;
            detectionReason = "Spawner(s) detected (Critical mode)";
         } else if (chunk.getTotalNonSpawner() >= this.minimumStorageCount.getIntValue()) {
            isStash = true;
            detectionReason = "Storage threshold reached (" + chunk.getTotalNonSpawner() + " blocks)";
         }

         if (isStash) {
            this.processedChunks.add(chunkPos);
            EndStashChunk prevChunk = null;
            int existingIndex = this.foundStashes.indexOf(chunk);
            if (existingIndex < 0) {
               this.foundStashes.add(chunk);
            } else {
               prevChunk = (EndStashChunk)this.foundStashes.set(existingIndex, chunk);
            }

            if (this.sendNotifications.getValue() && (!chunk.equals(prevChunk) || !chunk.countsEqual(prevChunk))) {
               String stashType = isCriticalSpawner ? "End spawner base" : "End stash";
               System.out.println("Found " + stashType + " at " + chunk.x + ", " + chunk.z + ". " + detectionReason);
            }

            if (this.enableWebhook.getValue() && (!chunk.equals(prevChunk) || !chunk.countsEqual(prevChunk))) {
               this.sendWebhookNotification(chunk, isCriticalSpawner, detectionReason);
            }

            if (this.disconnectOnBaseFind.getValue()) {
               String stashTypeForDisconnect = isCriticalSpawner ? "End spawner base" : "End stash";
               this.disconnectPlayer(stashTypeForDisconnect, chunk);
            }
         }

      }
   }

   private boolean quickSpawnerCheck(WorldChunk worldChunk) {
      ChunkPos chunkPos = worldChunk.getPos();
      int xStart = chunkPos.getStartX();
      int zStart = chunkPos.getStartZ();
      int[] checkY = new int[]{8, 16, 24, 32, 40, 48, 56, 64};
      int[] checkX = new int[]{2, 6, 10, 14};
      int[] checkZ = new int[]{2, 6, 10, 14};

      for(int y : checkY) {
         for(int x : checkX) {
            for(int z : checkZ) {
               BlockPos pos = new BlockPos(xStart + x, y, zStart + z);

               try {
                  if (this.mc.world.getBlockState(pos).getBlock() == Blocks.SPAWNER) {
                     return true;
                  }
               } catch (Exception var22) {
               }
            }
         }
      }

      return false;
   }

   private boolean isStorageBlock(BlockEntityType type) {
      return type == net.minecraft.block.entity.BlockEntityType.CHEST || type == net.minecraft.block.entity.BlockEntityType.BARREL || type == net.minecraft.block.entity.BlockEntityType.SHULKER_BOX || type == net.minecraft.block.entity.BlockEntityType.ENDER_CHEST || type == net.minecraft.block.entity.BlockEntityType.FURNACE || type == net.minecraft.block.entity.BlockEntityType.BLAST_FURNACE || type == net.minecraft.block.entity.BlockEntityType.SMOKER || type == net.minecraft.block.entity.BlockEntityType.DISPENSER || type == net.minecraft.block.entity.BlockEntityType.DROPPER || type == net.minecraft.block.entity.BlockEntityType.HOPPER;
   }

   private void disconnectPlayer(String stashType, EndStashChunk chunk) {
      System.out.println("Disconnecting due to " + stashType + " found at " + chunk.x + ", " + chunk.z);
      this.toggle();
      Executors.newSingleThreadScheduledExecutor().schedule(() -> {
         if (this.mc.player != null) {
            this.mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("END STASH FOUND AT " + chunk.x + ", " + chunk.z + "!")));
         }

      }, 1L, TimeUnit.SECONDS);
   }

   private void sendWebhookNotification(EndStashChunk chunk, boolean isCriticalSpawner, String detectionReason) {
      String url = this.webhookUrl.getValue().trim();
      if (url.isEmpty()) {
         System.out.println("Webhook URL not configured!");
      } else {
         CompletableFuture.runAsync(() -> {
            try {
               String serverInfo = this.mc.getCurrentServerEntry() != null ? this.mc.getCurrentServerEntry().address : "Unknown Server";
               String messageContent = "";
               if (this.selfPing.getValue() && !this.discordId.getValue().trim().isEmpty()) {
                  messageContent = String.format("<@%s>", this.discordId.getValue().trim());
               }

               String stashType = isCriticalSpawner ? "End Spawner Base" : "End Stash";
               String description = String.format("%s found at End coordinates %d, %d!", stashType, chunk.x, chunk.z);
               StringBuilder itemsFound = new StringBuilder();
               int totalItems = 0;
               if (chunk.chests > 0) {
                  itemsFound.append("Chests: ").append(chunk.chests).append("\\n");
                  totalItems += chunk.chests;
               }

               if (chunk.barrels > 0) {
                  itemsFound.append("Barrels: ").append(chunk.barrels).append("\\n");
                  totalItems += chunk.barrels;
               }

               if (chunk.shulkers > 0) {
                  itemsFound.append("Shulker Boxes: ").append(chunk.shulkers).append("\\n");
                  totalItems += chunk.shulkers;
               }

               if (chunk.enderChests > 0) {
                  itemsFound.append("Ender Chests: ").append(chunk.enderChests).append("\\n");
                  totalItems += chunk.enderChests;
               }

               if (chunk.furnaces > 0) {
                  itemsFound.append("Furnaces: ").append(chunk.furnaces).append("\\n");
                  totalItems += chunk.furnaces;
               }

               if (chunk.dispensersDroppers > 0) {
                  itemsFound.append("Dispensers/Droppers: ").append(chunk.dispensersDroppers).append("\\n");
                  totalItems += chunk.dispensersDroppers;
               }

               if (chunk.hoppers > 0) {
                  itemsFound.append("Hoppers: ").append(chunk.hoppers).append("\\n");
                  totalItems += chunk.hoppers;
               }

               if (isCriticalSpawner) {
                  itemsFound.append("Spawners: Present\\n");
               }

               DiscordWebhook webhook = new DiscordWebhook(url);
               webhook.setUsername("RTP End-Stashfinder");
               webhook.setAvatarUrl("https://i.imgur.com/OL2y1cr.png");
               webhook.setContent(messageContent);
               DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();
               embed.setTitle("\ud83c\udf0c End Stashfinder Alert");
               embed.setDescription(description);
               embed.setColor(new Color(isCriticalSpawner ? 9830144 : 8388736));
               embed.addField("Detection Reason", detectionReason, false);
               embed.addField("Total Items Found", String.valueOf(totalItems), false);
               embed.addField("Items Breakdown", itemsFound.toString(), false);
               embed.addField("End Coordinates", chunk.x + ", " + chunk.z, true);
               embed.addField("Server", serverInfo, true);
               embed.addField("Time", "<t:" + System.currentTimeMillis() / 1000L + ":R>", true);
               embed.setFooter("RTP End-Stashfinder", (String)null);
               webhook.addEmbed(embed);
               webhook.execute();
               System.out.println("Webhook notification sent successfully");
            } catch (Throwable e) {
               System.out.println("Failed to send webhook: " + e.getMessage());
            }

         });
      }
   }

   public static class EndStashChunk {
      public ChunkPos chunkPos;
      public transient int x;
      public transient int z;
      public int chests;
      public int barrels;
      public int shulkers;
      public int enderChests;
      public int furnaces;
      public int dispensersDroppers;
      public int hoppers;
      public int spawners;

      public EndStashChunk(ChunkPos chunkPos) {
         this.chunkPos = chunkPos;
         this.calculatePos();
      }

      public void calculatePos() {
         this.x = this.chunkPos.x * 16 + 8;
         this.z = this.chunkPos.z * 16 + 8;
      }

      public int getTotal() {
         return this.chests + this.barrels + this.shulkers + this.enderChests + this.furnaces + this.dispensersDroppers + this.hoppers + this.spawners;
      }

      public int getTotalNonSpawner() {
         return this.chests + this.barrels + this.shulkers + this.enderChests + this.furnaces + this.dispensersDroppers + this.hoppers;
      }

      public boolean countsEqual(EndStashChunk c) {
         if (c == null) {
            return false;
         } else {
            return this.chests == c.chests && this.barrels == c.barrels && this.shulkers == c.shulkers && this.enderChests == c.enderChests && this.furnaces == c.furnaces && this.dispensersDroppers == c.dispensersDroppers && this.hoppers == c.hoppers && this.spawners == c.spawners;
         }
      }

      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            EndStashChunk chunk = (EndStashChunk)o;
            return Objects.equals(this.chunkPos, chunk.chunkPos);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return Objects.hash(new Object[]{this.chunkPos});
      }
   }
}
