package dev.gambleclient.module.modules.render;

import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;

public final class SwingSpeed extends Module {
   private final NumberSetting swingSpeed = (NumberSetting)(new NumberSetting(EncryptedString.of("Swing Speed"), (double)1.0F, (double)20.0F, (double)6.0F, (double)1.0F)).setDescription(EncryptedString.of("Speed of hand swinging animation"));

   public SwingSpeed() {
      super(EncryptedString.of("Swing Speed"), EncryptedString.of("Modifies the speed of hand swinging animation"), -1, Category.RENDER);
      this.addSettings(new Setting[]{this.swingSpeed});
   }

   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   public NumberSetting getSwingSpeed() {
      return this.swingSpeed;
   }
}
