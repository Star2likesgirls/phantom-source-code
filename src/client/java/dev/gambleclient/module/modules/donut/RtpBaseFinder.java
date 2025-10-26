package dev.gambleclient.module.modules.donut;

import dev.gambleclient.Gamble;
import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.mixin.MobSpawnerLogicAccessor;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.modules.combat.AutoTotem;
import dev.gambleclient.module.modules.misc.AutoEat;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.ModeSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.module.setting.StringSetting;
import dev.gambleclient.utils.BlockUtil;
import dev.gambleclient.utils.EnchantmentUtil;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.InventoryUtil;
import dev.gambleclient.utils.embed.DiscordWebhook;
import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Random;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.text.MutableText;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.texture.Scaling;
import net.minecraft.client.realms.gui.RealmsWorldSlotButton;

public final class RtpBaseFinder extends Module {
   public final ModeSetting mode;
   private final BooleanSetting spawn;
   private final NumberSetting minStorage;
   private final BooleanSetting autoTotemBuy;
   private final NumberSetting totemSlot;
   private final BooleanSetting autoMend;
   private final NumberSetting xpBottleSlot;
   private final BooleanSetting discordNotification;
   private final StringSetting webhook;
   private final BooleanSetting totemCheck;
   private final NumberSetting totemCheckTime;
   private final NumberSetting digToY;
   private Vec3d currentPosition;
   private Vec3d previousPosition;
   private double idleTime;
   private double totemCheckCounter;
   private boolean isDigging;
   private boolean shouldDig;
   private boolean isRepairing;
   private boolean isBuyingTotem;
   private int selectedSlot;
   private int rtpCooldown;
   private int actionDelay;
   private int totemBuyCounter;
   private int spawnerCounter;

