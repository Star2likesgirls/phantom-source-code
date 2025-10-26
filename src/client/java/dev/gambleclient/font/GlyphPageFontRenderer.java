package dev.gambleclient.font;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.gambleclient.utils.EncryptedString;
import java.awt.Font;
import java.io.InputStream;
import java.util.Objects;
import java.util.Random;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexFormat;

public final class GlyphPageFontRenderer {
   public Random random = new Random();
   private float posX;
   private float posY;
   private final int[] colorCode = new int[32];
   private boolean isBold;
   private boolean isItalic;
   private boolean isUnderline;
   private boolean isStrikethrough;
   private final GlyphPage regular;
   private final GlyphPage bold;
   private final GlyphPage italic;
   private final GlyphPage boldItalic;

   public GlyphPageFontRenderer(GlyphPage regular, GlyphPage bold, GlyphPage italic, GlyphPage boldItalic) {
      this.regular = regular;
      this.bold = bold;
      this.italic = italic;
      this.boldItalic = boldItalic;

      for(int n = 0; n < 32; ++n) {
         int j = (n >> 3 & 1) * 85;
         int k = (n >> 2 & 1) * 170 + j;
         int l = (n >> 1 & 1) * 170 + j;
         int m = (n & 1) * 170 + j;
         if (n == 6) {
            k += 85;
         }

         if (n >= 16) {
            k /= 4;
            l /= 4;
            m /= 4;
         }

         this.colorCode[n] = (k & 255) << 16 | (l & 255) << 8 | m & 255;
      }

   }

   public static GlyphPageFontRenderer a(CharSequence font, int size, boolean bold, boolean italic, boolean boldItalic) {
      char[] chars = new char[256];

      for(int i = 0; i < 256; ++i) {
         chars[i] = (char)i;
      }

      GlyphPage regularPage = new GlyphPage(new Font(font.toString(), 0, size), true, true);
      regularPage.generate(chars);
      regularPage.setup();
      GlyphPage boldPage = regularPage;
      GlyphPage italicPage = regularPage;
      GlyphPage boldItalicPage = regularPage;
      if (bold) {
         boldPage = new GlyphPage(new Font(font.toString(), 1, size), true, true);
         boldPage.generate(chars);
         boldPage.setup();
      }

      if (italic) {
         italicPage = new GlyphPage(new Font(font.toString(), 2, size), true, true);
         italicPage.generate(chars);
         italicPage.setup();
      }

      if (boldItalic) {
         boldItalicPage = new GlyphPage(new Font(font.toString(), 3, size), true, true);
         boldItalicPage.generate(chars);
         boldItalicPage.setup();
      }

      return new GlyphPageFontRenderer(regularPage, boldPage, italicPage, boldItalicPage);
   }

   public static GlyphPageFontRenderer init(CharSequence id, int size, boolean bold, boolean italic, boolean boldItalic) {
      try {
         char[] chars = new char[256];

         for(int i = 0; i < chars.length; ++i) {
            chars[i] = (char)i;
         }

         Font font = Font.createFont(0, (InputStream)Objects.requireNonNull(GlyphPageFontRenderer.class.getResourceAsStream(id.toString()))).deriveFont(0, (float)size);
         GlyphPage regularPage = new GlyphPage(font, true, true);
         regularPage.generate(chars);
         regularPage.setup();
         GlyphPage boldPage = regularPage;
         GlyphPage italicPage = regularPage;
         GlyphPage boldItalicPage = regularPage;
         if (bold) {
            boldPage = new GlyphPage(Font.createFont(0, (InputStream)Objects.requireNonNull(GlyphPageFontRenderer.class.getResourceAsStream(id.toString()))).deriveFont(1, (float)size), true, true);
            boldPage.generate(chars);
            boldPage.setup();
         }

         if (italic) {
            italicPage = new GlyphPage(Font.createFont(0, (InputStream)Objects.requireNonNull(GlyphPageFontRenderer.class.getResourceAsStream(id.toString()))).deriveFont(2, (float)size), true, true);
            italicPage.generate(chars);
            italicPage.setup();
         }

         if (boldItalic) {
            boldItalicPage = new GlyphPage(Font.createFont(0, (InputStream)Objects.requireNonNull(GlyphPageFontRenderer.class.getResourceAsStream(id.toString()))).deriveFont(3, (float)size), true, true);
            boldItalicPage.generate(chars);
            boldItalicPage.setup();
         }

         return new GlyphPageFontRenderer(regularPage, boldPage, italicPage, boldItalicPage);
      } catch (Throwable _t) {
         _t.printStackTrace(System.err);
         return null;
      }
   }

