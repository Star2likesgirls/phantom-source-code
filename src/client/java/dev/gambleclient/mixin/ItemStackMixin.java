package dev.gambleclient.mixin;

import dev.gambleclient.Gamble;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.modules.misc.AutoFirework;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemStack.class})
public class ItemStackMixin {
   @Inject(
      method = {"decrement"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onDecrement(int amount, CallbackInfo ci) {
      if (Gamble.INSTANCE != null && Gamble.INSTANCE.MODULE_MANAGER != null) {
         Module autoFirework = Gamble.INSTANCE.MODULE_MANAGER.getModuleByClass(AutoFirework.class);
         if (autoFirework != null && autoFirework.isEnabled()) {
            ItemStack stack = (ItemStack)(Object)this;
            if (stack.isOf(Items.FIREWORK_ROCKET)) {
               try {
                  Field antiConsumeField = autoFirework.getClass().getDeclaredField("antiConsume");
                  antiConsumeField.setAccessible(true);
                  Object antiConsumeSetting = antiConsumeField.get(autoFirework);
                  Method getValueMethod = antiConsumeSetting.getClass().getMethod("getValue");
                  boolean antiConsumeEnabled = (Boolean)getValueMethod.invoke(antiConsumeSetting);
                  if (antiConsumeEnabled) {
                     ci.cancel();
                  }
               } catch (Exception var9) {
               }
            }
         }
      }

   }
}
