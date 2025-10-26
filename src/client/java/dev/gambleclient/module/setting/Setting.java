package dev.gambleclient.module.setting;

public abstract class Setting {
   private CharSequence name;
   public CharSequence description;

   public Setting(CharSequence a) {
      this.name = a;
   }

   public void getDescription(CharSequence a) {
      this.name = a;
   }

   public CharSequence getName() {
      return this.name;
   }

   public CharSequence getDescription() {
      return this.description;
   }

   public Setting setDescription(CharSequence description) {
      this.description = description;
      return this;
   }
}
