package dev.gambleclient.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.gambleclient.Gamble;
import java.awt.Color;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexFormat;

public final class GambleRenderUtils {
   public static void drawBox(MatrixStack matrices, Box box, Color color) {
      Vec3d cameraPos = Gamble.mc.gameRenderer.getCamera().getPos();
      Box relativeBox = box.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      WorldRenderer.drawBox(matrices, buffer, (double)((float)relativeBox.minX), (double)((float)relativeBox.minY), (double)((float)relativeBox.minZ), (double)((float)relativeBox.maxX), (double)((float)relativeBox.maxY), (double)((float)relativeBox.maxZ), (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.disableBlend();
   }

   public static void drawOutline(MatrixStack matrices, Box box, Color color) {
      Vec3d cameraPos = Gamble.mc.gameRenderer.getCamera().getPos();
      Box relativeBox = box.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      WorldRenderer.drawBox(matrices, buffer, relativeBox, (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, 1.0F);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.disableBlend();
   }
}
