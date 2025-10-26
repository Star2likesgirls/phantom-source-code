package dev.gambleclient.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.gambleclient.Gamble;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BindSetting;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.ItemSetting;
import dev.gambleclient.module.setting.MacroSetting;
import dev.gambleclient.module.setting.MinMaxSetting;
import dev.gambleclient.module.setting.ModeSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.module.setting.StringSetting;
import dev.gambleclient.utils.EncryptedString;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

public final class ConfigManager {
   private JsonObject jsonObject;
   private final File configFile;
   private final Gson gson;

   public ConfigManager() {
      String userHome = System.getProperty("user.home");
      String configDir = userHome + File.separator + ".minecraft" + File.separator + "config";
      File configFolder = new File(configDir);
      if (!configFolder.exists()) {
         configFolder.mkdirs();
      }

      this.configFile = new File(configFolder, "krypton_config.json");
      this.gson = (new GsonBuilder()).setPrettyPrinting().create();
      this.jsonObject = new JsonObject();
      this.loadConfigFromFile();
   }

   private void loadConfigFromFile() {
      try {
         if (this.configFile.exists()) {
            FileReader reader = new FileReader(this.configFile);

            try {
               this.jsonObject = (JsonObject)this.gson.fromJson(reader, JsonObject.class);
               if (this.jsonObject == null) {
                  this.jsonObject = new JsonObject();
               }
            } catch (Throwable var5) {
               try {
                  reader.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }

               throw var5;
            }

            reader.close();
         } else {
            this.jsonObject = new JsonObject();
         }
      } catch (Exception e) {
         System.err.println("[ConfigManager] Error loading config file: " + e.getMessage());
         e.printStackTrace();
         this.jsonObject = new JsonObject();
      }

   }

   public void loadProfile() {
      try {
         if (this.jsonObject == null) {
            this.jsonObject = new JsonObject();
            return;
         }

         int modulesLoaded = 0;

         for(Object nextObj : Gamble.INSTANCE.getModuleManager().c()) {
            Module next = (Module)nextObj;
            try {
               String moduleName = this.getModuleName(next);
               JsonElement value = this.jsonObject.get(moduleName);
               if (value != null && value.isJsonObject()) {
                  JsonObject asJsonObject = value.getAsJsonObject();
                  JsonElement value2 = asJsonObject.get("enabled");
                  if (value2 != null && value2.isJsonPrimitive() && value2.getAsBoolean()) {
                     next.toggle(true);
                  }

                  for(Object next2 : next.getSettings()) {
                     try {
                        String settingName = this.getSettingName((Setting)next2);
                        JsonElement value3 = asJsonObject.get(settingName);
                        if (value3 != null) {
                           this.setValueFromJson((Setting)next2, value3, next);
                        }
                     } catch (Exception e) {
                        System.err.println("[ConfigManager] Error loading setting for module " + moduleName + ": " + e.getMessage());
                     }
                  }

                  ++modulesLoaded;
               }
            } catch (Exception e) {
               System.err.println("[ConfigManager] Error loading module: " + e.getMessage());
            }
         }
      } catch (Exception ex) {
         System.err.println("[ConfigManager] Error loading profile: " + ex.getMessage());
         ex.printStackTrace();
      }

   }

   private String getModuleName(Module module) {
      try {
         CharSequence name = module.getName();
         return name instanceof EncryptedString ? ((EncryptedString)name).toString() : name.toString();
      } catch (Exception var3) {
         return "Module_" + module.hashCode();
      }
   }

   private String getSettingName(Setting setting) {
      try {
         CharSequence name = setting.getName();
         return name instanceof EncryptedString ? ((EncryptedString)name).toString() : name.toString();
      } catch (Exception var3) {
         return "Setting_" + setting.hashCode();
      }
   }

   private void setValueFromJson(Setting setting, JsonElement jsonElement, Module module) {
      try {
         if (setting instanceof BooleanSetting booleanSetting) {
            if (jsonElement.isJsonPrimitive()) {
               booleanSetting.setValue(jsonElement.getAsBoolean());
            }
         } else if (setting instanceof ModeSetting enumSetting) {
            if (jsonElement.isJsonPrimitive()) {
               int asInt = jsonElement.getAsInt();
               if (asInt != -1) {
                  enumSetting.setModeIndex(asInt);
               } else {
                  enumSetting.setModeIndex(enumSetting.getOriginalValue());
               }
            }
         } else if (setting instanceof NumberSetting numberSetting) {
            if (jsonElement.isJsonPrimitive()) {
               numberSetting.getValue(jsonElement.getAsDouble());
            }
         } else if (setting instanceof BindSetting bindSetting) {
            if (jsonElement.isJsonPrimitive()) {
               int asInt2 = jsonElement.getAsInt();
               bindSetting.setValue(asInt2);
               if (bindSetting.isModuleKey()) {
                  module.setKeybind(asInt2);
               }
            }
         } else if (setting instanceof StringSetting stringSetting) {
            if (jsonElement.isJsonPrimitive()) {
               stringSetting.setValue(jsonElement.getAsString());
            }
         } else if (setting instanceof MinMaxSetting minMaxSetting) {
            if (jsonElement.isJsonObject()) {
               JsonObject asJsonObject = jsonElement.getAsJsonObject();
               if (asJsonObject.has("min") && asJsonObject.has("max")) {
                  double asDouble = asJsonObject.get("min").getAsDouble();
                  double asDouble2 = asJsonObject.get("max").getAsDouble();
                  minMaxSetting.setCurrentMin(asDouble);
                  minMaxSetting.setCurrentMax(asDouble2);
               }
            }
         } else if (setting instanceof ItemSetting && jsonElement.isJsonPrimitive()) {
            ((ItemSetting)setting).setItem((Item)Registries.ITEM.get(Identifier.of(jsonElement.getAsString())));
         } else if (setting instanceof MacroSetting && jsonElement.isJsonArray()) {
            MacroSetting macroSetting = (MacroSetting)setting;
            macroSetting.clearCommands();

            for(JsonElement element : jsonElement.getAsJsonArray()) {
               if (element.isJsonPrimitive()) {
                  macroSetting.addCommand(element.getAsString());
               }
            }
         }
      } catch (Exception ex) {
         System.err.println("[ConfigManager] Error setting value from JSON: " + ex.getMessage());
      }

   }