   public int drawStringWithShadow(MatrixStack matrices, CharSequence text, float x, float y, int color) {
      return this.drawString(matrices, text, x, y, color, true);
   }

   public int drawStringWithShadow(MatrixStack matrices, CharSequence text, double x, double y, int color) {
      return this.drawString(matrices, text, (float)x, (float)y, color, true);
   }

   public int drawString(MatrixStack matrices, CharSequence text, float x, float y, int color) {
      return this.drawString(matrices, text, x, y, color, false);
   }

   public int drawString(MatrixStack matrices, CharSequence text, double x, double y, int color) {
      return this.drawString(matrices, text, (float)x, (float)y, color, false);
   }

   public int drawCenteredString(MatrixStack matrices, CharSequence text, double x, double y, float scale, int color) {
      return this.drawString(matrices, text, (float)x - (float)(this.getStringWidth(text) / 2), (float)y, scale, color, false);
   }

   public int drawCenteredString(MatrixStack matrices, CharSequence text, double x, double y, int color) {
      return this.drawString(matrices, text, (float)x - (float)(this.getStringWidth(text) / 2), (float)y, color, false);
   }

   public int drawCenteredStringWithShadow(MatrixStack matrices, CharSequence text, double x, double y, int color) {
      return this.drawString(matrices, text, (float)x - (float)(this.getStringWidth(text) / 2), (float)y, color, true);
   }

   public int drawString(MatrixStack matrices, CharSequence text, float x, float y, float scale, int color, boolean shadow) {
      this.resetStyles();
      return shadow ? Math.max(this.renderString(matrices, text, x + 1.0F, y + 1.0F, scale, color, true), this.renderString(matrices, text, x, y, scale, color, false)) : this.renderString(matrices, text, x, y, scale, color, false);
   }

   public int drawString(MatrixStack matrices, CharSequence text, float x, float y, int color, boolean shadow) {
      this.resetStyles();
      return shadow ? Math.max(this.renderString(matrices, text, x + 1.0F, y + 1.0F, color, true), this.renderString(matrices, text, x, y, color, false)) : this.renderString(matrices, text, x, y, color, false);
   }

   private int renderString(MatrixStack matrices, CharSequence text, float x, float y, int color, boolean shadow) {
      if (text == null) {
         return 0;
      } else {
         if ((color & -67108864) == 0) {
            color |= -16777216;
         }

         if (shadow) {
            color = (color & 16579836) >> 2 | color & -16777216;
         }

         this.posX = x * 2.0F;
         this.posY = y * 2.0F;
         this.a(matrices, text, shadow, color);
         return (int)(this.posX / 4.0F);
      }
   }

   private int renderString(MatrixStack matrices, CharSequence text, float x, float y, float scale, int color, boolean shadow) {
      if (text == null) {
         return 0;
      } else {
         if ((color & -67108864) == 0) {
            color |= -16777216;
         }

         if (shadow) {
            color = (color & 16579836) >> 2 | color & -16777216;
         }

         this.posX = x * 2.0F;
         this.posY = y * 2.0F;
         this.renderStringAtPos(matrices, text, scale, shadow, color);
         return (int)(this.posX / 4.0F);
      }
   }

