package dev.gambleclient.module.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.Render2DEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.RenderUtils;
import dev.gambleclient.utils.TextRenderer;
import java.awt.Color;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.VertexFormat;

public final class RegionMap extends Module {
   private final NumberSetting x = new NumberSetting(EncryptedString.of("X"), (double)0.0F, (double)1920.0F, (double)10.0F, (double)1.0F);
   private final NumberSetting y = new NumberSetting(EncryptedString.of("Y"), (double)0.0F, (double)1080.0F, (double)10.0F, (double)1.0F);
   private final NumberSetting scale = new NumberSetting(EncryptedString.of("Scale"), (double)15.0F, (double)40.0F, (double)20.0F, (double)1.0F);
   private final NumberSetting textScale = new NumberSetting(EncryptedString.of("Text Scale"), (double)0.5F, (double)2.0F, 0.7, 0.1);
   private final BooleanSetting showCoords = new BooleanSetting(EncryptedString.of("Show Coords"), true);
   private final BooleanSetting showRegionNames = new BooleanSetting(EncryptedString.of("Show Region Names"), true);
   private final BooleanSetting showGrid = new BooleanSetting(EncryptedString.of("Show Grid"), true);
   private final NumberSetting bgRed = new NumberSetting(EncryptedString.of("BG Red"), (double)0.0F, (double)255.0F, (double)30.0F, (double)1.0F);
   private final NumberSetting bgGreen = new NumberSetting(EncryptedString.of("BG Green"), (double)0.0F, (double)255.0F, (double)30.0F, (double)1.0F);
   private final NumberSetting bgBlue = new NumberSetting(EncryptedString.of("BG Blue"), (double)0.0F, (double)255.0F, (double)30.0F, (double)1.0F);
   private final NumberSetting bgAlpha = new NumberSetting(EncryptedString.of("BG Alpha"), (double)0.0F, (double)255.0F, (double)120.0F, (double)1.0F);
   private final NumberSetting arrowRed = new NumberSetting(EncryptedString.of("Arrow Red"), (double)0.0F, (double)255.0F, (double)255.0F, (double)1.0F);
   private final NumberSetting arrowGreen = new NumberSetting(EncryptedString.of("Arrow Green"), (double)0.0F, (double)255.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting arrowBlue = new NumberSetting(EncryptedString.of("Arrow Blue"), (double)0.0F, (double)255.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting gridRed = new NumberSetting(EncryptedString.of("Grid Red"), (double)0.0F, (double)255.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting gridGreen = new NumberSetting(EncryptedString.of("Grid Green"), (double)0.0F, (double)255.0F, (double)0.0F, (double)1.0F);
   private final NumberSetting gridBlue = new NumberSetting(EncryptedString.of("Grid Blue"), (double)0.0F, (double)255.0F, (double)0.0F, (double)1.0F);
   private final int[][] regionData = new int[][]{{82, 5}, {100, 3}, {101, 3}, {102, 3}, {103, 2}, {104, 2}, {105, 2}, {106, 2}, {91, 2}, {83, 5}, {44, 3}, {75, 3}, {42, 3}, {41, 2}, {40, 2}, {39, 2}, {38, 2}, {92, 2}, {84, 5}, {45, 3}, {14, 3}, {13, 3}, {12, 2}, {11, 2}, {10, 2}, {37, 2}, {93, 2}, {85, 5}, {46, 5}, {74, 5}, {3, 3}, {2, 2}, {1, 2}, {25, 2}, {36, 2}, {94, 2}, {86, 4}, {47, 4}, {72, 4}, {71, 4}, {5, 2}, {4, 2}, {24, 2}, {35, 2}, {95, 2}, {87, 4}, {51, 1}, {17, 1}, {9, 0}, {8, 0}, {7, 0}, {23, 0}, {34, 0}, {96, 2}, {88, 4}, {54, 1}, {18, 1}, {61, 0}, {62, 0}, {21, 0}, {22, 0}, {33, 0}, {97, 0}, {89, 0}, {26, 1}, {27, 0}, {28, 0}, {29, 0}, {30, 0}, {59, 0}, {32, 0}, {98, 0}, {90, 0}, {107, 1}, {108, 1}, {109, 1}, {110, 1}, {111, 1}, {112, 1}, {113, 1}, {99, 0}};
   private final Color[] regionColors = new Color[]{new Color(159, 206, 99), new Color(0, 166, 99), new Color(79, 173, 234), new Color(47, 110, 186), new Color(245, 194, 66), new Color(252, 136, 3)};
   private final String[] regionNames = new String[]{"EU Central", "EU West", "NA East", "NA West", "Asia", "Oceania"};

   public RegionMap() {
      super(EncryptedString.of("Region Map"), EncryptedString.of("Shows DonutSMP region map with your position"), -1, Category.RENDER);
      this.addSettings(new Setting[]{this.x, this.y, this.scale, this.textScale, this.showCoords, this.showRegionNames, this.showGrid, this.bgRed, this.bgGreen, this.bgBlue, this.bgAlpha, this.arrowRed, this.arrowGreen, this.arrowBlue, this.gridRed, this.gridGreen, this.gridBlue});
   }

   @EventListener
   public void onRender2D(Render2DEvent event) {
      if (this.mc.player != null) {
         DrawContext context = event.context;
         MatrixStack matrices = context.getMatrices();
         int mapX = this.x.getIntValue();
         int mapY = this.y.getIntValue();
         int cellSize = this.scale.getIntValue();
         int mapWidth = 9 * cellSize;
         int mapHeight = 9 * cellSize;
         Color bgColor = new Color(this.bgRed.getIntValue(), this.bgGreen.getIntValue(), this.bgBlue.getIntValue(), this.bgAlpha.getIntValue());
         RenderUtils.renderRoundedQuad(matrices, bgColor, (double)mapX, (double)mapY, (double)(mapX + mapWidth), (double)(mapY + mapHeight), (double)0.0F, (double)15.0F);

         for(int row = 0; row < 9; ++row) {
            for(int col = 0; col < 9; ++col) {
               Color rawRegionColor = this.regionColors[this.regionData[row * 9 + col][1]];
               Color regionColor = new Color(rawRegionColor.getRed(), rawRegionColor.getGreen(), rawRegionColor.getBlue(), this.bgAlpha.getIntValue());
               RenderUtils.renderRoundedQuad(matrices, regionColor, (double)(mapX + col * cellSize + 1), (double)(mapY + row * cellSize + 1), (double)(mapX + (col + 1) * cellSize), (double)(mapY + (row + 1) * cellSize), (double)0.0F, (double)15.0F);
            }
         }

         if (this.showGrid.getValue()) {
            Color gridCol = new Color(this.gridRed.getIntValue(), this.gridGreen.getIntValue(), this.gridBlue.getIntValue());

            for(int i = 1; i < 9; ++i) {
               RenderUtils.renderRoundedQuad(matrices, gridCol, (double)(mapX + i * cellSize), (double)mapY, (double)(mapX + i * cellSize + 1), (double)(mapY + mapHeight), (double)0.0F, (double)15.0F);
               RenderUtils.renderRoundedQuad(matrices, gridCol, (double)mapX, (double)(mapY + i * cellSize), (double)(mapX + mapWidth), (double)(mapY + i * cellSize + 1), (double)0.0F, (double)15.0F);
            }
         }

         for(int row = 0; row < 9; ++row) {
            for(int col = 0; col < 9; ++col) {
               String text = String.valueOf(this.regionData[row * 9 + col][0]);
               float scaledWidth = (float)TextRenderer.getWidth(text) * (float)this.textScale.getValue();
               float scaledHeight = 8.0F * (float)this.textScale.getValue();
               float textX = (float)(mapX + col * cellSize) + ((float)cellSize - scaledWidth) / 2.0F;
               float textY = (float)(mapY + row * cellSize) + ((float)cellSize - scaledHeight) / 2.0F;
               matrices.push();
               matrices.translate(textX, textY, 0.0F);
               matrices.scale((float)this.textScale.getValue(), (float)this.textScale.getValue(), 1.0F);
               TextRenderer.drawString(text, context, 0, 0, Color.WHITE.getRGB());
               matrices.pop();
            }
         }

         double pX = this.mc.player.getX();
         double pZ = this.mc.player.getZ();
         int gridX = (int)((pX + (double)225000.0F) / (double)50000.0F);
         int gridZ = (int)((pZ + (double)225000.0F) / (double)50000.0F);
         if (gridX >= 0 && gridX < 9 && gridZ >= 0 && gridZ < 9) {
            int arrowX = mapX + (int)((pX + (double)225000.0F) % (double)225000.0F / (double)225000.0F * (double)mapWidth);
            int arrowY = mapY + (int)((pZ + (double)225000.0F) % (double)225000.0F / (double)225000.0F * (double)mapHeight);
            this.drawPlayerArrow(matrices, arrowX, arrowY, this.mc.player.getYaw());
         }

         int infoY = mapY + mapHeight + 5;
         if (this.showCoords.getValue()) {
            TextRenderer.drawString(String.format("X: %d, Z: %d", (int)pX, (int)pZ), context, mapX, infoY, Color.WHITE.getRGB());
            infoY += 15;
            int region = this.getCurrentRegion(pX, pZ);
            if (region != -1) {
               TextRenderer.drawString("Region: " + region, context, mapX, infoY, Color.WHITE.getRGB());
               infoY += 15;
            }
         }

         if (this.showRegionNames.getValue()) {
            infoY += 5;

            for(int i = 0; i < this.regionNames.length; ++i) {
               RenderUtils.renderRoundedQuad(matrices, this.regionColors[i], (double)mapX, (double)(infoY + i * 15), (double)(mapX + 12), (double)(infoY + i * 15 + 12), (double)2.0F, (double)15.0F);
               TextRenderer.drawString(this.regionNames[i], context, mapX + 17, infoY + i * 15 + 2, Color.WHITE.getRGB());
            }
         }

      }
   }

   private void drawPlayerArrow(MatrixStack matrices, int centerX, int centerY, float yaw) {
      matrices.push();
      matrices.translate((float)centerX, (float)centerY, 0.0F);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw - 90.0F));
      Color color = new Color(this.arrowRed.getIntValue(), this.arrowGreen.getIntValue(), this.arrowBlue.getIntValue());
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      RenderSystem.enableBlend();
      BufferBuilder buffer = Tessellator.getInstance().begin(net.minecraft.client.render.VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      int size = 6;
      buffer.vertex(matrices.peek().getPositionMatrix(), 0.0F, (float)(-size), 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), 255);
      buffer.vertex(matrices.peek().getPositionMatrix(), (float)(-size), (float)size, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), 255);
      buffer.vertex(matrices.peek().getPositionMatrix(), (float)size, (float)size, 0.0F).color(color.getRed(), color.getGreen(), color.getBlue(), 255);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.disableBlend();
      matrices.pop();
   }

   private int getCurrentRegion(double x, double z) {
      int gridX = (int)((x + (double)225000.0F) / (double)50000.0F);
      int gridZ = (int)((z + (double)225000.0F) / (double)50000.0F);
      return gridX >= 0 && gridX < 9 && gridZ >= 0 && gridZ < 9 ? this.regionData[gridZ * 9 + gridX][0] : -1;
   }
}
