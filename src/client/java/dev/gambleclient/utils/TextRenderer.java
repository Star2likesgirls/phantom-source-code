package dev.gambleclient.utils;

import dev.gambleclient.Gamble;
import dev.gambleclient.font.Fonts;
import dev.gambleclient.module.modules.client.Phantom;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public final class TextRenderer {
   public static void drawString(CharSequence charSequence, DrawContext drawContext, int n, int n2, int n3) {
      if (Phantom.useCustomFont.getValue()) {
         Fonts.FONT.drawString(drawContext.getMatrices(), charSequence, (float)n, (float)n2, n3);
      } else {
         drawLargeString(charSequence, drawContext, n, n2, n3);
      }

   }

   public static int getWidth(CharSequence charSequence) {
      return Phantom.useCustomFont.getValue() ? Fonts.FONT.getStringWidth(charSequence) : Gamble.mc.textRenderer.getWidth(charSequence.toString()) * 2;
   }

   public static void drawCenteredString(CharSequence charSequence, DrawContext drawContext, int n, int n2, int n3) {
      if (Phantom.useCustomFont.getValue()) {
         Fonts.FONT.drawString(drawContext.getMatrices(), charSequence, (float)(n - Fonts.FONT.getStringWidth(charSequence) / 2), (float)n2, n3);
      } else {
         drawCenteredMinecraftText(charSequence, drawContext, n, n2, n3);
      }

   }

   public static void drawLargeString(CharSequence charSequence, DrawContext drawContext, int n, int n2, int n3) {
      MatrixStack matrices = drawContext.getMatrices();
      matrices.push();
      matrices.scale(2.0F, 2.0F, 2.0F);
      drawContext.drawText(Gamble.mc.textRenderer, charSequence.toString(), n / 2, n2 / 2, n3, false);
      matrices.scale(1.0F, 1.0F, 1.0F);
      matrices.pop();
   }

   public static void drawCenteredMinecraftText(CharSequence charSequence, DrawContext drawContext, int n, int n2, int n3) {
      MatrixStack matrices = drawContext.getMatrices();
      matrices.push();
      matrices.scale(2.0F, 2.0F, 2.0F);
      drawContext.drawText(Gamble.mc.textRenderer, (String)charSequence, n / 2 - Gamble.mc.textRenderer.getWidth((String)charSequence) / 2, n2 / 2, n3, false);
      matrices.scale(1.0F, 1.0F, 1.0F);
      matrices.pop();
   }
}
