package dev.gambleclient.module.modules.donut;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.module.setting.StringSetting;
import dev.gambleclient.utils.EncryptedString;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.gui.screen.world.WorldCreator;

public class SpawnerProtect extends Module {
   private final NumberSetting spawnerRange = new NumberSetting(EncryptedString.of("Spawner Range"), (double)1.0F, (double)64.0F, (double)16.0F, (double)1.0F);
   private final NumberSetting recheckDelay = new NumberSetting(EncryptedString.of("Recheck Delay s"), (double)1.0F, (double)30.0F, (double)1.0F, (double)1.0F);
   private final NumberSetting miningTimeout = new NumberSetting(EncryptedString.of("Mining Timeout s"), (double)1.0F, (double)60.0F, (double)3.0F, (double)1.0F);
   private final NumberSetting miningRestartDelay = new NumberSetting(EncryptedString.of("Restart Delay s"), (double)1.0F, (double)60.0F, (double)2.0F, (double)1.0F);
   private final NumberSetting emergencyDistance = new NumberSetting(EncryptedString.of("Emergency Dist"), (double)1.0F, (double)32.0F, (double)7.0F, (double)1.0F);
   private final NumberSetting positionTolerance = new NumberSetting(EncryptedString.of("Position Tolerance"), (double)1.0F, (double)32.0F, (double)5.0F, (double)1.0F);
   private final BooleanSetting webhook = new BooleanSetting(EncryptedString.of("Webhook"), false);
   private final StringSetting webhookUrl = new StringSetting(EncryptedString.of("Webhook URL"), "");
   private final BooleanSetting selfPing = new BooleanSetting(EncryptedString.of("Self Ping"), false);
   private final StringSetting discordId = new StringSetting(EncryptedString.of("Discord ID"), "");
   private final BooleanSetting whitelistEnabled = new BooleanSetting(EncryptedString.of("Whitelist Enabled"), false);
   private final StringSetting whitelistCsv = new StringSetting(EncryptedString.of("Whitelist CSV"), "");
   private State state;
   private String detectedPlayer;
   private long detectionTime;
   private boolean minedSpawners;
   private boolean depositedItems;
   private BlockPos currentTarget;
   private boolean waiting;
   private int tickCounter;
   private int recheckCounter;
   private int confirmCounter;
   private int miningStartTick;
   private boolean miningActive;
   private boolean timeoutTriggered;
   private int restartCounter;
   private BlockPos enderChestTarget;
   private int chestAttempts;
   private int transferDelay;
   private int lastProcessedSlot;
   private boolean emergencyDisconnect;
   private String emergencyReason;
   private Vec3d startPosition;
   private boolean startTracked;
   private int positionCheckTicks;
   private final HttpClient httpClient;

   public SpawnerProtect() {
      super(EncryptedString.of("SpawnerProtect"), EncryptedString.of("Auto mines & stores spawners on player detect"), -1, Category.DONUT);
      this.state = SpawnerProtect.State.IDLE;
      this.detectedPlayer = "";
      this.detectionTime = 0L;
      this.minedSpawners = false;
      this.depositedItems = false;
      this.currentTarget = null;
      this.waiting = false;
      this.tickCounter = 0;
      this.recheckCounter = 0;
      this.confirmCounter = 0;
      this.miningStartTick = 0;
      this.miningActive = false;
      this.timeoutTriggered = false;
      this.restartCounter = 0;
      this.enderChestTarget = null;
      this.chestAttempts = 0;
      this.transferDelay = 0;
      this.lastProcessedSlot = -1;
      this.emergencyDisconnect = false;
      this.emergencyReason = "";
      this.startPosition = null;
      this.startTracked = false;
      this.positionCheckTicks = 0;
      this.httpClient = HttpClient.newHttpClient();
      this.addSettings(new Setting[]{this.spawnerRange, this.recheckDelay, this.miningTimeout, this.miningRestartDelay, this.emergencyDistance, this.positionTolerance, this.webhook, this.webhookUrl, this.selfPing, this.discordId, this.whitelistEnabled, this.whitelistCsv});
   }

   public void onEnable() {
      this.resetAll();
      if (this.mc.player != null) {
         this.startPosition = this.mc.player.getPos();
         this.startTracked = true;
         this.info("Start position recorded.");
      }

      this.info("SpawnerProtect active.");
   }

   public void onDisable() {
      this.stopBreaking();
      this.setSneaking(false);
      this.releaseMovementKeys();
   }

