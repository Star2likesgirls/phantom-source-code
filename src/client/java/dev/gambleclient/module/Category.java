package dev.gambleclient.module;

import dev.gambleclient.utils.EncryptedString;

public enum Category {
   COMBAT(EncryptedString.of("PvP")),
   MISC(EncryptedString.of("Extras")),
   DONUT(EncryptedString.of("Phantom")),
   RENDER(EncryptedString.of("ESP")),
   CLIENT(EncryptedString.of("Other"));

   public final CharSequence name;

   private Category(final CharSequence name) {
      this.name = name;
   }

   // $FF: synthetic method
   private static Category[] $values() {
      return new Category[]{COMBAT, MISC, DONUT, RENDER, CLIENT};
   }
}
