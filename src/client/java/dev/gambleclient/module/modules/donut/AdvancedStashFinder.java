package dev.gambleclient.module.modules.donut;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.ModeSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.module.setting.StringSetting;
import dev.gambleclient.utils.EncryptedString;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.text.Text;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.entity.BarrelBlockEntity;

public class AdvancedStashFinder extends Module {
   private final NumberSetting minimumStorage = new NumberSetting(EncryptedString.of("Min Storage"), (double)1.0F, (double)500.0F, (double)4.0F, (double)1.0F);
   private final NumberSetting minimumDistance = new NumberSetting(EncryptedString.of("Min Distance"), (double)0.0F, (double)1000000.0F, (double)0.0F, (double)10.0F);
   private final BooleanSetting criticalSpawner = new BooleanSetting(EncryptedString.of("Critical Spawner"), true);
   private final BooleanSetting disconnectOnFind = new BooleanSetting(EncryptedString.of("Disconnect On Find"), false);
   private final BooleanSetting notifications = new BooleanSetting(EncryptedString.of("Notifications"), true);
   private final ModeSetting notifyMode;
   private final BooleanSetting webhook;
   private final StringSetting webhookUrl;
   private final BooleanSetting selfPing;
   private final StringSetting discordId;
   private final List stashes;
   private final Set processed;
   private final Map cooldown;
   private final HttpClient httpClient;

   public AdvancedStashFinder() {
      super(EncryptedString.of("AdvancedStashFinder"), EncryptedString.of("Detects stash chunks & spawners"), -1, Category.DONUT);
      this.notifyMode = new ModeSetting(EncryptedString.of("Notify Mode"), AdvancedStashFinder.StashNotifyMode.BOTH, StashNotifyMode.class);
      this.webhook = new BooleanSetting(EncryptedString.of("Webhook"), false);
      this.webhookUrl = new StringSetting(EncryptedString.of("Webhook URL"), "");
      this.selfPing = new BooleanSetting(EncryptedString.of("Self Ping"), false);
      this.discordId = new StringSetting(EncryptedString.of("Discord ID"), "");
      this.stashes = new ArrayList();
      this.processed = new HashSet();
      this.cooldown = new HashMap();
      this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();
      this.addSettings(new Setting[]{this.minimumStorage, this.minimumDistance, this.criticalSpawner, this.disconnectOnFind, this.notifications, this.notifyMode, this.webhook, this.webhookUrl, this.selfPing, this.discordId});
   }

   public void onEnable() {
      this.load();
      this.processed.clear();
      this.info("Enabled.");
   }

   public void onDisable() {
      this.saveJson();
      this.saveCsv();
      this.info("Disabled.");
   }

   @EventListener
   public void onTick(TickEvent e) {
      if (this.mc.world != null && this.mc.player != null) {
         if (this.mc.world.getTime() % 40L == 0L) {
            int vd = (Integer)this.mc.options.getViewDistance().getValue();
            int pcx = (int)this.mc.player.getX() >> 4;
            int pcz = (int)this.mc.player.getZ() >> 4;

            for(int dx = -vd - 1; dx <= vd + 1; ++dx) {
               for(int dz = -vd - 1; dz <= vd + 1; ++dz) {
                  WorldChunk wc = this.mc.world.getChunkManager().getWorldChunk(pcx + dx, pcz + dz);
                  if (wc != null) {
                     this.analyzeChunk(wc);
                  }
               }
            }

         }
      }
   }

