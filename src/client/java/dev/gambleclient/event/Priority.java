package dev.gambleclient.event;

public enum Priority {
   HIGHEST(0),
   HIGH(1),
   NORMAL(2),
   LOW(3),
   LOWEST(4);

   private final int value;

   private Priority(final int value) {
      this.value = value;
   }

   public int getValue() {
      return this.value;
   }

   // $FF: synthetic method
   private static Priority[] $values() {
      return new Priority[]{HIGHEST, HIGH, NORMAL, LOW, LOWEST};
   }
}
