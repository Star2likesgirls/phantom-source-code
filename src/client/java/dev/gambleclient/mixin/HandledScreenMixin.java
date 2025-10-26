package dev.gambleclient.mixin;

import net.minecraft.screen.slot.Slot;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({HandledScreen.class})
public interface HandledScreenMixin {
   @Accessor
   Slot getFocusedSlot();
}
