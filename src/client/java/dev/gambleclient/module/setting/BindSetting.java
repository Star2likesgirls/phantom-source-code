package dev.gambleclient.module.setting;

public final class BindSetting extends Setting {
   private final boolean moduleKey;
   private final int defaultValue;
   private boolean listening;
   private int value;

   public BindSetting(CharSequence name, int value, boolean isModule) {
      super(name);
      this.value = value;
      this.defaultValue = value;
      this.moduleKey = isModule;
   }

   public boolean isModuleKey() {
      return this.moduleKey;
   }

   public boolean isListening() {
      return this.listening;
   }

   public int getDefaultValue() {
      return this.defaultValue;
   }

   public void setListening(boolean listening) {
      this.listening = listening;
   }

   public int getValue() {
      return this.value;
   }

   public void setValue(int a) {
      this.value = a;
   }

   public void toggleListening() {
      this.listening = !this.listening;
   }

   public BindSetting setDescription(CharSequence charSequence) {
      super.setDescription(charSequence);
      return this;
   }
}
