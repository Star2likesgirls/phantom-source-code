package dev.gambleclient.utils;

import dev.gambleclient.Gamble;
import dev.gambleclient.mixin.ClientPlayerInteractionManagerAccessor;
import java.util.function.Predicate;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class InventoryUtil {
   public static void swap(int selectedSlot) {
      if (selectedSlot >= 0 && selectedSlot <= 8) {
         Gamble.mc.player.getInventory().selectedSlot = selectedSlot;
         ((ClientPlayerInteractionManagerAccessor)Gamble.mc.interactionManager).syncSlot();
      }
   }

   public static boolean swapStack(Predicate predicate) {
      PlayerInventory getInventory = Gamble.mc.player.getInventory();

      for(int i = 0; i < 9; ++i) {
         if (predicate.test(getInventory.getStack(i))) {
            getInventory.selectedSlot = i;
            return true;
         }
      }

      return false;
   }

   public static boolean swapItem(Predicate predicate) {
      PlayerInventory getInventory = Gamble.mc.player.getInventory();

      for(int i = 0; i < 9; ++i) {
         if (predicate.test(getInventory.getStack(i).getItem())) {
            getInventory.selectedSlot = i;
            return true;
         }
      }

      return false;
   }

   public static boolean swap(Item item) {
      return swapItem((item2) -> item2 == item);
   }

   public static int getSlot(Item obj) {
      ScreenHandler currentScreenHandler = Gamble.mc.player.currentScreenHandler;
      if (Gamble.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
         int n = 0;

         for(int i = 0; i < ((GenericContainerScreenHandler)Gamble.mc.player.currentScreenHandler).getRows() * 9; ++i) {
            if (currentScreenHandler.getSlot(i).getStack().getItem().equals(obj)) {
               ++n;
            }
         }

         return n;
      } else {
         return 0;
      }
   }
}
