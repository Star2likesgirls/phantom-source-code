package dev.gambleclient.module.modules.misc;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BindSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.module.setting.StringSetting;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.KeyUtils;
import dev.gambleclient.utils.embed.DiscordWebhook;
import java.util.concurrent.CompletableFuture;

public final class CordSnapper extends Module {
   private final BindSetting activateKey = new BindSetting(EncryptedString.of("Activate Key"), -1, false);
   private final StringSetting webhookUrl = new StringSetting(EncryptedString.of("Webhook"), "");
   private int cooldownCounter = 0;

   public CordSnapper() {
      super(EncryptedString.of("Cord Snapper"), EncryptedString.of("Sends base coordinates to discord webhook"), -1, Category.MISC);
      this.addSettings(new Setting[]{this.activateKey, this.webhookUrl});
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
         if (this.cooldownCounter > 0) {
            --this.cooldownCounter;
         } else {
            if (KeyUtils.isKeyPressed(this.activateKey.getValue())) {
               DiscordWebhook embedSender = new DiscordWebhook(this.webhookUrl.value);
               double var10001 = this.mc.player.getX();
               embedSender.setContent("Coordinates: x: " + var10001 + " y: " + this.mc.player.getY() + " z: " + this.mc.player.getZ());
               CompletableFuture.runAsync(() -> {
                  try {
                     embedSender.execute();
                  } catch (Throwable _t) {
                     _t.printStackTrace(System.err);
                  }

               });
               this.cooldownCounter = 40;
            }

         }
      }
   }
}
