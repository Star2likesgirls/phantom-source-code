package dev.gambleclient.module.modules.client;

import dev.gambleclient.Gamble;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.module.setting.StringSetting;
import dev.gambleclient.utils.EncryptedString;

public final class ConfigDebug extends Module {
   private final BooleanSetting testBoolean = (new BooleanSetting(EncryptedString.of("Test Boolean"), false)).setDescription(EncryptedString.of("Test boolean setting"));
   private final NumberSetting testNumber = (NumberSetting)(new NumberSetting(EncryptedString.of("Test Number"), (double)1.0F, (double)100.0F, (double)50.0F, (double)1.0F)).setDescription(EncryptedString.of("Test number setting"));
   private final StringSetting testString = (new StringSetting(EncryptedString.of("Test String"), "Default Value")).setDescription(EncryptedString.of("Test string setting"));

   public ConfigDebug() {
      super(EncryptedString.of("Config Debug"), EncryptedString.of("Debug module for testing config saving"), -1, Category.CLIENT);
      this.addSettings(new Setting[]{this.testBoolean, this.testNumber, this.testString});
   }

   public void onEnable() {
      System.out.println("[ConfigDebug] Module enabled - testing config saving...");
      this.testBoolean.setValue(true);
      this.testNumber.getValue((double)75.0F);
      this.testString.setValue("Test Value Changed");
      Gamble.INSTANCE.getConfigManager().manualSave();
      System.out.println("[ConfigDebug] Config save test completed. Check console for results.");
      this.toggle();
   }

   public void onDisable() {
   }
}