   private void saveConfigToFile() {
      try {
         File parentDir = this.configFile.getParentFile();
         if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
         }

         FileWriter writer = new FileWriter(this.configFile);

         try {
            this.gson.toJson(this.jsonObject, writer);
         } catch (Throwable var6) {
            try {
               writer.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         writer.close();
      } catch (IOException e) {
         System.err.println("[ConfigManager] Error saving config file: " + e.getMessage());
         e.printStackTrace();
      }

   }

   public void saveConfig() {
      this.shutdown();
   }

   public void manualSave() {
      this.shutdown();
   }

   public void reloadConfig() {
      this.loadConfigFromFile();
      this.loadProfile();
   }

   public File getConfigFile() {
      return this.configFile;
   }

   public void shutdown() {
      try {
         this.jsonObject = new JsonObject();
         int modulesSaved = 0;

         for(Object moduleObj : Gamble.INSTANCE.getModuleManager().c()) {
            Module module = (Module)moduleObj;
            try {
               String moduleName = this.getModuleName(module);
               JsonObject jsonObject = new JsonObject();
               jsonObject.addProperty("enabled", module.isEnabled());

               for(Object settingObj : module.getSettings()) {
                  Setting setting = (Setting)settingObj;
                  try {
                     this.save(setting, jsonObject, module);
                  } catch (Exception e) {
                     System.err.println("[ConfigManager] Error saving setting for module " + moduleName + ": " + e.getMessage());
                  }
               }

               this.jsonObject.add(moduleName, jsonObject);
               ++modulesSaved;
            } catch (Exception e) {
               System.err.println("[ConfigManager] Error saving module: " + e.getMessage());
            }
         }

         this.saveConfigToFile();
      } catch (Exception _t) {
         System.err.println("[ConfigManager] Error during shutdown: " + _t.getMessage());
         _t.printStackTrace(System.err);
      }

   }

   private void save(Setting setting, JsonObject jsonObject, Module module) {
      try {
         String settingName = this.getSettingName(setting);
         if (setting instanceof BooleanSetting booleanSetting) {
            jsonObject.addProperty(settingName, booleanSetting.getValue());
         } else if (setting instanceof ModeSetting enumSetting) {
            jsonObject.addProperty(settingName, enumSetting.getModeIndex());
         } else if (setting instanceof NumberSetting numberSetting) {
            jsonObject.addProperty(settingName, numberSetting.getValue());
         } else if (setting instanceof BindSetting bindSetting) {
            jsonObject.addProperty(settingName, bindSetting.getValue());
         } else if (setting instanceof StringSetting stringSetting) {
            jsonObject.addProperty(settingName, stringSetting.getValue());
         } else if (setting instanceof MinMaxSetting) {
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("min", ((MinMaxSetting)setting).getCurrentMin());
            jsonObject2.addProperty("max", ((MinMaxSetting)setting).getCurrentMax());
            jsonObject.add(settingName, jsonObject2);
         } else if (setting instanceof ItemSetting) {
            ItemSetting itemSetting = (ItemSetting)setting;
            jsonObject.addProperty(settingName, Registries.ITEM.getId(itemSetting.getItem()).toString());
         } else if (setting instanceof MacroSetting) {
            MacroSetting macroSetting = (MacroSetting)setting;
            JsonArray commandsArray = new JsonArray();

            for(Object commandObj : macroSetting.getCommands()) {
               String command = (String)commandObj;
               commandsArray.add(command);
            }

            jsonObject.add(settingName, commandsArray);
         }
      } catch (Exception ex) {
         String settingName = "Unknown";

         try {
            settingName = this.getSettingName(setting);
         } catch (Exception var15) {
            settingName = "Setting_" + setting.hashCode();
         }

         System.err.println("[ConfigManager] Error saving setting " + settingName + ": " + ex.getMessage());
      }

   }
}
