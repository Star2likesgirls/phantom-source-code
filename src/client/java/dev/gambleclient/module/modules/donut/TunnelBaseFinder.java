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
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.HitResult;
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
import net.minecraft.client.texture.Scaling;
import net.minecraft.client.realms.gui.RealmsWorldSlotButton;

public final class TunnelBaseFinder extends Module {
   private final NumberSetting minimumStorage = new NumberSetting(EncryptedString.of("Minimum Storage"), (double)1.0F, (double)500.0F, (double)100.0F, (double)1.0F);
   private final BooleanSetting spawners = new BooleanSetting(EncryptedString.of("Spawners"), true);
   private final BooleanSetting autoTotemBuy = new BooleanSetting(EncryptedString.of("Auto Totem Buy"), true);
   private final NumberSetting totemSlot = new NumberSetting(EncryptedString.of("Totem Slot"), (double)1.0F, (double)9.0F, (double)8.0F, (double)1.0F);
   private final BooleanSetting autoMend = (new BooleanSetting(EncryptedString.of("Auto Mend"), true)).setDescription(EncryptedString.of("Automatically repairs pickaxe."));
   private final NumberSetting xpBottleSlot = new NumberSetting(EncryptedString.of("XP Bottle Slot"), (double)1.0F, (double)9.0F, (double)9.0F, (double)1.0F);
   private final BooleanSetting discordNotification = new BooleanSetting(EncryptedString.of("Discord Notification"), false);
   private final StringSetting webhook = new StringSetting(EncryptedString.of("Webhook"), "");
   private final BooleanSetting totemCheck = new BooleanSetting(EncryptedString.of("Totem Check"), true);
   private final NumberSetting totemCheckTime = new NumberSetting(EncryptedString.of("Totem Check Time"), (double)1.0F, (double)120.0F, (double)20.0F, (double)1.0F);
   private Direction currentDirection;
   private int blocksMined;
   private int spawnerCount;
   private int idleTicks;
   private Vec3d lastPosition;
   private boolean isDigging = false;
   private boolean shouldDig = false;
   private int totemCheckCounter = 0;
   private int totemBuyCounter = 0;
   private double actionDelay = (double)0.0F;

   public TunnelBaseFinder() {
      super(EncryptedString.of("Tunnel Base Finder"), EncryptedString.of("Finds bases digging tunnel"), -1, Category.DONUT);
      this.addSettings(new Setting[]{this.minimumStorage, this.spawners, this.autoTotemBuy, this.totemSlot, this.autoMend, this.xpBottleSlot, this.discordNotification, this.webhook, this.totemCheck, this.totemCheckTime});
   }

   public void onEnable() {
      super.onEnable();
      this.currentDirection = this.getInitialDirection();
      this.blocksMined = 0;
      this.idleTicks = 0;
      this.spawnerCount = 0;
      this.lastPosition = null;
   }

