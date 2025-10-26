package dev.gambleclient.module.modules.donut;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.ItemSetting;
import dev.gambleclient.module.setting.ModeSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.module.setting.StringSetting;
import dev.gambleclient.utils.EncryptedString;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;

public final class AuctionSniper extends Module {
   private final ItemSetting snipingItem;
   private final StringSetting price;
   private final ModeSetting mode;
   private final StringSetting apiKey;
   private final NumberSetting refreshDelay;
   private final NumberSetting buyDelay;
   private final NumberSetting apiRefreshRate;
   private final BooleanSetting showApiNotifications;
   private int delayCounter;
   private boolean isProcessing;
   private final HttpClient httpClient;
   private final Gson gson;
   private long lastApiCallTimestamp;
   private final Map snipingItems;
   private boolean isApiQueryInProgress;
   private boolean isAuctionSniping;
   private int auctionPageCounter;
   private String currentSellerName;

   public AuctionSniper() {
      super(EncryptedString.of("Auction Sniper"), EncryptedString.of("Snipes items on auction house for cheap"), -1, Category.DONUT);
      this.snipingItem = new ItemSetting(EncryptedString.of("Sniping Item"), Items.AIR);
      this.price = new StringSetting(EncryptedString.of("Price"), "1k");
      this.mode = (new ModeSetting(EncryptedString.of("Mode"), AuctionSniper.Mode.MANUAL, Mode.class)).setDescription(EncryptedString.of("Manual is faster but api doesnt require auction gui opened all the time"));
      this.apiKey = (new StringSetting(EncryptedString.of("Api Key"), "")).setDescription(EncryptedString.of("You can get it by typing /api in chat"));
      this.refreshDelay = new NumberSetting(EncryptedString.of("Refresh Delay"), (double)0.0F, (double)100.0F, (double)2.0F, (double)1.0F);
      this.buyDelay = new NumberSetting(EncryptedString.of("Buy Delay"), (double)0.0F, (double)100.0F, (double)2.0F, (double)1.0F);
      this.apiRefreshRate = (new NumberSetting(EncryptedString.of("API Refresh Rate"), (double)10.0F, (double)5000.0F, (double)250.0F, (double)10.0F)).getValue(EncryptedString.of("How often to query the API (in milliseconds)"));
      this.showApiNotifications = (new BooleanSetting(EncryptedString.of("Show API Notifications"), true)).setDescription(EncryptedString.of("Show chat notifications for API actions"));
      this.lastApiCallTimestamp = 0L;
      this.snipingItems = new HashMap();
      this.isApiQueryInProgress = false;
      this.isAuctionSniping = false;
      this.auctionPageCounter = -1;
      this.currentSellerName = "";
      this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
      this.gson = new Gson();
      Setting[] settingArray = new Setting[]{this.snipingItem, this.price, this.mode, this.apiKey, this.refreshDelay, this.buyDelay, this.apiRefreshRate, this.showApiNotifications};
      this.addSettings(settingArray);
   }

   public void onEnable() {
      super.onEnable();
      double d = this.parsePrice(this.price.getValue());
      if (d == (double)-1.0F) {
         if (this.mc.player != null) {
            ClientPlayerEntity clientPlayerEntity = this.mc.player;
            clientPlayerEntity.sendMessage(Text.of("Invalid Price"), true);
         }

         this.toggle();
      } else {
         if (this.snipingItem.getItem() != Items.AIR) {
            Map<String, Double> map = this.snipingItems;
            map.put(this.snipingItem.getItem().toString(), d);
         }

         this.lastApiCallTimestamp = 0L;
         this.isApiQueryInProgress = false;
         this.isAuctionSniping = false;
         this.currentSellerName = "";
      }
   }

   public void onDisable() {
      super.onDisable();
      this.isAuctionSniping = false;
   }

