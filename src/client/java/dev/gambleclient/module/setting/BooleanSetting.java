package dev.gambleclient.module.setting;

public final class BooleanSetting extends Setting {
   private boolean value;
   private final boolean defaultValue;

   public BooleanSetting(CharSequence charSequence, boolean value) {
      super(charSequence);
      this.value = value;
      this.defaultValue = value;
   }

   public void toggle() {
      this.setValue(!this.value);
   }

   public void setValue(boolean a) {
      this.value = a;
   }

   public boolean getDefaultValue() {
      return this.defaultValue;
   }

   public boolean getValue() {
      return this.value;
   }

   public BooleanSetting setDescription(CharSequence charSequence) {
      super.setDescription(charSequence);
      return this;
   }
}