   public void onDisable() {
      super.onDisable();
      this.mc.options.leftKey.setPressed(false);
      this.mc.options.rightKey.setPressed(false);
      this.mc.options.forwardKey.setPressed(false);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.currentDirection != null) {
         Module moduleByClass = Gamble.INSTANCE.MODULE_MANAGER.getModuleByClass(AutoEat.class);
         if (!moduleByClass.isEnabled() || !((AutoEat)moduleByClass).shouldEat()) {
            int n = (this.calculateDirection(this.currentDirection) + 90 * this.idleTicks) % 360;
            if (this.mc.player.getYaw() != (float)n) {
               this.mc.player.setYaw((float)n);
            }

            if (this.mc.player.getPitch() != 2.0F) {
               this.mc.player.setPitch(2.0F);
            }

            this.updateDirection(this.getInitialDirection());
            if (this.blocksMined > 0) {
               this.mc.options.forwardKey.setPressed(false);
               --this.blocksMined;
            } else {
               this.notifyFound();
               if (this.autoTotemBuy.getValue()) {
                  int n2 = this.totemSlot.getIntValue() - 1;
                  if (!this.mc.player.getInventory().getStack(n2).isOf(Items.TOTEM_OF_UNDYING)) {
                     if (this.totemBuyCounter < 30 && !this.shouldDig) {
                        ++this.totemBuyCounter;
                        return;
                     }

                     this.totemBuyCounter = 0;
                     this.shouldDig = true;
                     if (this.mc.player.getInventory().selectedSlot != n2) {
                        InventoryUtil.swap(n2);
                     }

                     ScreenHandler currentScreenHandler = this.mc.player.currentScreenHandler;
                     if (this.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler && ((GenericContainerScreenHandler)currentScreenHandler).getRows() == 3) {
                        if (currentScreenHandler.getSlot(11).getStack().isOf(Items.END_STONE)) {
                           this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 13, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                           this.blocksMined = 10;
                           return;
                        }

                        if (currentScreenHandler.getSlot(16).getStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                           this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 13, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                           this.blocksMined = 10;
                           return;
                        }

                        this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.DROP_ALL_ITEMS, BlockPos.ORIGIN, Direction.DOWN));
                        if (currentScreenHandler.getSlot(23).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
                           this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 23, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                           this.blocksMined = 10;
                           return;
                        }

                        this.mc.getNetworkHandler().sendChatCommand("shop");
                        this.blocksMined = 10;
                        return;
                     }

                     this.mc.getNetworkHandler().sendChatCommand("shop");
                     this.blocksMined = 10;
                     return;
                  }

                  if (this.shouldDig) {
                     if (this.mc.currentScreen != null) {
                        this.mc.player.closeHandledScreen();
                        this.blocksMined = 20;
                     }

                     this.shouldDig = false;
                     this.totemBuyCounter = 0;
                  }
               }

               if (this.isDigging) {
                  int n3 = this.xpBottleSlot.getIntValue() - 1;
                  ItemStack getStack = this.mc.player.getInventory().getStack(n3);
                  if (this.mc.player.getInventory().selectedSlot != n3) {
                     InventoryUtil.swap(n3);
                  }

                  if (!getStack.isOf(Items.EXPERIENCE_BOTTLE)) {
                     ScreenHandler fishHook = this.mc.player.currentScreenHandler;
                     if (!(this.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) || ((GenericContainerScreenHandler)fishHook).getRows() != 3) {
                        this.mc.getNetworkHandler().sendChatCommand("shop");
                        this.blocksMined = 10;
                        return;
                     }

                     if (fishHook.getSlot(11).getStack().isOf(Items.END_STONE)) {
                        this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 13, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                        this.blocksMined = 10;
                        return;
                     }

                     if (fishHook.getSlot(16).getStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                        this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 16, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                        this.blocksMined = 10;
                        return;
                     }

                     if (fishHook.getSlot(17).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
                        this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 17, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                        this.blocksMined = 10;
                        return;
                     }

                     this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.DROP_ALL_ITEMS, BlockPos.ORIGIN, Direction.DOWN));
                     if (fishHook.getSlot(23).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
                        this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 23, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                        this.blocksMined = 10;
                        return;
                     }

                     this.mc.getNetworkHandler().sendChatCommand("shop");
                     this.blocksMined = 10;
                  } else {
                     if (this.mc.currentScreen != null) {
                        this.mc.player.closeHandledScreen();
                        this.blocksMined = 20;
                        return;
                     }

                     if (!EnchantmentUtil.hasEnchantment(this.mc.player.getOffHandStack(), Enchantments.MENDING)) {
                        this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 36 + this.totemCheckCounter, 40, net.minecraft.screen.slot.SlotActionType.SWAP, this.mc.player);
                        this.blocksMined = 20;
                        return;
                     }

                     if (this.mc.player.getOffHandStack().getDamage() > 0) {
                        ActionResult interactItem = this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
                        if (interactItem.isAccepted() && interactItem.shouldSwingHand()) {
                           this.mc.player.swingHand(Hand.MAIN_HAND);
                        }

                        this.blocksMined = 1;
                        return;
                     }

