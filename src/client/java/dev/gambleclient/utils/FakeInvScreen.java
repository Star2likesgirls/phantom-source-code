package dev.gambleclient.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

public class FakeInvScreen extends InventoryScreen {
   public FakeInvScreen(PlayerEntity playerEntity) {
      super(playerEntity);
   }

   protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return false;
   }
}
