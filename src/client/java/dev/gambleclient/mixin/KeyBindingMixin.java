package dev.gambleclient.mixin;

import dev.gambleclient.Gamble;
import dev.gambleclient.imixin.IKeybinding;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({KeyBinding.class})
public abstract class KeyBindingMixin implements IKeybinding {
   @Shadow
   private InputUtil.Key field_1655;

   @Shadow
   public abstract void setPressed(boolean var1);

   public boolean krypton$isActuallyPressed() {
      return InputUtil.isKeyPressed(Gamble.mc.getWindow().getHandle(), this.field_1655.getCode());
   }

   public void krypton$resetPressed() {
      this.setPressed(this.krypton$isActuallyPressed());
   }
}
