package dev.gambleclient.module.modules.donut;

import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.module.setting.StringSetting;
import dev.gambleclient.utils.EncryptedString;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;

public class AutoShulkerOrder extends Module {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private Stage stage;
   private long stageStart;
   private static final long WAIT_TIME_MS = 50L;
   private int shulkerMoveIndex;
   private long lastShulkerMoveTime;
   private int exitCount;
   private int finalExitCount;
   private long finalExitStart;
   private int bulkBuyCount;
   private static final int MAX_BULK_BUY = 5;
   private String targetPlayer;
   private boolean isTargetingActive;
   private final StringSetting minPrice;
   private final BooleanSetting notifications;
   private final BooleanSetting speedMode;
   private final BooleanSetting enableTargeting;
   private final StringSetting targetPlayerName;
   private final BooleanSetting targetOnlyMode;

   public AutoShulkerOrder() {
      super(EncryptedString.of("AutoShulkerOrder"), EncryptedString.of("Automatically buys shulkers and sells them in orders for profit with player targeting"), -1, Category.DONUT);
      this.stage = AutoShulkerOrder.Stage.NONE;
      this.stageStart = 0L;
      this.shulkerMoveIndex = 0;
      this.lastShulkerMoveTime = 0L;
      this.exitCount = 0;
      this.finalExitCount = 0;
      this.finalExitStart = 0L;
      this.bulkBuyCount = 0;
      this.targetPlayer = "";
      this.isTargetingActive = false;
      this.minPrice = (new StringSetting(EncryptedString.of("Min Price"), "850")).setDescription(EncryptedString.of("Minimum price to deliver shulkers for (supports K, M, B suffixes)."));
      this.notifications = (new BooleanSetting(EncryptedString.of("Notifications"), true)).setDescription(EncryptedString.of("Show detailed price checking notifications."));
      this.speedMode = (new BooleanSetting(EncryptedString.of("Speed Mode"), true)).setDescription(EncryptedString.of("Maximum speed mode - removes most delays (may be unstable)."));
      this.enableTargeting = (new BooleanSetting(EncryptedString.of("Enable Targeting"), false)).setDescription(EncryptedString.of("Enable targeting a specific player (ignores minimum price)."));
      this.targetPlayerName = (new StringSetting(EncryptedString.of("Target Player"), "")).setDescription(EncryptedString.of("Specific player name to target for orders."));
      this.targetOnlyMode = (new BooleanSetting(EncryptedString.of("Target Only Mode"), false)).setDescription(EncryptedString.of("Only look for orders from the targeted player, ignore all others."));
      this.addSettings(new Setting[]{this.minPrice, this.notifications, this.speedMode, this.enableTargeting, this.targetPlayerName, this.targetOnlyMode});
   }

   public void onEnable() {
      double parsedPrice = this.parsePrice(this.minPrice.getValue());
      if (parsedPrice == (double)-1.0F && !this.enableTargeting.getValue()) {
         if (this.notifications.getValue()) {
            this.error("Invalid minimum price format!");
         }

         this.toggle(false);
      } else {
         this.updateTargetPlayer();
         this.stage = AutoShulkerOrder.Stage.SHOP;
         this.stageStart = System.currentTimeMillis();
         this.shulkerMoveIndex = 0;
         this.lastShulkerMoveTime = 0L;
         this.exitCount = 0;
         this.finalExitCount = 0;
         this.bulkBuyCount = 0;
         if (this.notifications.getValue()) {
            String modeInfo = this.isTargetingActive ? " | Targeting: " + this.targetPlayer : "";
            this.info("\ud83d\ude80 FAST AutoShulkerOrder activated! Minimum: %s%s", this.minPrice.getValue(), modeInfo);
         }

      }
   }

   public void onDisable() {
      this.stage = AutoShulkerOrder.Stage.NONE;
   }