   private void analyzeChunk(WorldChunk chunk) {
      ChunkPos pos = chunk.getPos();
      if (!this.processed.contains(pos)) {
         this.processed.add(pos);
         double ax = Math.abs((double)pos.x * (double)16.0F);
         double az = Math.abs((double)pos.z * (double)16.0F);
         double dist = Math.sqrt(ax * ax + az * az);
         if (!(dist < this.minimumDistance.getValue())) {
            StashChunk sc = new StashChunk(pos);

            for(BlockEntity be : chunk.getBlockEntities().values()) {
               if (be instanceof MobSpawnerBlockEntity) {
                  ++sc.spawners;
               } else if (be instanceof ChestBlockEntity) {
                  ++sc.chests;
               } else if (be instanceof BarrelBlockEntity) {
                  ++sc.barrels;
               } else if (be instanceof ShulkerBoxBlockEntity) {
                  ++sc.shulkers;
               } else if (be instanceof EnderChestBlockEntity) {
                  ++sc.enderChests;
               } else if (be instanceof AbstractFurnaceBlockEntity) {
                  ++sc.furnaces;
               } else if (be instanceof DispenserBlockEntity) {
                  ++sc.dispensersDroppers;
               } else if (be instanceof HopperBlockEntity) {
                  ++sc.hoppers;
               }
            }

            boolean isCritical = false;
            boolean isStash = false;
            String reason = "";
            if (this.criticalSpawner.getValue() && sc.spawners > 0) {
               isCritical = true;
               isStash = true;
               reason = "Spawner (critical)";
            } else if ((double)sc.getTotal() >= this.minimumStorage.getValue()) {
               isStash = true;
               reason = "Storage threshold " + sc.getTotal();
            }

            if (isStash) {
               long now = System.currentTimeMillis();
               Long last = (Long)this.cooldown.get(pos);
               if (last == null || now - last >= 15000L) {
                  this.cooldown.put(pos, now);
                  int idx = this.stashes.indexOf(sc);
                  StashChunk prev = null;
                  if (idx >= 0) {
                     prev = (StashChunk)this.stashes.set(idx, sc);
                  } else {
                     this.stashes.add(sc);
                  }

                  this.saveJson();
                  this.saveCsv();
                  boolean changed = prev == null || !sc.sameCounts(prev);
                  if (this.notifications.getValue() && changed) {
                     StashNotifyMode nm = this.currentMode();
                     this.chatNotify(sc, isCritical, reason);
                  }

                  if (this.webhook.getValue() && changed) {
                     this.sendWebhook(sc, isCritical, reason);
                  }

                  if (this.disconnectOnFind.getValue() && changed) {
                     this.info("Disconnecting after stash at " + sc.x + "," + sc.z);
                     this.mc.execute(() -> {
                        if (this.mc.world != null) {
                           this.mc.world.disconnect();
                        }

                        this.toggle();
                     });
                  }

               }
            }
         }
      }
   }

   private StashNotifyMode currentMode() {
      try {
         Enum<?> raw = this.notifyMode.getValue();
         if (raw instanceof StashNotifyMode sm) {
            return sm;
         } else {
            return AdvancedStashFinder.StashNotifyMode.BOTH;
         }
      } catch (Throwable var3) {
         return AdvancedStashFinder.StashNotifyMode.BOTH;
      }
   }

   private void chatNotify(StashChunk sc, boolean critical, String reason) {
      if (this.mc.player != null) {
         this.mc.player.sendMessage(Text.literal("§d[StashFinder] §fFound " + (critical ? "Spawner Base" : "Stash") + " at §b" + sc.x + "§f, §b" + sc.z + "§f (" + reason + ")"), false);
      }

   }

