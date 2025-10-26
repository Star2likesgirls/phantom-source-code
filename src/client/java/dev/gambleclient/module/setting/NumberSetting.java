package dev.gambleclient.module.setting;

public final class NumberSetting extends Setting {
   private final double min;
   private final double max;
   private double value;
   private final double format;
   private final double defaultValue;

   public NumberSetting(CharSequence charSequence, double min, double max, double defaultValue, double format) {
      super(charSequence);
      this.min = min;
      this.max = max;
      this.value = defaultValue;
      this.format = format;
      this.defaultValue = defaultValue;
   }

   public double getValue() {
      return this.value;
   }

   public double getDefaultValue() {
      return this.defaultValue;
   }

   public double getFormat() {
      return this.format;
   }

   public double getMin() {
      return this.min;
   }

   public double getMax() {
      return this.max;
   }

   public int getIntValue() {
      return (int)this.value;
   }

   public float getFloatValue() {
      return (float)this.value;
   }

   public long getLongValue() {
      return (long)this.value;
   }

   public void getValue(double b) {
      double n = (double)1.0F / this.format;
      this.value = (double)Math.round(Math.max(this.min, Math.min(this.max, b)) * n) / n;
   }

   public NumberSetting getValue(CharSequence charSequence) {
      super.setDescription(charSequence);
      return this;
   }
}