   private void resetAll() {
      this.state = SpawnerProtect.State.IDLE;
      this.detectedPlayer = "";
      this.detectionTime = 0L;
      this.minedSpawners = false;
      this.depositedItems = false;
      this.currentTarget = null;
      this.waiting = false;
      this.recheckCounter = 0;
      this.confirmCounter = 0;
      this.miningStartTick = 0;
      this.miningActive = false;
      this.timeoutTriggered = false;
      this.restartCounter = 0;
      this.enderChestTarget = null;
      this.chestAttempts = 0;
      this.transferDelay = 0;
      this.lastProcessedSlot = -1;
      this.emergencyDisconnect = false;
      this.emergencyReason = "";
      this.positionCheckTicks = 0;
      this.tickCounter = 0;
   }

   @EventListener
   public void onTick(TickEvent e) {
      if (this.mc.player != null && this.mc.world != null) {
         ++this.tickCounter;
         ++this.positionCheckTicks;
         if (this.transferDelay > 0) {
            --this.transferDelay;
         } else {
            if (this.positionCheckTicks >= 20) {
               this.positionCheckTicks = 0;
               if (!this.isAtStartPosition() && this.state == SpawnerProtect.State.IDLE) {
                  this.state = SpawnerProtect.State.POSITION_DISPLACED;
                  this.info("Moved away - paused.");
               } else if (this.isAtStartPosition() && this.state == SpawnerProtect.State.POSITION_DISPLACED) {
                  this.state = SpawnerProtect.State.IDLE;
                  this.info("Returned - resumed.");
               }
            }

            if (this.state != SpawnerProtect.State.POSITION_DISPLACED) {
               if (!this.checkEmergency()) {
                  switch (this.state.ordinal()) {
                     case 0 -> this.detectPlayers();
                     case 1 -> this.mineSpawnersLogic();
                     case 2 -> this.confirmNoMoreSpawners();
                     case 3 -> this.moveToEnderChest();
                     case 4 -> this.openChestSequence();
                     case 5 -> this.depositItems();
                     case 6 -> this.disconnectSequence();
                  }

               }
            }
         }
      }
   }

   private boolean isAtStartPosition() {
      if (this.startTracked && this.startPosition != null && this.mc.player != null) {
         return this.mc.player.getPos().distanceTo(this.startPosition) <= this.positionTolerance.getValue();
      } else {
         return true;
      }
   }

   private boolean checkEmergency() {
      for(PlayerEntity p : this.mc.world.getPlayers()) {
         if (p != this.mc.player && !this.isWhitelisted(p.getGameProfile().getName())) {
            double d = (double)p.distanceTo(this.mc.player);
            if (d <= this.emergencyDistance.getValue()) {
               this.emergencyDisconnect = true;
               String var10001 = p.getGameProfile().getName();
               this.emergencyReason = "Player " + var10001 + " too close (" + String.format("%.1f", d) + ")";
               this.detectedPlayer = p.getGameProfile().getName();
               this.detectionTime = System.currentTimeMillis();
               this.state = SpawnerProtect.State.DISCONNECTING;
               this.info("Emergency disconnect triggered.");
               return true;
            }
         }
      }

      return false;
   }

   private void detectPlayers() {
      for(PlayerEntity p : this.mc.world.getPlayers()) {
         if (p != this.mc.player && !this.isWhitelisted(p.getGameProfile().getName())) {
            this.detectedPlayer = p.getGameProfile().getName();
            this.detectionTime = System.currentTimeMillis();
            this.info("Player detected: " + this.detectedPlayer);
            this.setSneaking(true);
            this.state = SpawnerProtect.State.GOING_TO_SPAWNERS;
            return;
         }
      }

   }

   private boolean isWhitelisted(String name) {
      if (!this.whitelistEnabled.getValue()) {
         return false;
      } else {
         for(Object sObj : this.parseWhitelist()) {
            String s = (String)sObj;
            if (s.equalsIgnoreCase(name)) {
               return true;
            }
         }

         return false;
      }
   }