   private void updateTargetPlayer() {
      this.targetPlayer = "";
      this.isTargetingActive = false;
      if (this.enableTargeting.getValue() && !this.targetPlayerName.getValue().trim().isEmpty()) {
         this.targetPlayer = this.targetPlayerName.getValue().trim();
         this.isTargetingActive = true;
         if (this.notifications.getValue()) {
            this.info("\ud83c\udfaf Targeting enabled for player: %s", this.targetPlayer);
         }
      } else if (this.notifications.getValue() && this.enableTargeting.getValue()) {
         this.info("⚠️ Targeting disabled - no player name provided");
      }

   }

   public void onTick() {
      if (this.mc.player != null && this.mc.world != null) {
         long now = System.currentTimeMillis();
         switch (this.stage.ordinal()) {
            case 0:
            default:
               break;
            case 1:
               this.mc.player.sendMessage(Text.of("/shop"));
               this.stage = AutoShulkerOrder.Stage.SHOP_END;
               this.stageStart = now;
               break;
            case 2:
               Screen var33 = this.mc.currentScreen;
               if (var33 instanceof GenericContainerScreen) {
                  GenericContainerScreen screen = (GenericContainerScreen)var33;
                  ScreenHandler handler = screen.getScreenHandler();

                  for(Slot slot : handler.slots) {
                     ItemStack stack = slot.getStack();
                     if (!stack.isEmpty() && this.isEndStone(stack)) {
                        this.mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                        this.stage = AutoShulkerOrder.Stage.SHOP_SHULKER;
                        this.stageStart = now;
                        this.bulkBuyCount = 0;
                        return;
                     }
                  }

                  if (now - this.stageStart > (long)(this.speedMode.getValue() ? 1000 : 3000)) {
                     this.mc.player.closeHandledScreen();
                     this.stage = AutoShulkerOrder.Stage.SHOP;
                     this.stageStart = now;
                  }
               }
               break;
            case 3:
               Screen var31 = this.mc.currentScreen;
               if (var31 instanceof GenericContainerScreen) {
                  GenericContainerScreen screen = (GenericContainerScreen)var31;
                  ScreenHandler handler = screen.getScreenHandler();
                  boolean foundShulker = false;

                  for(Slot slot : handler.slots) {
                     ItemStack stack = slot.getStack();
                     if (!stack.isEmpty() && this.isShulkerBox(stack)) {
                        int clickCount = this.speedMode.getValue() ? 10 : 5;

                        for(int i = 0; i < clickCount; ++i) {
                           this.mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                        }

                        foundShulker = true;
                        ++this.bulkBuyCount;
                        break;
                     }
                  }

                  if (foundShulker) {
                     this.stage = AutoShulkerOrder.Stage.SHOP_CONFIRM;
                     this.stageStart = now;
                     return;
                  }

                  if (now - this.stageStart > (long)(this.speedMode.getValue() ? 500 : 1500)) {
                     this.mc.player.closeHandledScreen();
                     this.stage = AutoShulkerOrder.Stage.SHOP;
                     this.stageStart = now;
                  }
               }
               break;
            case 4:
               Screen var29 = this.mc.currentScreen;
               if (var29 instanceof GenericContainerScreen) {
                  GenericContainerScreen screen = (GenericContainerScreen)var29;
                  ScreenHandler handler = screen.getScreenHandler();
                  boolean foundGreen = false;

                  for(Slot slot : handler.slots) {
                     ItemStack stack = slot.getStack();
                     if (!stack.isEmpty() && this.isGreenGlass(stack)) {
                        for(int i = 0; i < (this.speedMode.getValue() ? 3 : 2); ++i) {
                           this.mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                        }

                        foundGreen = true;
                        break;
                     }
                  }

                  if (foundGreen) {
                     this.stage = AutoShulkerOrder.Stage.SHOP_CHECK_FULL;
                     this.stageStart = now;
                     return;
                  }

                  if (now - this.stageStart > (long)(this.speedMode.getValue() ? 200 : 800)) {
                     this.stage = AutoShulkerOrder.Stage.SHOP_SHULKER;
                     this.stageStart = now;
                  }
               }
               break;
            case 5:
               if (now - this.stageStart > (long)(this.speedMode.getValue() ? 100 : 200)) {
                  if (this.isInventoryFull()) {
                     this.mc.player.closeHandledScreen();
                     this.stage = AutoShulkerOrder.Stage.SHOP_EXIT;
                     this.stageStart = now;
                  } else if (now - this.stageStart > (long)(this.speedMode.getValue() ? 200 : 400)) {
                     this.stage = AutoShulkerOrder.Stage.SHOP_SHULKER;
                     this.stageStart = now;
                  }
               }
               break;
            case 6:
               if (this.mc.currentScreen == null) {
                  this.stage = AutoShulkerOrder.Stage.WAIT;
                  this.stageStart = now;
               }

               if (now - this.stageStart > (long)(this.speedMode.getValue() ? 1000 : 5000)) {
                  this.mc.player.closeHandledScreen();
                  this.stage = AutoShulkerOrder.Stage.SHOP;
                  this.stageStart = now;
               }
               break;
            case 7:
               long waitTime = this.speedMode.getValue() ? 25L : 50L;
               if (now - this.stageStart >= waitTime) {
                  if (this.isTargetingActive && !this.targetPlayer.isEmpty()) {
                     this.stage = AutoShulkerOrder.Stage.TARGET_ORDERS;
                  } else {
                     this.mc.player.sendMessage(Text.of("/orders shulker"));
                     this.stage = AutoShulkerOrder.Stage.ORDERS;
                  }

                  this.stageStart = now;
               }
               break;
            case 8:
               Object screenObj = this.mc.currentScreen;
               if (screenObj instanceof GenericContainerScreen) {
                  GenericContainerScreen screen = (GenericContainerScreen)screenObj;
                  ScreenHandler handler = screen.getScreenHandler();
                  boolean foundOrder = false;
                  if (this.speedMode.getValue() && now - this.stageStart < 200L) {
                     return;
                  }

                  for(Slot slot : handler.slots) {
                     ItemStack stack = slot.getStack();
                     if (!stack.isEmpty() && this.isShulkerBox(stack) && this.isPurple(stack)) {
                        boolean shouldTakeOrder = false;
                        String orderPlayer = this.getOrderPlayerName(stack);
                        boolean isTargetedOrder = this.isTargetingActive && orderPlayer != null && orderPlayer.equalsIgnoreCase(this.targetPlayer);
                        if (isTargetedOrder) {
                           shouldTakeOrder = true;
                           if (this.notifications.getValue()) {
                              double orderPrice = this.getOrderPrice(stack);
                              this.info("\ud83c\udfaf Found TARGET order from %s: %s", orderPlayer, orderPrice > (double)0.0F ? this.formatPrice(orderPrice) : "Unknown price");
                           }
                        } else if (!this.targetOnlyMode.getValue()) {
                           double orderPrice = this.getOrderPrice(stack);
                           double minPriceValue = this.parsePrice(this.minPrice.getValue());
                           if (orderPrice >= minPriceValue) {
                              shouldTakeOrder = true;
                              if (this.notifications.getValue()) {
                                 this.info("✅ Found order: %s", this.formatPrice(orderPrice));
                              }
                           }
                        }

                        if (shouldTakeOrder) {
                           this.mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                           this.stage = AutoShulkerOrder.Stage.ORDERS_SELECT;
                           this.stageStart = now + (long)(this.speedMode.getValue() ? 100 : 50);
                           this.shulkerMoveIndex = 0;
                           this.lastShulkerMoveTime = 0L;
                           foundOrder = true;
                           if (this.notifications.getValue()) {
                              this.info("\ud83d\udd04 Selected order, preparing to transfer items...");
                           }

                           return;
                        }
                     }
                  }

                  if (!foundOrder && now - this.stageStart > (long)(this.speedMode.getValue() ? 3000 : 5000)) {
                     this.mc.player.closeHandledScreen();
                     this.stage = AutoShulkerOrder.Stage.SHOP;
                     this.stageStart = now;
                  }
               }
               break;
            case 9:
               Object screenObj2 = this.mc.currentScreen;
               if (screenObj2 instanceof GenericContainerScreen) {
                  GenericContainerScreen screen = (GenericContainerScreen)screenObj2;
                  ScreenHandler handler = screen.getScreenHandler();
                  if (this.shulkerMoveIndex >= 36) {
                     this.mc.player.closeHandledScreen();
                     this.stage = AutoShulkerOrder.Stage.ORDERS_CONFIRM;
                     this.stageStart = now;
                     this.shulkerMoveIndex = 0;
                     return;
                  }

                  long moveDelay = this.speedMode.getValue() ? 10L : 100L;
                  if (now - this.lastShulkerMoveTime >= moveDelay) {
                     int batchSize = this.speedMode.getValue() ? 3 : 1;

                     for(int batch = 0; batch < batchSize && this.shulkerMoveIndex < 36; ++batch) {
                        ItemStack stack = this.mc.player.getInventory().getStack(this.shulkerMoveIndex);
                        if (this.isShulkerBox(stack)) {
                           int playerSlotId = -1;

                           for(Slot slot : handler.slots) {
                              if (slot.inventory == this.mc.player.getInventory() && slot.getIndex() == this.shulkerMoveIndex) {
                                 playerSlotId = slot.id;
                                 break;
                              }
                           }

                           if (playerSlotId != -1) {
                              this.mc.interactionManager.clickSlot(handler.syncId, playerSlotId, 0, net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, this.mc.player);
                           }
                        }

                        ++this.shulkerMoveIndex;
                     }

                     this.lastShulkerMoveTime = now;
                  }
               }
               break;
            case 10:
               if (this.mc.currentScreen == null) {
                  ++this.exitCount;
                  if (this.exitCount < 2) {
                     this.mc.player.closeHandledScreen();
                     this.stageStart = now;
                  } else {
                     this.exitCount = 0;
                     this.stage = AutoShulkerOrder.Stage.ORDERS_CONFIRM;
                     this.stageStart = now;
                  }
               }
               break;
            case 11:
               Object screenObj3 = this.mc.currentScreen;
               if (screenObj3 instanceof GenericContainerScreen) {
                  GenericContainerScreen screen = (GenericContainerScreen)screenObj3;
                  ScreenHandler handler = screen.getScreenHandler();

                  for(Slot slot : handler.slots) {
                     ItemStack stack = slot.getStack();
                     if (!stack.isEmpty() && this.isGreenGlass(stack)) {
                        for(int i = 0; i < (this.speedMode.getValue() ? 15 : 5); ++i) {
                           this.mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, this.mc.player);
                        }

                        this.stage = AutoShulkerOrder.Stage.ORDERS_FINAL_EXIT;
                        this.stageStart = now;
                        this.finalExitCount = 0;
                        this.finalExitStart = now;
                        if (this.notifications.getValue()) {
                           this.info("✅ Order completed! Going back to shop to buy more shulkers...");
                        }

                        return;
                     }
                  }

                  if (now - this.stageStart > (long)(this.speedMode.getValue() ? 2000 : 5000)) {
                     this.mc.player.closeHandledScreen();
                     this.stage = AutoShulkerOrder.Stage.SHOP;
                     this.stageStart = now;
                  }
               }
               break;
            case 12:
               long exitDelay = this.speedMode.getValue() ? 50L : 200L;
               if (this.finalExitCount == 0) {
                  if (System.currentTimeMillis() - this.finalExitStart >= exitDelay) {
                     this.mc.player.closeHandledScreen();
                     ++this.finalExitCount;
                     this.finalExitStart = System.currentTimeMillis();
                  }
               } else if (this.finalExitCount == 1) {
                  if (System.currentTimeMillis() - this.finalExitStart >= exitDelay) {
                     this.mc.player.closeHandledScreen();
                     ++this.finalExitCount;
                     this.finalExitStart = System.currentTimeMillis();
                  }
               } else {
                  this.finalExitCount = 0;
                  this.stage = AutoShulkerOrder.Stage.CYCLE_PAUSE;
                  this.stageStart = System.currentTimeMillis();
               }
               break;
            case 13:
               long cycleWait = this.speedMode.getValue() ? 10L : 25L;
               if (now - this.stageStart >= cycleWait) {
                  this.updateTargetPlayer();
                  this.stage = AutoShulkerOrder.Stage.SHOP;
                  this.stageStart = now;
               }
               break;
            case 14:
               this.mc.player.sendMessage(Text.of("/orders " + this.targetPlayer));
               this.stage = AutoShulkerOrder.Stage.ORDERS;
               this.stageStart = now;
               if (this.notifications.getValue()) {
                  this.info("\ud83d\udd0d Checking orders for: %s", this.targetPlayer);
               }
         }

      }
   }

   private String getOrderPlayerName(ItemStack stack) {
      if (stack.isEmpty()) {
         return null;
      } else {
         Item.TooltipContext tooltipContext = Item.TooltipContext.create(this.mc.world);
         List<Text> tooltip = stack.getTooltip(tooltipContext, this.mc.player, net.minecraft.item.tooltip.TooltipType.BASIC);
         Pattern[] namePatterns = new Pattern[]{Pattern.compile("(?i)player\\s*:\\s*([a-zA-Z0-9_]+)"), Pattern.compile("(?i)from\\s*:\\s*([a-zA-Z0-9_]+)"), Pattern.compile("(?i)by\\s*:\\s*([a-zA-Z0-9_]+)"), Pattern.compile("(?i)seller\\s*:\\s*([a-zA-Z0-9_]+)"), Pattern.compile("(?i)owner\\s*:\\s*([a-zA-Z0-9_]+)"), Pattern.compile("\\b([a-zA-Z0-9_]{3,16})\\b")};

         for(Text line : tooltip) {
            String text = line.getString();

            for(Pattern pattern : namePatterns) {
               Matcher matcher = pattern.matcher(text);
               if (matcher.find()) {
                  String playerName = matcher.group(1);
                  if (playerName.length() >= 3 && playerName.length() <= 16 && playerName.matches("[a-zA-Z0-9_]+")) {
                     return playerName;
                  }
               }
            }
         }

         return null;
      }
   }

   private double getOrderPrice(ItemStack stack) {
      if (stack.isEmpty()) {
         return (double)-1.0F;
      } else {
         Item.TooltipContext tooltipContext = Item.TooltipContext.create(this.mc.world);
         List<Text> tooltip = stack.getTooltip(tooltipContext, this.mc.player, net.minecraft.item.tooltip.TooltipType.BASIC);
         return this.parseTooltipPrice(tooltip);
      }
   }

   private double parseTooltipPrice(List tooltip) {
      if (tooltip != null && !tooltip.isEmpty()) {
         Pattern[] pricePatterns = new Pattern[]{Pattern.compile("\\$([\\d,]+(?:\\.[\\d]+)?)([kmb])?", 2), Pattern.compile("(?i)price\\s*:\\s*([\\d,]+(?:\\.[\\d]+)?)([kmb])?", 2), Pattern.compile("(?i)pay\\s*:\\s*([\\d,]+(?:\\.[\\d]+)?)([kmb])?", 2), Pattern.compile("(?i)reward\\s*:\\s*([\\d,]+(?:\\.[\\d]+)?)([kmb])?", 2), Pattern.compile("([\\d,]+(?:\\.[\\d]+)?)([kmb])?\\s*coins?", 2), Pattern.compile("\\b([\\d,]+(?:\\.[\\d]+)?)([kmb])\\b", 2)};

         for(Object lineObj : tooltip) {
            Text line = (Text)lineObj;
            String text = line.getString();

            for(Pattern pattern : pricePatterns) {
               Matcher matcher = pattern.matcher(text);
               if (matcher.find()) {
                  String numberStr = matcher.group(1).replace(",", "");
                  String suffix = "";
                  if (matcher.groupCount() >= 2 && matcher.group(2) != null) {
                     suffix = matcher.group(2).toLowerCase();
                  }

                  try {
                     double basePrice = Double.parseDouble(numberStr);
                     double multiplier = (double)1.0F;
                     switch (suffix) {
                        case "k" -> multiplier = (double)1000.0F;
                        case "m" -> multiplier = (double)1000000.0F;
                        case "b" -> multiplier = (double)1.0E9F;
                     }

                     return basePrice * multiplier;
                  } catch (NumberFormatException var19) {
                  }
               }
            }
         }

         return (double)-1.0F;
      } else {
         return (double)-1.0F;
      }
   }

   private double parsePrice(String priceStr) {
      if (priceStr != null && !priceStr.isEmpty()) {
         String cleaned = priceStr.trim().toLowerCase().replace(",", "");
         double multiplier = (double)1.0F;
         if (cleaned.endsWith("b")) {
            multiplier = (double)1.0E9F;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
         } else if (cleaned.endsWith("m")) {
            multiplier = (double)1000000.0F;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
         } else if (cleaned.endsWith("k")) {
            multiplier = (double)1000.0F;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
         }

         try {
            return Double.parseDouble(cleaned) * multiplier;
         } catch (NumberFormatException var6) {
            return (double)-1.0F;
         }
      } else {
         return (double)-1.0F;
      }
   }

   private String formatPrice(double price) {
      if (price >= (double)1.0E9F) {
         return String.format("$%.1fB", price / (double)1.0E9F);
      } else if (price >= (double)1000000.0F) {
         return String.format("$%.1fM", price / (double)1000000.0F);
      } else {
         return price >= (double)1000.0F ? String.format("$%.1fK", price / (double)1000.0F) : String.format("$%.0f", price);
      }
   }

   private boolean isEndStone(ItemStack stack) {
      return stack.getItem() == Items.END_STONE || stack.getName().getString().toLowerCase(Locale.ROOT).contains("end");
   }

   private boolean isShulkerBox(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem().getName().getString().toLowerCase(Locale.ROOT).contains("shulker box");
   }

   private boolean isPurple(ItemStack stack) {
      return stack.getItem() == Items.SHULKER_BOX;
   }

   private boolean isGreenGlass(ItemStack stack) {
      return stack.getItem() == Items.LIME_STAINED_GLASS_PANE || stack.getItem() == Items.GREEN_STAINED_GLASS_PANE;
   }

   private boolean isInventoryFull() {
      for(int i = 9; i <= 35; ++i) {
         ItemStack stack = this.mc.player.getInventory().getStack(i);
         if (stack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public void info(String message, Object... args) {
      if (this.notifications.getValue()) {
         this.mc.player.sendMessage(Text.literal(String.format(message, args)), false);
      }

   }

   public void error(String message, Object... args) {
      if (this.notifications.getValue()) {
         ClientPlayerEntity var10000 = this.mc.player;
         String var10001 = String.format(message, args);
         var10000.sendMessage(Text.literal("§c" + var10001), false);
      }

   }

   private static enum Stage {
      NONE,
      SHOP,
      SHOP_END,
      SHOP_SHULKER,
      SHOP_CONFIRM,
      SHOP_CHECK_FULL,
      SHOP_EXIT,
      WAIT,
      ORDERS,
      ORDERS_SELECT,
      ORDERS_EXIT,
      ORDERS_CONFIRM,
      ORDERS_FINAL_EXIT,
      CYCLE_PAUSE,
      TARGET_ORDERS;

      // $FF: synthetic method
      private static Stage[] $values() {
         return new Stage[]{NONE, SHOP, SHOP_END, SHOP_SHULKER, SHOP_CONFIRM, SHOP_CHECK_FULL, SHOP_EXIT, WAIT, ORDERS, ORDERS_SELECT, ORDERS_EXIT, ORDERS_CONFIRM, ORDERS_FINAL_EXIT, CYCLE_PAUSE, TARGET_ORDERS};
      }
   }
}