   private void sendWebhook(StashChunk sc, boolean critical, String reason) {
      String url = this.webhookUrl.getValue().trim();
      if (url.isEmpty()) {
         this.warning("Webhook URL empty.");
      } else {
         CompletableFuture.runAsync(() -> {
            try {
               String mention = "";
               if (this.selfPing.getValue() && !this.discordId.getValue().trim().isEmpty()) {
                  mention = "<@" + this.discordId.getValue().trim() + ">";
               }

               String title = critical ? "Spawner Base Found" : "Stash Found";
               StringBuilder breakdown = new StringBuilder();
               if (sc.spawners > 0) {
                  breakdown.append("Spawners: ").append(sc.spawners).append("\\n");
               }

               if (sc.chests > 0) {
                  breakdown.append("Chests: ").append(sc.chests).append("\\n");
               }

               if (sc.barrels > 0) {
                  breakdown.append("Barrels: ").append(sc.barrels).append("\\n");
               }

               if (sc.shulkers > 0) {
                  breakdown.append("Shulkers: ").append(sc.shulkers).append("\\n");
               }

               if (sc.enderChests > 0) {
                  breakdown.append("EnderChests: ").append(sc.enderChests).append("\\n");
               }

               if (sc.furnaces > 0) {
                  breakdown.append("Furnaces: ").append(sc.furnaces).append("\\n");
               }

               if (sc.dispensersDroppers > 0) {
                  breakdown.append("Disp/Drops: ").append(sc.dispensersDroppers).append("\\n");
               }

               if (sc.hoppers > 0) {
                  breakdown.append("Hoppers: ").append(sc.hoppers).append("\\n");
               }

               int color = critical ? 16711680 : 3447003;
               String json = "{\n  \"content\": \"%s\",\n  \"username\": \"Advanced Stashfinder\",\n  \"embeds\": [{\n    \"title\": \"%s\",\n    \"description\": \"Location: %d, %d\",\n    \"color\": %d,\n    \"fields\": [\n      {\"name\":\"Reason\",\"value\":\"%s\",\"inline\":false},\n      {\"name\":\"Total\",\"value\":\"%d\",\"inline\":false},\n      {\"name\":\"Breakdown\",\"value\":\"%s\",\"inline\":false}\n    ],\n    \"footer\":{\"text\":\"AdvancedStashFinder Phantom\"}\n  }]\n}\n".formatted(this.escape(mention), this.escape(title), sc.x, sc.z, color, this.escape(reason), sc.getTotal(), this.escape(breakdown.toString()));
               HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json").POST(BodyPublishers.ofString(json)).timeout(Duration.ofSeconds(15L)).build();
               HttpResponse<String> resp = this.httpClient.send(req, BodyHandlers.ofString());
               if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                  this.info("Webhook sent.");
               } else {
                  this.warning("Webhook failed " + resp.statusCode());
               }
            } catch (Exception ex) {
               this.warning("Webhook error: " + ex.getMessage());
            }

         });
      }
   }

   private File dataDir() {
      return new File(MinecraftClient.getInstance().runDirectory, "phantom-stashes");
   }

   private File jsonFile() {
      return new File(this.dataDir(), "advanced-stashes.json");
   }

   private File csvFile() {
      return new File(this.dataDir(), "advanced-stashes.csv");
   }

   private void load() {
      this.stashes.clear();
      File jf = this.jsonFile();
      if (jf.exists()) {
         try {
            BufferedReader ignored = new BufferedReader(new FileReader(jf));
            ignored.close();
         } catch (IOException ioe) {
            this.warning("Load JSON failed: " + ioe.getMessage());
         }

      }
   }

   private void saveJson() {
      try {
         this.dataDir().mkdirs();
         FileWriter fw = new FileWriter(this.jsonFile());

         try {
            fw.write("[\n");

            for(int i = 0; i < this.stashes.size(); ++i) {
               StashChunk sc = (StashChunk)this.stashes.get(i);
               fw.write(String.format("  {\"x\":%d,\"z\":%d,\"chests\":%d,\"barrels\":%d,\"shulkers\":%d,\"ender\":%d,\"furnaces\":%d,\"disp\":%d,\"hoppers\":%d,\"spawners\":%d}%s\n", sc.x, sc.z, sc.chests, sc.barrels, sc.shulkers, sc.enderChests, sc.furnaces, sc.dispensersDroppers, sc.hoppers, sc.spawners, i == this.stashes.size() - 1 ? "" : ","));
            }

            fw.write("]");
         } catch (Throwable var5) {
            try {
               fw.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         fw.close();
      } catch (IOException ioe) {
         this.warning("Save JSON failed: " + ioe.getMessage());
      }

   }

   private void saveCsv() {
      try {
         this.dataDir().mkdirs();
         FileWriter fw = new FileWriter(this.csvFile());

         try {
            fw.write("X,Z,Chests,Barrels,Shulkers,EnderChests,Furnaces,DispensersDroppers,Hoppers,Spawners\n");

            for(Object scObj : this.stashes) {
               StashChunk sc = (StashChunk)scObj;
               fw.write(sc.x + "," + sc.z + "," + sc.chests + "," + sc.barrels + "," + sc.shulkers + "," + sc.enderChests + "," + sc.furnaces + "," + sc.dispensersDroppers + "," + sc.hoppers + "," + sc.spawners + "\n");
            }
         } catch (Throwable var5) {
            try {
               fw.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         fw.close();
      } catch (IOException ioe) {
         this.warning("Save CSV failed: " + ioe.getMessage());
      }

   }

   public String getInfo() {
      return String.valueOf(this.stashes.size());
   }

   private String escape(String s) {
      return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
   }

   private void info(String s) {
      if (this.mc.player != null) {
         this.mc.player.sendMessage(Text.literal("§a[ASF] " + s), false);
      }

   }

   private void warning(String s) {
      if (this.mc.player != null) {
         this.mc.player.sendMessage(Text.literal("§6[ASF] " + s), false);
      }

   }

   private static class StashChunk {
      final ChunkPos chunkPos;
      final int x;
      final int z;
      int chests;
      int barrels;
      int shulkers;
      int enderChests;
      int furnaces;
      int dispensersDroppers;
      int hoppers;
      int spawners;

      StashChunk(ChunkPos pos) {
         this.chunkPos = pos;
         this.x = pos.x * 16 + 8;
         this.z = pos.z * 16 + 8;
      }

      int getTotal() {
         return this.chests + this.barrels + this.shulkers + this.enderChests + this.furnaces + this.dispensersDroppers + this.hoppers + this.spawners;
      }

      boolean sameCounts(StashChunk o) {
         if (o == null) {
            return false;
         } else {
            return this.chests == o.chests && this.barrels == o.barrels && this.shulkers == o.shulkers && this.enderChests == o.enderChests && this.furnaces == o.furnaces && this.dispensersDroppers == o.dispensersDroppers && this.hoppers == o.hoppers && this.spawners == o.spawners;
         }
      }

      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else if (other instanceof StashChunk) {
            StashChunk sc = (StashChunk)other;
            return this.chunkPos.equals(sc.chunkPos);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return this.chunkPos.hashCode();
      }
   }

   public static enum StashNotifyMode {
      CHAT,
      TOAST,
      BOTH;

      // $FF: synthetic method
      private static StashNotifyMode[] $values() {
         return new StashNotifyMode[]{CHAT, TOAST, BOTH};
      }
   }
}