   private Set parseWhitelist() {
      String raw = this.whitelistCsv.getValue();
      Set<String> set = new HashSet();
      if (raw != null && !raw.isEmpty()) {
         for(String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
               set.add(trimmed);
            }
         }

         return set;
      } else {
         return set;
      }
   }

   private void mineSpawnersLogic() {
      this.setSneaking(true);
      if (this.timeoutTriggered) {
         ++this.restartCounter;
         if (!((double)this.restartCounter >= this.miningRestartDelay.getValue() * (double)20.0F)) {
            return;
         }

         this.timeoutTriggered = false;
         this.restartCounter = 0;
         this.info("Retrying mining after timeout.");
      }

      if (this.currentTarget == null) {
         this.currentTarget = this.findNearestSpawner();
         if (this.currentTarget == null && !this.waiting) {
            this.waiting = true;
            this.recheckCounter = 0;
            this.confirmCounter = 0;
            this.state = SpawnerProtect.State.WAITING_CONFIRM;
            this.info("No spawners found. Confirming...");
         } else if (this.currentTarget != null) {
            this.miningStartTick = this.tickCounter;
            this.miningActive = true;
            this.info("Mining spawner " + this.currentTarget.toShortString());
         }
      } else {
         if (this.miningActive && (double)(this.tickCounter - this.miningStartTick) > this.miningTimeout.getValue() * (double)20.0F) {
            this.info("Mining timeout at " + String.valueOf(this.currentTarget));
            this.stopBreaking();
            this.timeoutTriggered = true;
            this.miningActive = false;
            this.currentTarget = null;
            return;
         }

         this.lookAt(this.currentTarget);
         this.breakBlock(this.currentTarget);
         if (this.mc.world.getBlockState(this.currentTarget).isAir()) {
            this.info("Spawner broken " + this.currentTarget.toShortString());
            this.stopBreaking();
            this.miningActive = false;
            this.currentTarget = null;
            this.transferDelay = 4;
         }
      }

   }

   private void confirmNoMoreSpawners() {
      ++this.recheckCounter;
      if ((double)this.recheckCounter == this.recheckDelay.getValue() * (double)20.0F) {
         BlockPos again = this.findNearestSpawner();
         if (again != null) {
            this.waiting = false;
            this.state = SpawnerProtect.State.GOING_TO_SPAWNERS;
            this.currentTarget = again;
            this.miningStartTick = this.tickCounter;
            this.miningActive = true;
            this.info("Additional spawner found: " + again.toShortString());
            return;
         }
      }

      if ((double)this.recheckCounter > this.recheckDelay.getValue() * (double)20.0F) {
         ++this.confirmCounter;
         if (this.confirmCounter >= 5) {
            this.minedSpawners = true;
            this.setSneaking(false);
            this.state = SpawnerProtect.State.GOING_TO_CHEST;
            this.tickCounter = 0;
            this.info("All spawners cleared. Moving to ender chest.");
         }
      }

   }

   private BlockPos findNearestSpawner() {
      BlockPos base = this.mc.player.getBlockPos();
      int range = (int)this.spawnerRange.getValue();
      BlockPos nearest = null;
      double nearestSq = Double.MAX_VALUE;
      BlockPos min = base.add(-range, -range, -range);
      BlockPos max = base.add(range, range, range);

      for(BlockPos p : BlockPos.iterate(min, max)) {
         if (this.mc.world.getBlockState(p).getBlock() == Blocks.SPAWNER) {
            double d = p.getSquaredDistance(this.mc.player.getPos());
            if (d < nearestSq) {
               nearestSq = d;
               nearest = p.toImmutable();
            }
         }
      }

      return nearest;
   }

   private void moveToEnderChest() {
      if (this.enderChestTarget == null) {
         this.enderChestTarget = this.locateEnderChest();
         if (this.enderChestTarget == null) {
            this.info("No ender chest found. Disconnecting.");
            this.state = SpawnerProtect.State.DISCONNECTING;
            return;
         }

         this.info("Ender chest at " + this.enderChestTarget.toShortString());
      }

      this.moveToward(this.enderChestTarget);
      if (this.mc.player.getBlockPos().getSquaredDistance(this.enderChestTarget) <= (double)9.0F) {
         this.state = SpawnerProtect.State.OPENING_CHEST;
         this.chestAttempts = 0;
         this.info("Reached chest. Opening...");
      }

      if (this.tickCounter > 600) {
         this.info("Timeout locating chest.");
         this.state = SpawnerProtect.State.DISCONNECTING;
      }

   }

   private BlockPos locateEnderChest() {
      BlockPos base = this.mc.player.getBlockPos();
      BlockPos nearest = null;
      double best = Double.MAX_VALUE;

      for(BlockPos p : BlockPos.iterate(base.add(-16, -8, -16), base.add(16, 8, 16))) {
         if (this.mc.world.getBlockState(p).getBlock() == Blocks.ENDER_CHEST) {
            double d = p.getSquaredDistance(this.mc.player.getPos());
            if (d < best) {
               best = d;
               nearest = p.toImmutable();
            }
         }
      }

      return nearest;
   }

   private void moveToward(BlockPos target) {
      Vec3d player = this.mc.player.getPos();
      Vec3d center = Vec3d.ofCenter(target);
      Vec3d dir = center.subtract(player).normalize();
      float yaw = (float)(Math.toDegrees(Math.atan2(dir.z, dir.x)) - (double)90.0F);
      this.mc.player.setYaw(yaw);
      KeyBinding.setKeyPressed(this.mc.options.forwardKey.getDefaultKey(), true);
   }

   private void openChestSequence() {
      if (this.enderChestTarget == null) {
         this.state = SpawnerProtect.State.GOING_TO_CHEST;
      } else {
         this.releaseMovementKeys();
         KeyBinding.setKeyPressed(this.mc.options.jumpKey.getDefaultKey(), true);
         if (this.chestAttempts < 20) {
            this.lookAt(this.enderChestTarget);
         }

         if (this.chestAttempts % 5 == 0 && this.mc.interactionManager != null) {
            this.mc.interactionManager.interactBlock(this.mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(this.enderChestTarget), Direction.UP, this.enderChestTarget, false));
         }

         ++this.chestAttempts;
         if (this.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
            KeyBinding.setKeyPressed(this.mc.options.jumpKey.getDefaultKey(), false);
            this.state = SpawnerProtect.State.DEPOSITING;
            this.lastProcessedSlot = -1;
            this.tickCounter = 0;
            this.info("Chest opened.");
         }

         if (this.chestAttempts > 200) {
            this.info("Failed to open chest.");
            this.state = SpawnerProtect.State.DISCONNECTING;
         }

      }
   }

   private void depositItems() {
      ScreenHandler h = this.mc.player.currentScreenHandler;
      if (!(h instanceof GenericContainerScreenHandler)) {
         this.state = SpawnerProtect.State.OPENING_CHEST;
         this.chestAttempts = 0;
      } else if (!this.hasInventoryItems()) {
         this.depositedItems = true;
         this.info("Items deposited.");
         this.mc.player.closeHandledScreen();
         this.transferDelay = 10;
         this.state = SpawnerProtect.State.DISCONNECTING;
      } else {
         this.moveItems((GenericContainerScreenHandler)h);
         if (this.tickCounter > 900) {
            this.info("Deposit timeout.");
            this.state = SpawnerProtect.State.DISCONNECTING;
         }

      }
   }

   private boolean hasInventoryItems() {
      for(int i = 0; i < 36; ++i) {
         ItemStack st = this.mc.player.getInventory().getStack(i);
         if (!st.isEmpty() && st.getItem() != Items.AIR) {
            return true;
         }
      }

      return false;
   }

   private void moveItems(GenericContainerScreenHandler h) {
      int total = h.slots.size();
      int chestSlots = total - 36;
      int playerStart = chestSlots;
      int start = Math.max(this.lastProcessedSlot + 1, chestSlots);

      for(int i = 0; i < 36; ++i) {
         int slot = playerStart + (start - playerStart + i) % 36;
         ItemStack stack = h.getSlot(slot).getStack();
         if (!stack.isEmpty() && stack.getItem() != Items.AIR) {
            if (this.mc.interactionManager != null) {
               this.mc.interactionManager.clickSlot(h.syncId, slot, 0, net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, this.mc.player);
            }

            this.lastProcessedSlot = slot;
            this.transferDelay = 2;
            return;
         }
      }

      if (this.lastProcessedSlot >= playerStart) {
         this.lastProcessedSlot = playerStart - 1;
         this.transferDelay = 3;
      }

   }

   private void disconnectSequence() {
      this.releaseMovementKeys();
      this.stopBreaking();
      this.setSneaking(false);
      this.sendWebhook();
      if (this.mc.world != null) {
         this.mc.world.disconnect();
      }

      this.toggle();
   }

   private void lookAt(BlockPos pos) {
      Vec3d eye = this.mc.player.getEyePos();
      Vec3d target = Vec3d.ofCenter(pos);
      Vec3d diff = target.subtract(eye);
      double h = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
      float yaw = (float)(Math.toDegrees(Math.atan2(diff.z, diff.x)) - (double)90.0F);
      float pitch = (float)(-Math.toDegrees(Math.atan2(diff.y, h)));
      this.mc.player.setYaw(yaw);
      this.mc.player.setPitch(pitch);
   }

   private void breakBlock(BlockPos pos) {
      if (this.mc.interactionManager != null) {
         this.mc.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
         KeyBinding.setKeyPressed(this.mc.options.attackKey.getDefaultKey(), true);
      }

   }

   private void stopBreaking() {
      KeyBinding.setKeyPressed(this.mc.options.attackKey.getDefaultKey(), false);
   }

   private void setSneaking(boolean flag) {
      if (this.mc.player != null && this.mc.getNetworkHandler() != null) {
         if (flag && !this.mc.player.isSneaking()) {
            this.mc.player.setSneaking(true);
            this.mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
         } else if (!flag && this.mc.player.isSneaking()) {
            this.mc.player.setSneaking(false);
            this.mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(this.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
         }

      }
   }

   private void releaseMovementKeys() {
      KeyBinding.setKeyPressed(this.mc.options.forwardKey.getDefaultKey(), false);
      KeyBinding.setKeyPressed(this.mc.options.jumpKey.getDefaultKey(), false);
   }

   private void sendWebhook() {
      if (this.webhook.getValue()) {
         String url = this.webhookUrl.getValue().trim();
         if (url.isEmpty()) {
            this.info("Webhook disabled (empty URL).");
         } else {
            String mention = "";
            if (this.selfPing.getValue() && !this.discordId.getValue().trim().isEmpty()) {
               mention = "<@" + this.discordId.getValue().trim() + ">";
            }

            String title = this.emergencyDisconnect ? "SpawnerProtect Emergency" : "SpawnerProtect Alert";
            String desc;
            if (this.emergencyDisconnect) {
               desc = "**Player:** " + this.detectedPlayer + "\\n**Reason:** " + this.emergencyReason + "\\n**Spawners Mined:** " + (this.minedSpawners ? "✅" : "❌") + "\\n**Items Deposited:** " + (this.depositedItems ? "✅" : "❌") + "\\n**Disconnected:** Yes";
            } else {
               desc = "**Player:** " + this.detectedPlayer + "\\n**Spawners Mined:** " + (this.minedSpawners ? "✅" : "❌") + "\\n**Items Deposited:** " + (this.depositedItems ? "✅" : "❌") + "\\n**Disconnected:** Yes";
            }

            String json = "{\n  \"username\":\"SpawnerProtect\",\n  \"content\":\"%s\",\n  \"embeds\":[{\n    \"title\":\"%s\",\n    \"description\":\"%s\",\n    \"color\":%d,\n    \"timestamp\":\"%s\",\n    \"footer\":{\"text\":\"SpawnerProtect Phantom\"}\n  }]\n}\n".formatted(this.escape(mention), this.escape(title), this.escape(desc), this.emergencyDisconnect ? 16711680 : 16766720, Instant.now());
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json").POST(BodyPublishers.ofString(json)).build();
            this.httpClient.sendAsync(req, BodyHandlers.ofString()).thenAccept((r) -> {
               if (r.statusCode() >= 200 && r.statusCode() < 300) {
                  this.info("Webhook sent.");
               } else {
                  this.info("Webhook failure: " + r.statusCode());
               }

            }).exceptionally((ex) -> {
               this.info("Webhook error: " + ex.getMessage());
               return null;
            });
         }
      }
   }

   private String escape(String s) {
      return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
   }

   private void info(String s) {
      if (this.mc.player != null) {
         this.mc.player.sendMessage(Text.literal("§a[SP] " + s), false);
      }

   }

   public String getInfo() {
      if (this.detectedPlayer.isEmpty()) {
         return "Idle";
      } else {
         String var10000 = this.detectedPlayer;
         return var10000 + " " + this.state.name();
      }
   }

   private static enum State {
      IDLE,
      GOING_TO_SPAWNERS,
      WAITING_CONFIRM,
      GOING_TO_CHEST,
      OPENING_CHEST,
      DEPOSITING,
      DISCONNECTING,
      POSITION_DISPLACED;

      // $FF: synthetic method
      private static State[] $values() {
         return new State[]{IDLE, GOING_TO_SPAWNERS, WAITING_CONFIRM, GOING_TO_CHEST, OPENING_CHEST, DEPOSITING, DISCONNECTING, POSITION_DISPLACED};
      }
   }
}
