package dev.gambleclient.module.modules.misc;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.MinMaxSetting;
import dev.gambleclient.module.setting.ModeSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.module.setting.StringSetting;
import dev.gambleclient.utils.EncryptedString;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public final class AutoTPA extends Module {
   private final MinMaxSetting delayRange = new MinMaxSetting(EncryptedString.of("Delay"), (double)1.0F, (double)80.0F, (double)1.0F, (double)10.0F, (double)30.0F);
   private final ModeSetting mode;
   private final StringSetting playerName;
   private int delayCounter;

   public AutoTPA() {
      super(EncryptedString.of("Auto Tpa"), EncryptedString.of("Module that helps you teleport streamers to you"), -1, Category.MISC);
      this.mode = new ModeSetting(EncryptedString.of("Mode"), AutoTPA.Mode.TPAHERE, Mode.class);
      this.playerName = new StringSetting(EncryptedString.of("Player"), "DrDonutt");
      this.addSettings(new Setting[]{this.mode, this.delayRange, this.playerName});
   }

   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.delayCounter > 0) {
         --this.delayCounter;
      } else {
         ClientPlayNetworkHandler networkHandler = this.mc.getNetworkHandler();
         String commandPrefix;
         if (this.mode.getValue().equals(AutoTPA.Mode.TPA)) {
            commandPrefix = "tpa ";
         } else {
            commandPrefix = "tpahere ";
         }

         networkHandler.sendCommand(commandPrefix + this.playerName.getValue());
         this.delayCounter = this.delayRange.getRandomIntInRange();
      }
   }

   static enum Mode {
      TPA("Tpa", 0),
      TPAHERE("Tpahere", 1);

      private Mode(final String name, final int ordinal) {
      }

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{TPA, TPAHERE};
      }
   }
}
