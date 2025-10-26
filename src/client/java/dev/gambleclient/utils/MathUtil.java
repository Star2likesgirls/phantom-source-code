package dev.gambleclient.utils;

public final class MathUtil {
   public static double roundToNearest(double value, double step) {
      return step * (double)Math.round(value / step);
   }

   public static double smoothStep(double factor, double start, double end) {
      double max = Math.max((double)0.0F, Math.min((double)1.0F, factor));
      return start + (end - start) * max * max * ((double)3.0F - (double)2.0F * max);
   }

   public static double approachValue(float speed, double current, double target) {
      double ceil = Math.ceil(Math.abs(target - current) * (double)speed);
      return current < target ? Math.min(current + (double)((int)ceil), target) : Math.max(current - (double)((int)ceil), target);
   }

   public static double linearInterpolate(double factor, double start, double end) {
      return start + (end - start) * factor;
   }

   public static double exponentialInterpolate(double start, double end, double base, double exponent) {
      return linearInterpolate((double)(1.0F - (float)Math.pow(base, exponent)), start, end);
   }

   public static double clampValue(double value, double min, double max) {
      return Math.max(min, Math.min(value, max));
   }

   public static int clampInt(int value, int min, int max) {
      return Math.max(min, Math.min(value, max));
   }
}
