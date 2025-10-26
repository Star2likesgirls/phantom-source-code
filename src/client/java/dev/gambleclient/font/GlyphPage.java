package dev.gambleclient.font;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import javax.imageio.ImageIO;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat;
import org.lwjgl.BufferUtils;

public final class GlyphPage {
   private int imageSize;
   private int maxHeight = -1;
   private final Font font;
   private final boolean antiAlias;
   private final boolean fractionalMetrics;
   private final HashMap glyphs = new HashMap();
   private BufferedImage img;
   private AbstractTexture texture;

   public GlyphPage(Font font, boolean antiAlias, boolean fractionalMetrics) {
      this.font = font;
      this.antiAlias = antiAlias;
      this.fractionalMetrics = fractionalMetrics;
   }

   public void generate(char[] chars) {
      double width = (double)-1.0F;
      double height = (double)-1.0F;
      FontRenderContext frc = new FontRenderContext(new AffineTransform(), this.antiAlias, this.fractionalMetrics);

      for(char item : chars) {
         Rectangle2D bounds = this.font.getStringBounds(Character.toString(item), frc);
         if (width < bounds.getWidth()) {
            width = bounds.getWidth();
         }

         if (height < bounds.getHeight()) {
            height = bounds.getHeight();
         }
      }

      double maxWidth = width + (double)2.0F;
      double maxHeight = height + (double)2.0F;
      this.imageSize = (int)Math.ceil(Math.max(Math.ceil(Math.sqrt(maxWidth * maxWidth * (double)chars.length) / maxWidth), Math.ceil(Math.sqrt(maxHeight * maxHeight * (double)chars.length) / maxHeight)) * Math.max(maxWidth, maxHeight)) + 1;
      this.img = new BufferedImage(this.imageSize, this.imageSize, 2);
      Graphics2D graphics = this.img.createGraphics();
      graphics.setFont(this.font);
      graphics.setColor(new Color(255, 255, 255, 0));
      graphics.fillRect(0, 0, this.imageSize, this.imageSize);
      graphics.setColor(Color.white);
      graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, this.fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, this.antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
      graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, this.antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
      FontMetrics metrics = graphics.getFontMetrics();
      int currentHeight = 0;
      int posX = 0;
      int posY = 1;

      for(char c : chars) {
         Glyph glyph = new Glyph();
         Rectangle2D bounds = metrics.getStringBounds(Character.toString(c), graphics);
         glyph.width = bounds.getBounds().width + 8;
         glyph.height = bounds.getBounds().height;
         if (posX + glyph.width >= this.imageSize) {
            posX = 0;
            posY += currentHeight;
            currentHeight = 0;
         }

         glyph.x = posX;
         glyph.y = posY;
         if (glyph.height > this.maxHeight) {
            this.maxHeight = glyph.height;
         }

         if (glyph.height > currentHeight) {
            currentHeight = glyph.height;
         }

         graphics.drawString(Character.toString(c), posX + 2, posY + metrics.getAscent());
         posX += glyph.width;
         this.glyphs.put(c, glyph);
      }

   }

   public void setup() {
      try {
         ByteArrayOutputStream output = new ByteArrayOutputStream();
         ImageIO.write(this.img, "png", output);
         byte[] byteArray = output.toByteArray();
         ByteBuffer data = BufferUtils.createByteBuffer(byteArray.length).put(byteArray);
         data.flip();
         this.texture = new NativeImageBackedTexture(NativeImage.read(data));
      } catch (Throwable _t) {
         _t.printStackTrace(System.err);
      }

   }

   public void bind() {
      RenderSystem.setShaderTexture(0, this.texture.getGlId());
   }

   public void unbind() {
      RenderSystem.setShaderTexture(0, 0);
   }

   public float drawChar(MatrixStack stack, char ch, float x, float y, float r, float b, float g, float alpha) {
      Glyph glyph = (Glyph)this.glyphs.get(ch);
      if (glyph == null) {
         return 0.0F;
      } else {
         float pageX = (float)glyph.x / (float)this.imageSize;
         float pageY = (float)glyph.y / (float)this.imageSize;
         float pageWidth = (float)glyph.width / (float)this.imageSize;
         float pageHeight = (float)glyph.height / (float)this.imageSize;
         float width = (float)glyph.width;
         float height = (float)glyph.height;
         RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
         this.bind();
         BufferBuilder builder = Tessellator.getInstance().begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         builder.vertex(stack.peek().getPositionMatrix(), x, y + height, 0.0F).color(r, g, b, alpha).texture(pageX, pageY + pageHeight);
         builder.vertex(stack.peek().getPositionMatrix(), x + width, y + height, 0.0F).color(r, g, b, alpha).texture(pageX + pageWidth, pageY + pageHeight);
         builder.vertex(stack.peek().getPositionMatrix(), x + width, y, 0.0F).color(r, g, b, alpha).texture(pageX + pageWidth, pageY);
         builder.vertex(stack.peek().getPositionMatrix(), x, y, 0.0F).color(r, g, b, alpha).texture(pageX, pageY);
         BufferRenderer.drawWithGlobalProgram(builder.end());
         this.unbind();
         return width - 8.0F;
      }
   }

   public float getWidth(char c) {
      return (float)((Glyph)this.glyphs.get(c)).width;
   }

   public boolean isAntiAlias() {
      return this.antiAlias;
   }

   public boolean isFractionalMetrics() {
      return this.fractionalMetrics;
   }

   public int getMaxHeight() {
      return this.maxHeight;
   }

   public static final class Glyph {
      private int x;
      private int y;
      private int width;
      private int height;

      Glyph(int x, int y, int width, int height) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
      }

      Glyph() {
      }

      public int getX() {
         return this.x;
      }

      public int getY() {
         return this.y;
      }

      public int getWidth() {
         return this.width;
      }

      public int getHeight() {
         return this.height;
      }
   }
}