   public RtpBaseFinder() {
      super(EncryptedString.of("Rtp Base Finder"), EncryptedString.of("Automatically searches for bases on DonutSMP"), -1, Category.DONUT);
      this.mode = new ModeSetting(EncryptedString.of("Mode"), RtpBaseFinder.Mode.RANDOM, Mode.class);
      this.spawn = new BooleanSetting(EncryptedString.of("Spawners"), true);
      this.minStorage = new NumberSetting(EncryptedString.of("Minimum Storage"), (double)1.0F, (double)500.0F, (double)100.0F, (double)1.0F);
      this.autoTotemBuy = new BooleanSetting(EncryptedString.of("Auto Totem Buy"), true);
      this.totemSlot = new NumberSetting(EncryptedString.of("Totem Slot"), (double)1.0F, (double)9.0F, (double)8.0F, (double)1.0F);
      this.autoMend = (new BooleanSetting(EncryptedString.of("Auto Mend"), true)).setDescription(EncryptedString.of("Automatically repairs pickaxe."));
      this.xpBottleSlot = new NumberSetting(EncryptedString.of("XP Bottle Slot"), (double)1.0F, (double)9.0F, (double)9.0F, (double)1.0F);
      this.discordNotification = new BooleanSetting(EncryptedString.of("Discord Notification"), false);
      this.webhook = new StringSetting(EncryptedString.of("Webhook"), "");
      this.totemCheck = new BooleanSetting(EncryptedString.of("Totem Check"), true);
      this.totemCheckTime = new NumberSetting(EncryptedString.of("Totem Check Time"), (double)1.0F, (double)120.0F, (double)20.0F, (double)1.0F);
      this.digToY = new NumberSetting(EncryptedString.of("Dig To Y"), (double)-59.0F, (double)30.0F, (double)-20.0F, (double)1.0F);
      this.totemCheckCounter = (double)0.0F;
      this.isDigging = false;
      this.shouldDig = false;
      this.isRepairing = false;
      this.isBuyingTotem = false;
      this.selectedSlot = 0;
      this.rtpCooldown = 0;
      this.actionDelay = 0;
      this.totemBuyCounter = 0;
      this.spawnerCounter = 0;
      this.addSettings(new Setting[]{this.mode, this.spawn, this.minStorage, this.autoTotemBuy, this.totemSlot, this.autoMend, this.xpBottleSlot, this.discordNotification, this.webhook, this.totemCheck, this.totemCheckTime, this.digToY});
   }

   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.mc.player != null) {
         if (this.actionDelay > 0) {
            --this.actionDelay;
         } else {
            this.scanForEntities();
            if (this.autoTotemBuy.getValue()) {
               int n = this.totemSlot.getIntValue() - 1;
               if (!this.mc.player.getInventory().getStack(n).isOf(Items.TOTEM_OF_UNDYING)) {
                  if (this.totemBuyCounter < 30 && !this.isBuyingTotem) {
                     ++this.totemBuyCounter;
                     return;
                  }

                  this.totemBuyCounter = 0;
                  this.isBuyingTotem = true;
                  if (this.mc.player.getInventory().selectedSlot != n) {
                     InventoryUtil.swap(n);
                  }

                  ScreenHandler currentScreenHandler = this.mc.player.currentScreenHandler;
                  if (this.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler && ((GenericContainerScreenHandler)currentScreenHandler).getRows() == 3) {
                     if (currentScreenHandler.getSlot(11).getStack().isOf(Items.END_STONE)) {
                        this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 13, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                        this.actionDelay = 10;
                        return;
                     }

                     if (currentScreenHandler.getSlot(16).getStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                        this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 13, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                        this.actionDelay = 10;
                        return;
                     }

                     this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.DROP_ALL_ITEMS, BlockPos.ORIGIN, Direction.DOWN));
                     if (currentScreenHandler.getSlot(23).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
                        this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 23, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                        this.actionDelay = 10;
                        return;
                     }

                     this.mc.getNetworkHandler().sendChatCommand("shop");
                     this.actionDelay = 10;
                     return;
                  }

                  this.mc.getNetworkHandler().sendChatCommand("shop");
                  this.actionDelay = 10;
                  return;
               }

               if (this.isBuyingTotem) {
                  if (this.mc.currentScreen != null) {
                     this.mc.player.closeHandledScreen();
                     this.actionDelay = 20;
                  }

                  this.isBuyingTotem = false;
                  this.totemBuyCounter = 0;
               }
            }

            if (this.isRepairing) {
               int n2 = this.xpBottleSlot.getIntValue() - 1;
               ItemStack getStack = this.mc.player.getInventory().getStack(n2);
               if (this.mc.player.getInventory().selectedSlot != n2) {
                  InventoryUtil.swap(n2);
               }

               if (!getStack.isOf(Items.EXPERIENCE_BOTTLE)) {
                  ScreenHandler fishHook = this.mc.player.currentScreenHandler;
                  if (!(this.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) || ((GenericContainerScreenHandler)fishHook).getRows() != 3) {
                     this.mc.getNetworkHandler().sendChatCommand("shop");
                     this.actionDelay = 10;
                     return;
                  }

                  if (fishHook.getSlot(11).getStack().isOf(Items.END_STONE)) {
                     this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 13, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                     this.actionDelay = 10;
                     return;
                  }

                  if (fishHook.getSlot(16).getStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                     this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 16, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                     this.actionDelay = 10;
                     return;
                  }

                  if (fishHook.getSlot(17).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
                     this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 17, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                     this.actionDelay = 10;
                     return;
                  }

                  this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.DROP_ALL_ITEMS, BlockPos.ORIGIN, Direction.DOWN));
                  if (fishHook.getSlot(23).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
                     this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 23, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                     this.actionDelay = 10;
                     return;
                  }

                  this.mc.getNetworkHandler().sendChatCommand("shop");
                  this.actionDelay = 10;
               } else {
                  if (this.mc.currentScreen != null) {
                     this.mc.player.closeHandledScreen();
                     this.actionDelay = 20;
                     return;
                  }

                  if (!EnchantmentUtil.hasEnchantment(this.mc.player.getOffHandStack(), Enchantments.MENDING)) {
                     this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 36 + this.selectedSlot, 40, net.minecraft.screen.slot.SlotActionType.SWAP, this.mc.player);
                     this.actionDelay = 20;
                     return;
                  }

                  if (this.mc.player.getOffHandStack().getDamage() > 0) {
                     ActionResult interactItem = this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
                     if (interactItem.isAccepted() && interactItem.shouldSwingHand()) {
                        this.mc.player.swingHand(Hand.MAIN_HAND);
                     }

                     this.actionDelay = 1;
                     return;
                  }

                  this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 36 + this.selectedSlot, 40, net.minecraft.screen.slot.SlotActionType.SWAP, this.mc.player);
                  this.isRepairing = false;
               }
            } else {
               if (this.shouldDig) {
                  this.handleAutoEat();
               }

               if (this.totemCheck.getValue()) {
                  boolean equals = this.mc.player.getOffHandStack().getItem().equals(Items.TOTEM_OF_UNDYING);
                  Module moduleByClass = Gamble.INSTANCE.MODULE_MANAGER.getModuleByClass(AutoTotem.class);
                  if (equals) {
                     this.totemCheckCounter = (double)0.0F;
                  } else if (moduleByClass.isEnabled() && ((AutoTotem)moduleByClass).findItemSlot(Items.TOTEM_OF_UNDYING) != -1) {
                     this.totemCheckCounter = (double)0.0F;
                  } else {
                     ++this.totemCheckCounter;
                  }

                  if (this.totemCheckCounter > this.totemCheckTime.getValue()) {
                     this.notifyTotemExplosion("Your totem exploded", (int)this.mc.player.getX(), (int)this.mc.player.getY(), (int)this.mc.player.getZ());
                     return;
                  }
               }

               if (this.rtpCooldown > 0) {
                  --this.rtpCooldown;
                  if (this.rtpCooldown < 1) {
                     if (this.previousPosition != null && this.previousPosition.distanceTo(this.mc.player.getPos()) < (double)100.0F) {
                        this.sendRtpCommand();
                        return;
                     }

                     this.mc.player.setPitch(89.9F);
                     if (this.autoMend.getValue()) {
                        ItemStack size = this.mc.player.getMainHandStack();
                        if (EnchantmentUtil.hasEnchantment(size, Enchantments.MENDING) && size.getMaxDamage() - size.getDamage() < 100) {
                           this.isRepairing = true;
                           this.selectedSlot = this.mc.player.getInventory().selectedSlot;
                        }
                     }

                     this.shouldDig = true;
                  }

                  return;
               }

               if (this.currentPosition != null && this.currentPosition.distanceTo(this.mc.player.getPos()) < (double)2.0F) {
                  ++this.idleTime;
               } else {
                  this.currentPosition = this.mc.player.getPos();
                  this.idleTime = (double)0.0F;
               }

               if (this.idleTime > (double)20.0F && this.isDigging) {
                  this.sendRtpCommand();
                  this.isDigging = false;
                  return;
               }

               if (this.idleTime > (double)200.0F) {
                  this.sendRtpCommand();
                  this.idleTime = (double)0.0F;
                  return;
               }

               if (this.mc.player.getY() < (double)this.digToY.getIntValue() && !this.isDigging) {
                  this.isDigging = true;
                  this.shouldDig = false;
               }
            }

         }
      }
   }

   private void sendRtpCommand() {
      this.shouldDig = false;
      ClientPlayNetworkHandler networkHandler = this.mc.getNetworkHandler();
      Mode l;
      if (this.mode.getValue() == RtpBaseFinder.Mode.RANDOM) {
         l = this.getRandomMode();
      } else {
         l = (Mode)this.mode.getValue();
      }

      String var10001 = this.getModeName(l);
      networkHandler.sendChatCommand("rtp " + var10001);
      this.rtpCooldown = 150;
      this.idleTime = (double)0.0F;
      this.previousPosition = new Vec3d(this.mc.player.getPos().toVector3f());
   }

   private void disconnectWithMessage(Text text) {
      MutableText literal = Text.literal("[RTPBaseFinder] ");
      literal.append(text);
      this.toggle();
      this.mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(literal));
   }

   private Mode getRandomMode() {
      Mode[] array = new Mode[]{RtpBaseFinder.Mode.EUCENTRAL, RtpBaseFinder.Mode.EUWEST, RtpBaseFinder.Mode.EAST, RtpBaseFinder.Mode.WEST, RtpBaseFinder.Mode.ASIA, RtpBaseFinder.Mode.OCEANIA};
      return array[(new Random()).nextInt(array.length)];
   }

   private String getModeName(Mode mode) {
      int n = mode.ordinal() ^ 1886013532;
      int n2;
      if (n != 0) {
         n2 = (n * 31 >>> 4) % n ^ n >>> 16;
      } else {
         n2 = 0;
      }

      String name = null;
      switch (n2) {
         case 164469848 -> name = "eu central";
         case 164469854 -> name = "eu west";
         default -> name = mode.name();
      }

      return name;
   }

   private void handleAutoEat() {
      Module moduleByClass = Gamble.INSTANCE.MODULE_MANAGER.getModuleByClass(AutoEat.class);
      if (!moduleByClass.isEnabled()) {
         this.handleBlockBreaking(true);
      } else if (!((AutoEat)moduleByClass).shouldEat()) {
         this.handleBlockBreaking(true);
      }
   }

   private void handleBlockBreaking(boolean b) {
      if (this.mc.player.getPitch() != 89.9F) {
         this.mc.player.setPitch(89.9F);
      }

      if (!this.mc.player.isUsingItem()) {
         if (b && this.mc.crosshairTarget != null && this.mc.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult)this.mc.crosshairTarget;
            BlockPos blockPos = ((BlockHitResult)this.mc.crosshairTarget).getBlockPos();
            if (!this.mc.world.getBlockState(blockPos).isAir()) {
               Direction side = blockHitResult.getSide();
               if (this.mc.interactionManager.updateBlockBreakingProgress(blockPos, side)) {
                  this.mc.particleManager.addBlockBreakingParticles(blockPos, side);
                  this.mc.player.swingHand(Hand.MAIN_HAND);
               }
            }
         } else {
            this.mc.interactionManager.cancelBlockBreaking();
         }
      }

   }

   private void scanForEntities() {
      int n = 0;
      int n2 = 0;
      BlockPos blockPos = null;
      Iterator iterator = BlockUtil.getLoadedChunks().iterator();

      while(iterator.hasNext()) {
         for(Object next : ((WorldChunk)iterator.next()).getBlockEntityPositions()) {
            BlockEntity getBlockEntity = this.mc.world.getBlockEntity((BlockPos)next);
            if (this.spawn.getValue() && getBlockEntity instanceof MobSpawnerBlockEntity) {
               String string = ((MobSpawnerLogicAccessor)((MobSpawnerBlockEntity)getBlockEntity).getLogic()).getSpawnEntry().getNbt().getString("id");
               if (string != "minecraft:cave_spider" && string != "minecraft:spider") {
                  ++n2;
                  blockPos = (BlockPos)next;
               }
            }

            if (getBlockEntity instanceof ChestBlockEntity || getBlockEntity instanceof EnderChestBlockEntity || getBlockEntity instanceof ShulkerBoxBlockEntity || getBlockEntity instanceof FurnaceBlockEntity || getBlockEntity instanceof BarrelBlockEntity || getBlockEntity instanceof EnchantingTableBlockEntity) {
               ++n;
            }
         }
      }

      if (n2 > 0) {
         ++this.spawnerCounter;
      } else {
         this.spawnerCounter = 0;
      }

      if (this.spawnerCounter > 10) {
         this.notifyBaseOrSpawner("YOU FOUND SPAWNER", blockPos.getX(), blockPos.getY(), blockPos.getZ(), false);
         this.spawnerCounter = 0;
      }

      if (n > this.minStorage.getIntValue()) {
         this.notifyBaseOrSpawner("YOU FOUND BASE", (int)this.mc.player.getPos().x, (int)this.mc.player.getPos().y, (int)this.mc.player.getPos().z, true);
      }

   }

   private void notifyBaseOrSpawner(String s, int n, int n2, int n3, boolean b) {
      String s2;
      if (b) {
         s2 = "Base";
      } else {
         s2 = "Spawner";
      }

      if (this.discordNotification.getValue()) {
         DiscordWebhook embedSender = new DiscordWebhook(this.webhook.value);
         DiscordWebhook.EmbedObject bn = new DiscordWebhook.EmbedObject();
         bn.setTitle(s2);
         bn.setThumbnail("https://render.crafty.gg/3d/bust/" + String.valueOf(MinecraftClient.getInstance().getSession().getUuidOrNull()) + "?format=webp");
         bn.setDescription(s2 + " Found - " + MinecraftClient.getInstance().getSession().getUsername());
         bn.setColor(Color.GRAY);
         bn.setFooter(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), (String)null);
         bn.addField(s2 + "Found at", "x: " + n + " y: " + n2 + " z: " + n3, true);
         embedSender.addEmbed(bn);

         try {
            embedSender.execute();
         } catch (Throwable var10) {
         }
      }

      this.toggle();
      this.disconnectWithMessage(Text.of(s));
   }

   private void notifyTotemExplosion(String s, int n, int n2, int n3) {
      if (this.discordNotification.getValue()) {
         DiscordWebhook embedSender = new DiscordWebhook(this.webhook.value);
         DiscordWebhook.EmbedObject bn = new DiscordWebhook.EmbedObject();
         bn.setTitle("Totem Exploded");
         bn.setThumbnail("https://render.crafty.gg/3d/bust/" + String.valueOf(MinecraftClient.getInstance().getSession().getUuidOrNull()) + "?format=webp");
         bn.setDescription("Your Totem Exploded - " + MinecraftClient.getInstance().getSession().getUsername());
         bn.setColor(Color.RED);
         bn.setFooter(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), (String)null);
         bn.addField("Location", "x: " + n + " y: " + n2 + " z: " + n3, true);
         embedSender.addEmbed(bn);

         try {
            embedSender.execute();
         } catch (Throwable var8) {
         }
      }

      this.disconnectWithMessage(Text.of(s));
   }

   public boolean isRepairingActive() {
      return this.isRepairing;
   }

   static enum Mode {
      EUCENTRAL("eucentral", 0),
      EUWEST("euwest", 1),
      EAST("east", 2),
      WEST("west", 3),
      ASIA("asia", 4),
      OCEANIA("oceania", 5),
      RANDOM("random", 6);

      private Mode(final String name, final int ordinal) {
      }

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{EUCENTRAL, EUWEST, EAST, WEST, ASIA, OCEANIA, RANDOM};
      }
   }
}
