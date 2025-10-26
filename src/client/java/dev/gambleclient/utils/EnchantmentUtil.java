package dev.gambleclient.utils;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.DataComponentTypes;

public class EnchantmentUtil {
   public static boolean hasEnchantment(ItemStack itemStack, RegistryKey registryKey) {
      if (itemStack.isEmpty()) {
         return false;
      } else {
         Object2IntArrayMap<?> enchantmentMap = new Object2IntArrayMap();
         populateEnchantmentMap(itemStack, enchantmentMap);
         return containsEnchantment(enchantmentMap, registryKey);
      }
   }

   private static boolean containsEnchantment(Object2IntMap enchantmentMap, RegistryKey registryKey) {
      ObjectIterator var2 = enchantmentMap.keySet().iterator();

      while(var2.hasNext()) {
         Object enchantment = var2.next();
         if (((RegistryEntry)enchantment).matchesKey(registryKey)) {
            return true;
         }
      }

      return false;
   }

   public static void populateEnchantmentMap(ItemStack itemStack, Object2IntMap enchantmentMap) {
      enchantmentMap.clear();
      if (!itemStack.isEmpty()) {
         Set<?> enchantments;
         if (itemStack.getItem() == Items.ENCHANTED_BOOK) {
            enchantments = ((ItemEnchantmentsComponent)itemStack.get(DataComponentTypes.STORED_ENCHANTMENTS)).getEnchantmentEntries();
         } else {
            enchantments = itemStack.getEnchantments().getEnchantmentEntries();
         }

         for(Object enchantmentEntry : enchantments) {
            enchantmentMap.put(((Object2IntMap.Entry)enchantmentEntry).getKey(), ((Object2IntMap.Entry)enchantmentEntry).getIntValue());
         }
      }

   }
}
