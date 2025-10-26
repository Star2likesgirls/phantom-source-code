package dev.gambleclient.utils;

import java.awt.Color;

public final class ColorUtil {
   public static Color a(int n, int a) {
      Color hsbColor = Color.getHSBColor((float)((System.currentTimeMillis() * 3L + (long)(n * 175)) % 7200L) / 7200.0F, 0.6F, 1.0F);
      return new Color(hsbColor.getRed(), hsbColor.getGreen(), hsbColor.getBlue(), a);
   }

   public static Color alphaStep_Skidded_From_Prestige_Client_NumberOne(Color color, int n, int n2) {
      float[] hsbvals = new float[3];
      Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsbvals);
      hsbvals[2] = 0.25F + 0.75F * Math.abs(((float)(System.currentTimeMillis() % 2000L) / 1000.0F + (float)n / (float)n2 * 2.0F) % 2.0F - 1.0F) % 2.0F;
      int hsBtoRGB = Color.HSBtoRGB(hsbvals[0], hsbvals[1], hsbvals[2]);
      return new Color(hsBtoRGB >> 16 & 255, hsBtoRGB >> 8 & 255, hsBtoRGB & 255, color.getAlpha());
   }

   public static Color a(float n, Color color, Color color2) {
      return new Color((int)MathUtil.approachValue(n, (double)color2.getRed(), (double)color.getRed()), (int)MathUtil.approachValue(n, (double)color2.getGreen(), (double)color.getGreen()), (int)MathUtil.approachValue(n, (double)color2.getBlue(), (double)color.getBlue()));
   }

   public static Color a(float n, int n2, Color color) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)MathUtil.approachValue(n, (double)color.getAlpha(), (double)n2));
   }

   public static Color a(Color color, Color color2, float n) {
      return new Color(a(Math.round((float)color.getRed() + n * (float)(color2.getRed() - color.getRed())), 0, 255), a(Math.round((float)color.getGreen() + n * (float)(color2.getGreen() - color.getGreen())), 0, 255), a(Math.round((float)color.getBlue() + n * (float)(color2.getBlue() - color.getBlue())), 0, 255), a(Math.round((float)color.getAlpha() + n * (float)(color2.getAlpha() - color.getAlpha())), 0, 255));
   }

   private static int a(int b, int a, int a2) {
      return Math.max(a, Math.min(a2, b));
   }
}