   private void a(MatrixStack matrices, CharSequence text, boolean shadow, int color) {
      GlyphPage page = this.getPage();
      float g = (float)(color >> 16 & 255) / 255.0F;
      float h = (float)(color >> 8 & 255) / 255.0F;
      float k = (float)(color & 255) / 255.0F;
      matrices.push();
      matrices.scale(0.5F, 0.5F, 0.5F);
      GlStateManager._enableBlend();
      GlStateManager._blendFunc(770, 771);
      page.bind();
      GlStateManager._texParameter(3553, 10240, 9729);

      for(int i = 0; i < text.length(); ++i) {
         char ch = text.charAt(i);
         if (ch == '�' && i + 1 < text.length()) {
            int index = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1)));
            if (index < 16) {
               this.isBold = false;
               this.isStrikethrough = false;
               this.isUnderline = false;
               this.isItalic = false;
               if (index < 0) {
                  index = 15;
               }

               if (shadow) {
                  index += 16;
               }

               int j1 = this.colorCode[index];
               g = (float)(j1 >> 16 & 255) / 255.0F;
               h = (float)(j1 >> 8 & 255) / 255.0F;
               k = (float)(j1 & 255) / 255.0F;
            } else if (index != 16) {
               if (index == 17) {
                  this.isBold = true;
               } else if (index == 18) {
                  this.isStrikethrough = true;
               } else if (index == 19) {
                  this.isUnderline = true;
               } else if (index == 20) {
                  this.isItalic = true;
               } else {
                  this.isBold = false;
                  this.isStrikethrough = false;
                  this.isUnderline = false;
                  this.isItalic = false;
               }
            }

