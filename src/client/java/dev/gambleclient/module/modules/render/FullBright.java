package dev.gambleclient.module.modules.render;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.ModeSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public final class FullBright extends Module {
   private final ModeSetting mode;
   private double originalGamma;

   public FullBright() {
      super(EncryptedString.of("FullBright"), EncryptedString.of("Gives you the ability to clearly see even when in the dark"), -1, Category.RENDER);
      this.mode = (new ModeSetting(EncryptedString.of("Mode"), FullBright.FullBrightMode.GAMMA, FullBrightMode.class)).setDescription(EncryptedString.of("The way that will be used to change the game's brightness"));
      this.originalGamma = (double)1.0F;
      this.addSettings(new Setting[]{this.mode});
   }

   public void onEnable() {
      super.onEnable();
      if (this.mc.player != null) {
         if (this.mode.getValue() == FullBright.FullBrightMode.GAMMA) {
            try {
               Field gammaField = this.mc.options.getClass().getDeclaredField("gamma");
               gammaField.setAccessible(true);
               Object gammaOption = gammaField.get(this.mc.options);
               if (gammaOption != null) {
                  Method getValueMethod = gammaOption.getClass().getMethod("getValue");
                  this.originalGamma = (Double)getValueMethod.invoke(gammaOption);
               }
            } catch (Exception var4) {
               this.originalGamma = (double)1.0F;
            }
         }

         if (this.mode.getValue() == FullBright.FullBrightMode.POTION && !this.mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            this.mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, -1));
         }

      }
   }

   public void onDisable() {
      super.onDisable();
      if (this.mc.player != null) {
         if (this.mode.getValue() == FullBright.FullBrightMode.GAMMA) {
            try {
               Field gammaField = this.mc.options.getClass().getDeclaredField("gamma");
               gammaField.setAccessible(true);
               Object gammaOption = gammaField.get(this.mc.options);
               if (gammaOption != null) {
                  Method setValueMethod = gammaOption.getClass().getMethod("setValue", Double.TYPE);
                  setValueMethod.invoke(gammaOption, this.originalGamma);
               }
            } catch (Exception var4) {
            }
         }

         if (this.mode.getValue() == FullBright.FullBrightMode.POTION && this.mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            this.mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
         }

      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.mc.player != null) {
         if (this.mode.getValue() == FullBright.FullBrightMode.POTION) {
            if (!this.mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
               this.mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, -1));
            }

         } else {
            if (this.mode.getValue() == FullBright.FullBrightMode.GAMMA) {
               if (this.mc.options == null) {
                  return;
               }

               try {
                  try {
                     Method setGamma = this.mc.options.getClass().getMethod("setGamma", Double.TYPE);
                     setGamma.invoke(this.mc.options, (double)100.0F);
                     return;
                  } catch (NoSuchMethodException var16) {
                     try {
                        Method getGamma = this.mc.options.getClass().getMethod("getGamma");
                        Object gammaOption = getGamma.invoke(this.mc.options);
                        if (gammaOption != null) {
                           Method setValueMethod = gammaOption.getClass().getMethod("setValue", Double.TYPE);
                           setValueMethod.invoke(gammaOption, (double)100.0F);
                           return;
                        }
                     } catch (NoSuchMethodException var12) {
                     }

                     try {
                        Field gammaField = this.mc.options.getClass().getDeclaredField("gamma");
                        gammaField.setAccessible(true);
                        Object gammaOption = gammaField.get(this.mc.options);
                        if (gammaOption != null) {
                           Method setValueMethod = gammaOption.getClass().getMethod("setValue", Double.TYPE);
                           setValueMethod.invoke(gammaOption, (double)100.0F);
                           return;
                        }
                     } catch (NoSuchFieldException var11) {
                     }
                  }
               } catch (Exception var17) {
                  try {
                     Field brightnessField = this.mc.options.getClass().getDeclaredField("brightness");
                     brightnessField.setAccessible(true);
                     Object brightnessOption = brightnessField.get(this.mc.options);
                     if (brightnessOption != null) {
                        Method setValueMethod = brightnessOption.getClass().getMethod("setValue", Double.TYPE);
                        setValueMethod.invoke(brightnessOption, (double)1.0F);
                     }
                  } catch (Exception var15) {
                     try {
                        Field[] fields = this.mc.options.getClass().getDeclaredFields();

                        for(Field field : fields) {
                           if (field.getName().toLowerCase().contains("bright") || field.getName().toLowerCase().contains("gamma") || field.getName().toLowerCase().contains("light")) {
                              field.setAccessible(true);
                              Object option = field.get(this.mc.options);
                              if (option != null) {
                                 try {
                                    Method setValueMethod = option.getClass().getMethod("setValue", Double.TYPE);
                                    setValueMethod.invoke(option, (double)100.0F);
                                    break;
                                 } catch (Exception var13) {
                                 }
                              }
                           }
                        }
                     } catch (Exception var14) {
                        if (!this.mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                           this.mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, -1));
                        }
                     }
                  }
               }
            }

         }
      }
   }

   public static enum FullBrightMode {
      GAMMA,
      POTION;

      // $FF: synthetic method
      private static FullBrightMode[] $values() {
         return new FullBrightMode[]{GAMMA, POTION};
      }
   }
}
