package dev.gambleclient.module.modules.client;

import com.sun.jna.Memory;
import dev.gambleclient.Gamble;
import dev.gambleclient.gui.ClickGUI;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.module.setting.StringSetting;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.Utils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import net.minecraft.text.Text;

public final class SelfDestruct extends Module {
   public static boolean isActive = false;
   public static boolean hasSelfDestructed = false;
   private final BooleanSetting replaceMod = (new BooleanSetting(EncryptedString.of("Replace Mod"), true)).setDescription(EncryptedString.of("Replaces the mod with the specified JAR file"));
   private final BooleanSetting saveLastModified = (new BooleanSetting(EncryptedString.of("Save Last Modified"), true)).setDescription(EncryptedString.of("Saves the last modified date after self destruct"));
   private final StringSetting replaceUrl = new StringSetting(EncryptedString.of("Replace URL"), "https://cdn.modrinth.com/data/8shC1gFX/versions/sXO3idkS/BetterF3-11.0.1-Fabric-1.21.jar");

   public SelfDestruct() {
      super(EncryptedString.of("Self Destruct"), EncryptedString.of("Removes the client from your game |Credits to Argon for deletion|"), -1, Category.CLIENT);
      this.addSettings(new Setting[]{this.replaceMod, this.saveLastModified, this.replaceUrl});
   }

   public void onEnable() {
      isActive = true;
      hasSelfDestructed = true;

      try {
         Thread.sleep(100L);
      } catch (InterruptedException var7) {
      }

      Gamble.INSTANCE.getModuleManager().getModuleByClass(Phantom.class).toggle(false);
      this.toggle(false);
      Gamble.INSTANCE.getConfigManager().shutdown();
      if (this.mc.currentScreen instanceof ClickGUI) {
         Gamble.INSTANCE.shouldPreventClose = false;
         this.mc.currentScreen.close();
      }

      if (this.replaceMod.getValue()) {
         try {
            String downloadUrl = this.replaceUrl.getValue();
            File currentJar = Utils.getCurrentJarPath();
            if (currentJar.exists() && currentJar.isFile()) {
               this.replaceModFile(currentJar, downloadUrl);
            }
         } catch (Exception var6) {
         }
      }

      for(Object moduleObj : Gamble.INSTANCE.getModuleManager().c()) {
         Module module = (Module)moduleObj;
         module.toggle(false);
         module.setName((CharSequence)null);
         module.setDescription((CharSequence)null);

         for(Object settingObj : module.getSettings()) {
            Setting setting = (Setting)settingObj;
            setting.getDescription((CharSequence)null);
            setting.setDescription((CharSequence)null);
            if (setting instanceof StringSetting) {
               ((StringSetting)setting).setValue((String)null);
            }
         }

         module.getSettings().clear();
      }

      Runtime runtime = Runtime.getRuntime();
      if (this.saveLastModified.getValue()) {
         Gamble.INSTANCE.resetModifiedDate();
      }

      for(int i = 0; i <= 10; ++i) {
         runtime.gc();

         try {
            Thread.sleep((long)(100 * i));
            Memory.purge();
            Memory.disposeAll();
         } catch (InterruptedException var5) {
         }
      }

      if (this.mc.player != null) {
         this.mc.player.sendMessage(Text.literal("§c§l[SelfDestruct] §rClient has been cleared. Game will continue running."));
      }

   }

   private void replaceModFile(File targetFile, String downloadUrl) {
      try {
         URL url = new URL(downloadUrl);
         HttpURLConnection connection = (HttpURLConnection)url.openConnection();
         connection.setRequestMethod("GET");
         connection.setConnectTimeout(10000);
         connection.setReadTimeout(30000);
         InputStream inputStream = connection.getInputStream();

         try {
            FileOutputStream outputStream = new FileOutputStream(targetFile);

            try {
               byte[] buffer = new byte[8192];

               int bytesRead;
               while((bytesRead = inputStream.read(buffer)) != -1) {
                  outputStream.write(buffer, 0, bytesRead);
               }

               outputStream.flush();
            } catch (Throwable var11) {
               try {
                  outputStream.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }

               throw var11;
            }

            outputStream.close();
         } catch (Throwable var12) {
            if (inputStream != null) {
               try {
                  inputStream.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (inputStream != null) {
            inputStream.close();
         }

         connection.disconnect();
      } catch (Exception var13) {
      }

   }
}
