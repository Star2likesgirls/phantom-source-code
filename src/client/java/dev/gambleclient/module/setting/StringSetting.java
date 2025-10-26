package dev.gambleclient.module.setting;

public class StringSetting extends Setting {
   public String value;

   public StringSetting(CharSequence charSequence, String a) {
      super(charSequence);
      this.value = a;
   }

   public void setValue(String a) {
      this.value = a;
   }

   public String getValue() {
      return this.value;
   }

   public StringSetting setDescription(CharSequence charSequence) {
      super.setDescription(charSequence);
      return this;
   }
}