                     this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 36 + this.totemCheckCounter, 40, net.minecraft.screen.slot.SlotActionType.SWAP, this.mc.player);
                     this.isDigging = false;
                  }
               } else {
                  if (this.autoMend.getValue()) {
                     ItemStack size = this.mc.player.getMainHandStack();
                     if (EnchantmentUtil.hasEnchantment(size, Enchantments.MENDING) && size.getMaxDamage() - size.getDamage() < 100) {
                        this.isDigging = true;
                        this.totemCheckCounter = this.mc.player.getInventory().selectedSlot;
                     }
                  }

                  if (this.totemCheck.getValue()) {
                     boolean equals = this.mc.player.getOffHandStack().getItem().equals(Items.TOTEM_OF_UNDYING);
                     Module moduleByClass2 = Gamble.INSTANCE.MODULE_MANAGER.getModuleByClass(AutoTotem.class);
                     if (equals) {
                        this.actionDelay = (double)0.0F;
                     } else if (moduleByClass2.isEnabled() && ((AutoTotem)moduleByClass2).findItemSlot(Items.TOTEM_OF_UNDYING) != -1) {
                        this.actionDelay = (double)0.0F;
                     } else {
                        ++this.actionDelay;
                     }

                     if (this.actionDelay > this.totemCheckTime.getValue()) {
                        this.notifyTotemExploded("Your totem exploded", (int)this.mc.player.getX(), (int)this.mc.player.getY(), (int)this.mc.player.getZ());
                        return;
                     }
                  }

                  boolean a = false;
                  HitResult crosshairTarget = this.mc.crosshairTarget;
                  if (this.mc.crosshairTarget instanceof BlockHitResult) {
                     BlockPos blockPos = ((BlockHitResult)crosshairTarget).getBlockPos();
                     if (!BlockUtil.isBlockAtPosition(blockPos, Blocks.AIR)) {
                        a = this.isBlockPositionValid(blockPos, this.mc.player.getHorizontalFacing());
                     }
                  }

                  if (a) {
                     this.handleBlockBreaking(true);
                  }

                  boolean a2 = this.isBlockInDirection(this.mc.player.getHorizontalFacing(), 3);
                  boolean b = false;
                  HitResult crosshairTarget2 = this.mc.crosshairTarget;
                  if (this.mc.crosshairTarget instanceof BlockHitResult) {
                     b = this.mc.player.getCameraPosVec(1.0F).distanceTo(Vec3d.ofCenter(((BlockHitResult)crosshairTarget2).getBlockPos())) > (double)3.0F;
                  }

                  if (!a && (!b || !a2)) {
                     ++this.idleTicks;
                     this.lastPosition = this.mc.player.getPos();
                     this.blocksMined = 5;
                     return;
                  }

                  this.mc.options.forwardKey.setPressed(a2 && b);
                  if (this.idleTicks > 0 && this.lastPosition != null && this.mc.player.getPos().distanceTo(this.lastPosition) > (double)1.0F) {
                     this.lastPosition = this.mc.player.getPos();
                     Direction rotateYCounterclockwise = this.mc.player.getHorizontalFacing().rotateYCounterclockwise();
                     BlockPos blockPos2 = this.mc.player.getBlockPos().up().offset(rotateYCounterclockwise);

                     for(int i = 0; i < 5; ++i) {
                        blockPos2 = blockPos2.offset(rotateYCounterclockwise);
                        if (!this.mc.world.getBlockState(blockPos2).getBlock().equals(Blocks.AIR)) {
                           if (this.isBlockPositionValid(blockPos2, rotateYCounterclockwise) && this.isBlockPositionValid(blockPos2.offset(rotateYCounterclockwise), rotateYCounterclockwise)) {
                              --this.idleTicks;
                              this.blocksMined = 5;
                           }

                           return;
                        }
                     }
                  }
               }

            }
         }
      }
   }

   private int calculateDirection(Direction enum4) {
      if (enum4 == Direction.NORTH) {
         return 180;
      } else if (enum4 == Direction.SOUTH) {
         return 0;
      } else if (enum4 == Direction.EAST) {
         return 270;
      } else {
         return enum4 == Direction.WEST ? 90 : Math.round(this.mc.player.getYaw());
      }
   }

   private boolean isBlockInDirection(Direction direction, int n) {
      BlockPos down = this.mc.player.getBlockPos().down();
      BlockPos getBlockPos = this.mc.player.getBlockPos();

      for(int i = 0; i < n; ++i) {
         BlockPos offset = down.offset(direction, i);
         BlockPos offset2 = getBlockPos.offset(direction, i);
         if (this.mc.world.getBlockState(offset).isAir() || !this.isBlockPositionSafe(offset)) {
            return false;
         }

         if (!this.mc.world.getBlockState(offset2).isAir()) {
            return false;
         }
      }

      return true;
   }

   private boolean isBlockPositionValid(BlockPos blockPos, Direction direction) {
      BlockPos offset = blockPos.offset(direction);
      Direction rotateYClockwise = direction.rotateYClockwise();
      Direction up = Direction.UP;
      BlockPos offset2 = blockPos.offset(Direction.UP, 2);
      BlockPos offset3 = blockPos.offset(Direction.DOWN, -2);
      BlockPos offset4 = offset2.offset(rotateYClockwise, -1);
      if (this.isBlockPositionSafe(offset4) && this.mc.world.getBlockState(offset4).getBlock() != Blocks.GRAVEL) {
         if (!this.isBlockPositionSafe(offset3.offset(rotateYClockwise, -1))) {
            return false;
         } else {
            BlockPos offset5 = blockPos.offset(rotateYClockwise, 2);
            BlockPos offset6 = blockPos.offset(rotateYClockwise, -2);
            if (!this.isBlockPositionSafe(offset5.offset(up, -1))) {
               return false;
            } else if (!this.isBlockPositionSafe(offset6.offset(up, -1))) {
               return false;
            } else {
               while(this.isBlockPositionSafe(offset.offset(rotateYClockwise, -1).offset(up, -1))) {
               }

               return false;
            }
         }
      } else {
         return false;
      }
   }

   private boolean isBlockPositionSafe(BlockPos blockPos) {
      return this.isBlockValid(this.mc.world.getBlockState(blockPos).getBlock());
   }

   private boolean isBlockValid(Block block) {
      return block != Blocks.LAVA && block != Blocks.WATER;
   }

   private void updateDirection(Direction enum4) {
      double getX = this.mc.player.getX();
      double getY = this.mc.player.getZ();
      double floor = Math.floor(getY);
      double n = Math.floor(getX) + (double)0.5F - getX;
      double n2 = floor + (double)0.5F - getY;
      this.mc.options.leftKey.setPressed(false);
      this.mc.options.rightKey.setPressed(false);
      boolean b = false;
      boolean b2 = false;
      if (enum4 == Direction.SOUTH) {
         if (n > 0.1) {
            b2 = true;
         } else if (n < -0.1) {
            b = true;
         }
      }

      if (enum4 == Direction.NORTH) {
         if (n > 0.1) {
            b = true;
         } else if (n < -0.1) {
            b2 = true;
         }
      }

      if (enum4 == Direction.WEST) {
         if (n2 > 0.1) {
            b2 = true;
         } else if (n2 < -0.1) {
            b = true;
         }
      }

      if (enum4 == Direction.EAST) {
         if (n2 > 0.1) {
            b = true;
         } else if (n2 < -0.1) {
            b2 = true;
         }
      }

      if (b) {
         this.mc.options.rightKey.setPressed(true);
      }

      if (b2) {
         this.mc.options.leftKey.setPressed(true);
      }

   }

   private void handleBlockBreaking(boolean b) {
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

   private Direction getInitialDirection() {
      float n = this.mc.player.getYaw() % 360.0F;
      if (n < 0.0F) {
         n += 360.0F;
      }

      if (n >= 45.0F && n < 135.0F) {
         return Direction.WEST;
      } else if (n >= 135.0F && n < 225.0F) {
         return Direction.NORTH;
      } else {
         return n >= 225.0F && n < 315.0F ? Direction.EAST : Direction.SOUTH;
      }
   }

   private void notifyFound() {
      int n = 0;
      int n2 = 0;
      BlockPos blockPos = null;
      Iterator iterator = BlockUtil.getLoadedChunks().iterator();

      while(iterator.hasNext()) {
         for(Object next : ((WorldChunk)iterator.next()).getBlockEntityPositions()) {
            BlockEntity getBlockEntity = this.mc.world.getBlockEntity((BlockPos)next);
            if (this.spawners.getValue() && getBlockEntity instanceof MobSpawnerBlockEntity) {
               String string = ((MobSpawnerLogicAccessor)((MobSpawnerBlockEntity)getBlockEntity).getLogic()).getSpawnEntry().getNbt().getString("id");
               if (string != "minecraft:cave_spider" && string != "minecraft:spider") {
                  ++n2;
                  blockPos = (BlockPos)next;
               }
            }

            if (!(getBlockEntity instanceof ChestBlockEntity) && !(getBlockEntity instanceof EnderChestBlockEntity)) {
               if (getBlockEntity instanceof ShulkerBoxBlockEntity) {
                  throw new RuntimeException();
               }

               if (!(getBlockEntity instanceof FurnaceBlockEntity) && !(getBlockEntity instanceof BarrelBlockEntity) && !(getBlockEntity instanceof EnchantingTableBlockEntity)) {
                  continue;
               }
            }

            ++n;
         }
      }

      if (n2 > 0) {
         ++this.spawnerCount;
      } else {
         this.spawnerCount = 0;
      }

      if (this.spawnerCount > 10) {
         this.notifyFound("YOU FOUND SPAWNER", blockPos.getX(), blockPos.getY(), blockPos.getZ(), false);
         this.spawnerCount = 0;
      }

      if (n > this.minimumStorage.getIntValue()) {
         this.notifyFound("YOU FOUND BASE", (int)this.mc.player.getPos().x, (int)this.mc.player.getPos().y, (int)this.mc.player.getPos().z, true);
      }

   }

   private void notifyTotemExploded(String s, int n, int n2, int n3) {
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

   private void notifyFound(String s, int n, int n2, int n3, boolean b) {
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

   private void disconnectWithMessage(Text text) {
      MutableText literal = Text.literal("[TunnelBaseFinder] ");
      literal.append(text);
      this.toggle();
      this.mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(literal));
   }

   public boolean isDigging() {
      return this.isDigging;
   }
}
