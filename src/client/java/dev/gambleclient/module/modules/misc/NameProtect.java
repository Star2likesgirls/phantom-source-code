package dev.gambleclient.module.modules.misc;

import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.module.setting.StringSetting;
import dev.gambleclient.utils.EncryptedString;

public class NameProtect extends Module {
   private final StringSetting fakeName = new StringSetting("Fake Name", "Player");

   public NameProtect() {
      super(EncryptedString.of("Name Protect"), EncryptedString.of("Replaces your name with given one."), -1, Category.MISC);
      this.addSettings(new Setting[]{this.fakeName});
   }

   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   public String getFakeName() {
      return this.fakeName.getValue();
   }
}