   @EventListener
   public void onTick(TickEvent tickEvent) {
      if (this.mc.player != null) {
         if (this.delayCounter > 0) {
            --this.delayCounter;
         } else if (this.mode.isMode(AuctionSniper.Mode.API)) {
            this.handleApiMode();
         } else {
            if (this.mode.isMode(AuctionSniper.Mode.MANUAL)) {
               ScreenHandler screenHandler = this.mc.player.currentScreenHandler;
               if (!(this.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
                  String[] stringArray = this.snipingItem.getItem().getTranslationKey().split("\\.");
                  String string2 = stringArray[stringArray.length - 1];
                  String string3 = (String)Arrays.stream(string2.replace("_", " ").split(" ")).map((string) -> {
                     String var10000 = string.substring(0, 1).toUpperCase();
                     return var10000 + string.substring(1);
                  }).collect(Collectors.joining(" "));
                  this.mc.getNetworkHandler().sendChatCommand("ah " + string3);
                  this.delayCounter = 20;
                  return;
               }

               if (((GenericContainerScreenHandler)screenHandler).getRows() == 6) {
                  this.processSixRowAuction((GenericContainerScreenHandler)screenHandler);
               } else if (((GenericContainerScreenHandler)screenHandler).getRows() == 3) {
                  this.processThreeRowAuction((GenericContainerScreenHandler)screenHandler);
               }
            }

         }
      }
   }

   private void handleApiMode() {
      if (!this.isAuctionSniping) {
         if (this.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler && this.mc.currentScreen.getTitle().getString().contains("Page")) {
            this.mc.player.closeHandledScreen();
            this.delayCounter = 20;
         } else if (!this.isApiQueryInProgress) {
            long l = System.currentTimeMillis();
            long l2 = l - this.lastApiCallTimestamp;
            if (l2 > (long)this.apiRefreshRate.getIntValue()) {
               this.lastApiCallTimestamp = l;
               if (this.apiKey.getValue().isEmpty()) {
                  if (this.showApiNotifications.getValue()) {
                     ClientPlayerEntity clientPlayerEntity = this.mc.player;
                     clientPlayerEntity.sendMessage(Text.of("§cAPI key is not set. Set it using /api in-game."), false);
                  }

                  return;
               }

               this.isApiQueryInProgress = true;
               this.queryApi().thenAccept((list) -> this.processApiResponse((List)list));
            }

         }
      } else {
         ScreenHandler screenHandler = this.mc.player.currentScreenHandler;
         if (!(this.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
            if (this.auctionPageCounter != -1) {
               if (this.auctionPageCounter <= 40) {
                  ++this.auctionPageCounter;
               } else {
                  this.isAuctionSniping = false;
                  this.currentSellerName = "";
               }
            } else {
               this.mc.getNetworkHandler().sendChatCommand("ah " + this.currentSellerName);
               this.auctionPageCounter = 0;
            }
         } else {
            this.auctionPageCounter = -1;
            if (((GenericContainerScreenHandler)screenHandler).getRows() == 6) {
               this.processSixRowAuction((GenericContainerScreenHandler)screenHandler);
            } else if (((GenericContainerScreenHandler)screenHandler).getRows() == 3) {
               this.processThreeRowAuction((GenericContainerScreenHandler)screenHandler);
            }
         }

      }
   }

   private CompletableFuture queryApi() {
      return CompletableFuture.supplyAsync(() -> {
         try {
            String string = "https://api.donutsmp.net/v1/auction/list/1";
            HttpResponse<String> httpResponse = this.httpClient.send(HttpRequest.newBuilder().uri(URI.create(string)).header("Authorization", "Bearer " + this.apiKey.getValue()).header("Content-Type", "application/json").POST(BodyPublishers.ofString("{\"sort\": \"recently_listed\"}")).build(), BodyHandlers.ofString());
            if (httpResponse.statusCode() != 200) {
               if (this.showApiNotifications.getValue() && this.mc.player != null) {
                  ClientPlayerEntity clientPlayerEntity = this.mc.player;
                  clientPlayerEntity.sendMessage(Text.of("§cAPI Error: " + httpResponse.statusCode()), false);
               }

               ArrayList<?> arrayList = new ArrayList();
               this.isApiQueryInProgress = false;
               return arrayList;
            } else {
               Gson gson = this.gson;
               JsonArray jsonArray = ((JsonObject)gson.fromJson((String)httpResponse.body(), JsonObject.class)).getAsJsonArray("result");
               ArrayList<JsonObject> arrayList = new ArrayList();

               for(JsonElement jsonElement : jsonArray) {
                  arrayList.add(jsonElement.getAsJsonObject());
               }

               this.isApiQueryInProgress = false;
               return arrayList;
            }
         } catch (Throwable _t) {
            _t.printStackTrace(System.err);
            return List.of();
         }
      });
   }

   private void processApiResponse(List list) {
      for(Object e : list) {
         try {
            String string2 = ((JsonObject)e).getAsJsonObject("item").get("id").getAsString();
            long l = ((JsonObject)e).get("price").getAsLong();
            String string3 = ((JsonObject)e).getAsJsonObject("seller").get("name").getAsString();

            for(Object entryObj : this.snipingItems.entrySet()) {
               Map.Entry entry = (Map.Entry)entryObj;
               String string = (String)entry.getKey();
               double d = (Double)entry.getValue();
               if (string2.contains(string) && (double)l <= d) {
                  if (this.showApiNotifications.getValue() && this.mc.player != null) {
                     ClientPlayerEntity clientPlayerEntity = this.mc.player;
                     clientPlayerEntity.sendMessage(Text.of("§aFound " + string2 + " for " + this.formatPrice((double)l) + " §r(threshold: " + this.formatPrice(d) + ") §afrom seller: " + string3), false);
                  }

                  this.isAuctionSniping = true;
                  this.currentSellerName = string3;
                  return;
               }
            }
         } catch (Exception exception) {
            if (this.showApiNotifications.getValue() && this.mc.player != null) {
               ClientPlayerEntity clientPlayerEntity = this.mc.player;
               clientPlayerEntity.sendMessage(Text.of("§cError processing auction: " + exception.getMessage()), false);
            }
         }
      }

   }

   private void processSixRowAuction(GenericContainerScreenHandler genericContainerScreenHandler) {
      ItemStack itemStack = genericContainerScreenHandler.getSlot(47).getStack();
      if (itemStack.isOf(Items.AIR)) {
         this.delayCounter = 2;
      } else {
         for(Object e : itemStack.getTooltip(Item.TooltipContext.DEFAULT, this.mc.player, net.minecraft.item.tooltip.TooltipType.BASIC)) {
            String string = e.toString();
            if (string.contains("Recently Listed") && (((Text)e).getStyle().toString().contains("white") || string.contains("white"))) {
               this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 47, 1, net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, this.mc.player);
               this.delayCounter = 5;
               return;
            }
         }

         for(int i = 0; i < 44; ++i) {
            ItemStack itemStack2 = genericContainerScreenHandler.getSlot(i).getStack();
            if (itemStack2.isOf(this.snipingItem.getItem()) && this.isValidAuctionItem(itemStack2)) {
               if (this.isProcessing) {
                  this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, i, 1, net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, this.mc.player);
                  this.isProcessing = false;
                  return;
               }

               this.isProcessing = true;
               this.delayCounter = this.buyDelay.getIntValue();
               return;
            }
         }

         if (this.isAuctionSniping) {
            this.isAuctionSniping = false;
            this.currentSellerName = "";
            this.mc.player.closeHandledScreen();
         } else {
            this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 49, 1, net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, this.mc.player);
            this.delayCounter = this.refreshDelay.getIntValue();
         }

      }
   }

   private void processThreeRowAuction(GenericContainerScreenHandler genericContainerScreenHandler) {
      if (this.isValidAuctionItem(genericContainerScreenHandler.getSlot(13).getStack())) {
         this.mc.interactionManager.clickSlot(this.mc.player.currentScreenHandler.syncId, 15, 1, net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, this.mc.player);
         this.delayCounter = 20;
      }

      if (this.isAuctionSniping) {
         this.isAuctionSniping = false;
         this.currentSellerName = "";
      }

   }

   private double parseTooltipPrice(List list) {
      if (list != null && !list.isEmpty()) {
         Iterator iterator = list.iterator();

         while(iterator.hasNext()) {
            String string3 = ((Text)iterator.next()).getString();
            if (string3.matches("(?i).*price\\s*:\\s*\\$.*")) {
               String string4 = string3.replaceAll("[,$]", "");
               Matcher matcher = Pattern.compile("([\\d]+(?:\\.[\\d]+)?)\\s*([KMB])?", 2).matcher(string4);
               if (matcher.find()) {
                  String string2 = matcher.group(1);
                  String string = matcher.group(2) != null ? matcher.group(2).toUpperCase() : "";
                  return this.parsePrice(string2 + string);
               }
            }
         }

         return (double)-1.0F;
      } else {
         return (double)-1.0F;
      }
   }

   private boolean isValidAuctionItem(ItemStack itemStack) {
      List list = itemStack.getTooltip(Item.TooltipContext.DEFAULT, this.mc.player, net.minecraft.item.tooltip.TooltipType.BASIC);
      double d = this.parseTooltipPrice(list) / (double)itemStack.getCount();
      double d2 = this.parsePrice(this.price.getValue());
      if (d2 == (double)-1.0F) {
         if (this.mc.player != null) {
            ClientPlayerEntity clientPlayerEntity = this.mc.player;
            clientPlayerEntity.sendMessage(Text.of("Invalid Price"), true);
         }

         this.toggle();
         return false;
      } else if (d != (double)-1.0F) {
         boolean bl = d <= d2;
         return bl;
      } else {
         if (this.mc.player != null) {
            ClientPlayerEntity clientPlayerEntity = this.mc.player;
            clientPlayerEntity.sendMessage(Text.of("Invalid Auction Item Price"), true);

            for(int i = 0; i < list.size() - 1; ++i) {
               PrintStream printStream = System.out;
               printStream.println(i + ". " + ((Text)list.get(i)).getString());
            }
         }

         this.toggle();
         return false;
      }
   }

   private double parsePrice(String string) {
      if (string == null) {
         return (double)-1.0F;
      } else if (string.isEmpty()) {
         return (double)-1.0F;
      } else {
         String string2 = string.trim().toUpperCase();
         double d = (double)1.0F;
         if (string2.endsWith("B")) {
            d = (double)1.0E9F;
            string2 = string2.substring(0, string2.length() - 1);
         } else if (string2.endsWith("M")) {
            d = (double)1000000.0F;
            string2 = string2.substring(0, string2.length() - 1);
         } else if (string2.endsWith("K")) {
            d = (double)1000.0F;
            string2 = string2.substring(0, string2.length() - 1);
         }

         try {
            return Double.parseDouble(string2) * d;
         } catch (NumberFormatException var6) {
            return (double)-1.0F;
         }
      }
   }

   private String formatPrice(double d) {
      if (d >= (double)1.0E9F) {
         Object[] objectArray = new Object[]{d / (double)1.0E9F};
         return String.format("%.2fB", objectArray);
      } else if (d >= (double)1000000.0F) {
         Object[] objectArray = new Object[]{d / (double)1000000.0F};
         return String.format("%.2fM", objectArray);
      } else if (d >= (double)1000.0F) {
         Object[] objectArray = new Object[]{d / (double)1000.0F};
         return String.format("%.2fK", objectArray);
      } else {
         Object[] objectArray = new Object[]{d};
         return String.format("%.2f", objectArray);
      }
   }

   public static enum Mode {
      API("API", 0),
      MANUAL("MANUAL", 1);

      private Mode(final String name, final int ordinal) {
      }

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{API, MANUAL};
      }
   }
}
