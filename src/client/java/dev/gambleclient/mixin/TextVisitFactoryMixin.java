package dev.gambleclient.mixin;

import dev.gambleclient.Gamble;
import dev.gambleclient.module.modules.misc.NameProtect;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin({TextVisitFactory.class})
public class TextVisitFactoryMixin {
   @ModifyArg(
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
   ordinal = 0
),
      method = {"visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z"},
      index = 0
   )
   private static String adjustText(String s) {
      NameProtect nameprotect = (NameProtect)Gamble.INSTANCE.MODULE_MANAGER.getModuleByClass(NameProtect.class);
      return nameprotect.isEnabled() && s.contains(Gamble.mc.getSession().getUsername()) ? s.replace(Gamble.mc.getSession().getUsername(), nameprotect.getFakeName()) : s;
   }
}
