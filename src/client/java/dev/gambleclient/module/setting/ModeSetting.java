package dev.gambleclient.module.setting;

import java.util.Arrays;
import java.util.List;

public final class ModeSetting extends Setting {
   public int index;
   private final List possibleValues;
   private final int originalValue;

   public ModeSetting(CharSequence charSequence, Enum defaultValue, Class type) {
      super(charSequence);
      Class<?> enumClass = type;
      this.possibleValues = Arrays.asList((Enum[])enumClass.getEnumConstants());
      this.index = this.possibleValues.indexOf(defaultValue);
      this.originalValue = this.index;
   }

   public Enum getValue() {
      return (Enum)this.possibleValues.get(this.index);
   }

   public void setMode(Enum enum1) {
      this.index = this.possibleValues.indexOf(enum1);
   }

   public void setModeIndex(int a) {
      this.index = a;
   }

   public int getModeIndex() {
      return this.index;
   }

   public int getOriginalValue() {
      return this.originalValue;
   }

   public void cycleUp() {
      if (this.index < this.possibleValues.size() - 1) {
         ++this.index;
      } else {
         this.index = 0;
      }

   }

   public void cycleDown() {
      if (this.index > 0) {
         --this.index;
      } else {
         this.index = this.possibleValues.size() - 1;
      }

   }

   public boolean isMode(Enum enum1) {
      return this.index == this.possibleValues.indexOf(enum1);
   }

   public List getPossibleValues() {
      return this.possibleValues;
   }

   public ModeSetting setDescription(CharSequence charSequence) {
      super.setDescription(charSequence);
      return this;
   }
}