            ++i;
         } else {
            page = this.getPage();
            page.bind();
            this.doDraw(page.drawChar(matrices, ch, this.posX, this.posY, g, k, h, (float)(color >> 24 & 255) / 255.0F), page);
         }
      }

      page.unbind();
      matrices.pop();
   }

   private void renderStringAtPos(MatrixStack matrices, CharSequence text, float scale, boolean shadow, int color) {
      GlyphPage page = this.getPage();
      float g = (float)(color >> 16 & 255) / 255.0F;
      float h = (float)(color >> 8 & 255) / 255.0F;
      float k = (float)(color & 255) / 255.0F;
      matrices.push();
      matrices.scale(scale, scale, scale);
      GlStateManager._enableBlend();
      GlStateManager._blendFunc(770, 771);
      page.bind();
      GlStateManager._texParameter(3553, 10240, 9729);

      for(int i = 0; i < text.length(); ++i) {
         char ch = text.charAt(i);
         if (ch == '�' && i + 1 < text.length()) {
            int index = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1)));
            if (index < 16) {
               this.isBold = false;
               this.isStrikethrough = false;
               this.isUnderline = false;
               this.isItalic = false;
               if (index < 0) {
                  index = 15;
               }

               if (shadow) {
                  index += 16;
               }

               int j1 = this.colorCode[index];
               g = (float)(j1 >> 16 & 255) / 255.0F;
               h = (float)(j1 >> 8 & 255) / 255.0F;
               k = (float)(j1 & 255) / 255.0F;
            } else if (index != 16) {
               if (index == 17) {
                  this.isBold = true;
               } else if (index == 18) {
                  this.isStrikethrough = true;
               } else if (index == 19) {
                  this.isUnderline = true;
               } else if (index == 20) {
                  this.isItalic = true;
               } else {
                  this.isBold = false;
                  this.isStrikethrough = false;
                  this.isUnderline = false;
                  this.isItalic = false;
               }
            }

            ++i;
         } else {
            page = this.getPage();
            page.bind();
            this.doDraw(page.drawChar(matrices, ch, this.posX, this.posY, g, k, h, (float)(color >> 24 & 255) / 255.0F), page);
         }
      }

      page.unbind();
      matrices.pop();
   }

   private void doDraw(float f, GlyphPage page) {
      if (this.isStrikethrough) {
         BufferBuilder buffer = Tessellator.getInstance().begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
         buffer.vertex(this.posX, this.posY + (float)(page.getMaxHeight() / 2), 0.0F);
         buffer.vertex(this.posX + f, this.posY + (float)(page.getMaxHeight() / 2), 0.0F);
         buffer.vertex(this.posX + f, this.posY + (float)(page.getMaxHeight() / 2) - 1.0F, 0.0F);
         buffer.vertex(this.posX, this.posY + (float)(page.getMaxHeight() / 2) - 1.0F, 0.0F);
         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }

      if (this.isUnderline) {
         BufferBuilder buffer = Tessellator.getInstance().begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
         int l = this.isUnderline ? -1 : 0;
         buffer.vertex(this.posX + (float)l, this.posY + (float)page.getMaxHeight(), 0.0F);
         buffer.vertex(this.posX + f, this.posY + (float)page.getMaxHeight(), 0.0F);
         buffer.vertex(this.posX + f, this.posY + (float)page.getMaxHeight() - 1.0F, 0.0F);
         buffer.vertex(this.posX + (float)l, this.posY + (float)page.getMaxHeight() - 1.0F, 0.0F);
         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }

      this.posX += f;
   }

   private GlyphPage getPage() {
      if (this.isBold && this.isItalic) {
         return this.boldItalic;
      } else if (this.isBold) {
         return this.bold;
      } else {
         return this.isItalic ? this.italic : this.regular;
      }
   }

   private void resetStyles() {
      this.isBold = false;
      this.isItalic = false;
      this.isUnderline = false;
      this.isStrikethrough = false;
   }

   public int getHeight() {
      return this.regular.getMaxHeight() / 2;
   }

   public int getStringWidth(CharSequence text) {
      if (text == null) {
         return 0;
      } else {
         int width = 0;
         boolean on = false;

         for(int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (ch == '�') {
               on = true;
            } else if (on && ch >= '0' && ch <= 'r') {
               int index = "0123456789abcdefklmnor".indexOf(ch);
               if (index < 16) {
                  this.isBold = false;
                  this.isItalic = false;
               } else if (index == 17) {
                  this.isBold = true;
               } else if (index == 20) {
                  this.isItalic = true;
               } else if (index == 21) {
                  this.isBold = false;
                  this.isItalic = false;
               }

               ++i;
               on = false;
            } else {
               if (on) {
                  --i;
               }

               width += (int)(this.getPage().getWidth(text.charAt(i)) - 8.0F);
            }
         }

         return width / 2;
      }
   }

   public CharSequence trimStringToWidth(CharSequence text, int width) {
      return this.trimStringToWidth(text, width, false);
   }

   public CharSequence trimStringToWidth(CharSequence text, int maxWidth, boolean reverse) {
      StringBuilder sb = new StringBuilder();
      boolean on = false;
      int j = reverse ? text.length() - 1 : 0;
      int k = reverse ? -1 : 1;

      for(int width = 0; j >= 0 && j < text.length() && j < maxWidth; j += k) {
         char ch = text.charAt(j);
         if (ch == '�') {
            on = true;
         } else if (on && ch >= '0' && ch <= 'r') {
            int index = "0123456789abcdefklmnor".indexOf(ch);
            if (index < 16) {
               this.isBold = false;
               this.isItalic = false;
            } else if (index == 17) {
               this.isBold = true;
            } else if (index == 20) {
               this.isItalic = true;
            } else if (index == 21) {
               this.isBold = false;
               this.isItalic = false;
            }

            ++j;
            on = false;
         } else {
            if (on) {
               --j;
            }

            ch = text.charAt(j);
            width += (int)((this.getPage().getWidth(ch) - 8.0F) / 2.0F);
         }

         if (j > width) {
            break;
         }

         if (reverse) {
            sb.insert(0, ch);
         } else {
            sb.append(ch);
         }
      }

      return EncryptedString.of(sb.toString());
   }
}
