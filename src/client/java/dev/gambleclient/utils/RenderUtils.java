package dev.gambleclient.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.gambleclient.Gamble;
import dev.gambleclient.module.modules.client.Phantom;
import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.Camera;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.RotationAxis;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.client.render.VertexFormat;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class RenderUtils {
   public static VertexSorter vertexSorter;
   public static boolean rendering3D = true;

   public static Vec3d getCameraPos() {
      return getCamera().getPos();
   }

   public static Camera getCamera() {
      return Gamble.mc.getBlockEntityRenderDispatcher().camera;
   }

   public static double deltaTime() {
      double n;
      if (Gamble.mc.getCurrentFps() > 0) {
         n = (double)1.0F / (double)Gamble.mc.getCurrentFps();
      } else {
         n = (double)1.0F;
      }

      return n;
   }

   public static float fast(float n, float n2, float n3) {
      return (1.0F - MathHelper.clamp((float)(deltaTime() * (double)n3), 0.0F, 1.0F)) * n + MathHelper.clamp((float)(deltaTime() * (double)n3), 0.0F, 1.0F) * n2;
   }

   public static Vec3d getPlayerLookVec(PlayerEntity playerEntity) {
      float cos = MathHelper.cos(playerEntity.getYaw() * ((float)Math.PI / 180F) - (float)Math.PI);
      float sin = MathHelper.sin(playerEntity.getYaw() * ((float)Math.PI / 180F) - (float)Math.PI);
      float cos2 = MathHelper.cos(playerEntity.getPitch() * ((float)Math.PI / 180F));
      return (new Vec3d((double)(sin * cos2), (double)MathHelper.sin(playerEntity.getPitch() * ((float)Math.PI / 180F)), (double)(cos * cos2))).normalize();
   }

   public static void unscaledProjection() {
      vertexSorter = RenderSystem.getVertexSorting();
      RenderSystem.setProjectionMatrix((new Matrix4f()).setOrtho(0.0F, (float)Gamble.mc.getWindow().getFramebufferWidth(), (float)Gamble.mc.getWindow().getFramebufferHeight(), 0.0F, 1000.0F, 21000.0F), VertexSorter.BY_Z);
      rendering3D = false;
   }

   public static void scaledProjection() {
      RenderSystem.setProjectionMatrix((new Matrix4f()).setOrtho(0.0F, (float)((double)Gamble.mc.getWindow().getFramebufferWidth() / Gamble.mc.getWindow().getScaleFactor()), (float)((double)Gamble.mc.getWindow().getFramebufferHeight() / Gamble.mc.getWindow().getScaleFactor()), 0.0F, 1000.0F, 21000.0F), vertexSorter);
      rendering3D = true;
   }

   public static void renderRoundedQuad(MatrixStack matrixStack, Color color, double n, double n2, double n3, double n4, double n5, double n6, double n7, double n8, double n9) {
      int rgb = color.getRGB();
      Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      renderRoundedQuadInternal(positionMatrix, (float)(rgb >> 16 & 255) / 255.0F, (float)(rgb >> 8 & 255) / 255.0F, (float)(rgb & 255) / 255.0F, (float)(rgb >> 24 & 255) / 255.0F, n, n2, n3, n4, n5, n6, n7, n8, n9);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   private static void setup() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
   }

   private static void cleanup() {
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void renderRoundedQuad(MatrixStack matrixStack, Color color, double n, double n2, double n3, double n4, double n5, double n6) {
      renderRoundedQuad(matrixStack, color, n, n2, n3, n4, n5, n5, n5, n5, n6);
   }

   public static void renderRoundedOutlineInternal(Matrix4f matrix4f, float n, float n2, float n3, float n4, double n5, double n6, double n7, double n8, double n9, double n10, double n11, double n12, double n13, double n14) {
      BufferBuilder begin = Tessellator.getInstance().begin(net.minecraft.client.render.VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      double[][] array = new double[][]{{n7 - n12, n8 - n12, n12}, {n7 - n10, n6 + n10, n10}, {n5 + n9, n6 + n9, n9}, {n5 + n11, n8 - n11, n11}};

      for(int i = 0; i < 4; ++i) {
         double[] array2 = array[i];
         double n15 = array2[2];

         for(double angdeg = (double)i * (double)90.0F; angdeg < (double)90.0F + (double)i * (double)90.0F; angdeg += (double)90.0F / n14) {
            double radians = Math.toRadians(angdeg);
            double sin = Math.sin((double)((float)radians));
            double n16 = sin * n15;
            double cos = Math.cos((double)((float)radians));
            double n17 = cos * n15;
            begin.vertex(matrix4f, (float)array2[0] + (float)n16, (float)array2[1] + (float)n17, 0.0F).color(n, n2, n3, n4);
            begin.vertex(matrix4f, (float)(array2[0] + (double)((float)n16) + sin * n13), (float)(array2[1] + (double)((float)n17) + cos * n13), 0.0F).color(n, n2, n3, n4);
         }

         double radians2 = Math.toRadians((double)90.0F + (double)i * (double)90.0F);
         double sin2 = Math.sin((double)((float)radians2));
         double n18 = sin2 * n15;
         double cos2 = Math.cos((double)((float)radians2));
         double n19 = cos2 * n15;
         begin.vertex(matrix4f, (float)array2[0] + (float)n18, (float)array2[1] + (float)n19, 0.0F).color(n, n2, n3, n4);
         begin.vertex(matrix4f, (float)(array2[0] + (double)((float)n18) + sin2 * n13), (float)(array2[1] + (double)((float)n19) + cos2 * n13), 0.0F).color(n, n2, n3, n4);
      }

      double[] array3 = array[0];
      double n20 = array3[2];
      begin.vertex(matrix4f, (float)array3[0], (float)array3[1] + (float)n20, 0.0F).color(n, n2, n3, n4);
      begin.vertex(matrix4f, (float)array3[0], (float)(array3[1] + (double)((float)n20) + n13), 0.0F).color(n, n2, n3, n4);
      BufferRenderer.drawWithGlobalProgram(begin.end());
   }

   public static void setScissorRegion(int n, int n2, int n3, int n4) {
      MinecraftClient instance = MinecraftClient.getInstance();
      Screen currentScreen = instance.currentScreen;
      int n5;
      if (instance.currentScreen == null) {
         n5 = 0;
      } else {
         n5 = currentScreen.height - n4;
      }

      double scaleFactor = MinecraftClient.getInstance().getWindow().getScaleFactor();
      GL11.glScissor((int)((double)n * scaleFactor), (int)((double)n5 * scaleFactor), (int)((double)(n3 - n) * scaleFactor), (int)((double)(n4 - n2) * scaleFactor));
      GL11.glEnable(3089);
   }

   public static void renderCircle(MatrixStack matrixStack, Color color, double n, double n2, double n3, int n4) {
      int clamp = MathHelper.clamp(n4, 4, 360);
      int rgb = color.getRGB();
      Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
      setup();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder begin = Tessellator.getInstance().begin(net.minecraft.client.render.VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

      for(int i = 0; i < 360; i += Math.min(360 / clamp, 360 - i)) {
         double radians = Math.toRadians((double)i);
         begin.vertex(positionMatrix, (float)(n + Math.sin(radians) * n3), (float)(n2 + Math.cos(radians) * n3), 0.0F).color((float)(rgb >> 16 & 255) / 255.0F, (float)(rgb >> 8 & 255) / 255.0F, (float)(rgb & 255) / 255.0F, (float)(rgb >> 24 & 255) / 255.0F);
      }

      BufferRenderer.drawWithGlobalProgram(begin.end());
      cleanup();
   }

   public static void renderShaderRect(MatrixStack matrixStack, Color color, Color color2, Color color3, Color color4, float n, float n2, float n3, float n4, float n5, float n6) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      BufferBuilder begin = Tessellator.getInstance().begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n - 10.0F, n2 - 10.0F, 0.0F);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n - 10.0F, n2 + n4 + 20.0F, 0.0F);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n + n3 + 20.0F, n2 + n4 + 20.0F, 0.0F);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n + n3 + 20.0F, n2 - 10.0F, 0.0F);
      BufferRenderer.drawWithGlobalProgram(begin.end());
      RenderSystem.disableBlend();
   }

   public static void renderRoundedOutline(DrawContext drawContext, Color color, double n, double n2, double n3, double n4, double n5, double n6, double n7, double n8, double n9, double n10) {
      int rgb = color.getRGB();
      Matrix4f positionMatrix = drawContext.getMatrices().peek().getPositionMatrix();
      setup();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      renderRoundedOutlineInternal(positionMatrix, (float)(rgb >> 16 & 255) / 255.0F, (float)(rgb >> 8 & 255) / 255.0F, (float)(rgb & 255) / 255.0F, (float)(rgb >> 24 & 255) / 255.0F, n, n2, n3, n4, n5, n6, n7, n8, n9, n10);
      cleanup();
   }

   public static MatrixStack matrixFrom(double n, double n2, double n3) {
      MatrixStack matrixStack = new MatrixStack();
      Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrixStack.translate(n - camera.getPos().x, n2 - camera.getPos().y, n3 - camera.getPos().z);
      return matrixStack;
   }

   public static void renderQuad(MatrixStack matrixStack, float n, float n2, float n3, float n4, int n5) {
      float n6 = (float)(n5 >> 24 & 255) / 255.0F;
      float n7 = (float)(n5 >> 16 & 255) / 255.0F;
      float n8 = (float)(n5 >> 8 & 255) / 255.0F;
      float n9 = (float)(n5 & 255) / 255.0F;
      matrixStack.push();
      matrixStack.scale(0.5F, 0.5F, 0.5F);
      matrixStack.translate((double)n, (double)n2, (double)0.0F);
      Tessellator instance = Tessellator.getInstance();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      BufferBuilder begin = instance.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      begin.vertex(0.0F, 0.0F, 0.0F).color(n7, n8, n9, n6);
      begin.vertex(0.0F, n4, 0.0F).color(n7, n8, n9, n6);
      begin.vertex(n3, n4, 0.0F).color(n7, n8, n9, n6);
      begin.vertex(n3, 0.0F, 0.0F).color(n7, n8, n9, n6);
      BufferRenderer.drawWithGlobalProgram(begin.end());
      RenderSystem.disableBlend();
      matrixStack.pop();
   }

   public static void renderRoundedQuadInternal(Matrix4f matrix4f, float n, float n2, float n3, float n4, double n5, double n6, double n7, double n8, double n9, double n10, double n11, double n12, double n13) {
      BufferBuilder begin = Tessellator.getInstance().begin(net.minecraft.client.render.VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      double[][] array = new double[][]{{n7 - n12, n8 - n12, n12}, {n7 - n10, n6 + n10, n10}, {n5 + n9, n6 + n9, n9}, {n5 + n11, n8 - n11, n11}};

      for(int i = 0; i < 4; ++i) {
         double[] array2 = array[i];
         double n14 = array2[2];

         for(double angdeg = (double)i * (double)90.0F; angdeg < (double)90.0F + (double)i * (double)90.0F; angdeg += (double)90.0F / n13) {
            double radians = Math.toRadians(angdeg);
            begin.vertex(matrix4f, (float)array2[0] + (float)(Math.sin((double)((float)radians)) * n14), (float)array2[1] + (float)(Math.cos((double)((float)radians)) * n14), 0.0F).color(n, n2, n3, n4);
         }

         double radians2 = Math.toRadians((double)90.0F + (double)i * (double)90.0F);
         begin.vertex(matrix4f, (float)array2[0] + (float)(Math.sin((double)((float)radians2)) * n14), (float)array2[1] + (float)(Math.cos((double)((float)radians2)) * n14), 0.0F).color(n, n2, n3, n4);
      }

      BufferRenderer.drawWithGlobalProgram(begin.end());
   }

   public static void renderFilledBox(MatrixStack matrixStack, float n, float n2, float n3, float n4, float n5, float n6, Color color) {
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.setShaderColor((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
      RenderSystem.setShader(GameRenderer::getPositionProgram);
      BufferBuilder begin = Tessellator.getInstance().begin(net.minecraft.client.render.VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n, n2, n3);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n, n2, n3);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n, n2, n3);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n, n2, n6);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n, n5, n3);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n, n5, n6);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n, n5, n6);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n, n2, n6);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n4, n5, n6);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n4, n2, n6);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n4, n2, n6);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n4, n2, n3);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n4, n5, n6);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n4, n5, n3);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n4, n5, n3);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n4, n2, n3);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n, n5, n3);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n, n2, n3);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n, n2, n3);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n4, n2, n3);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n, n2, n6);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n4, n2, n6);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n4, n2, n6);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n, n5, n3);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n, n5, n3);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n, n5, n6);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n4, n5, n3);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n4, n5, n6);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n4, n5, n6);
      begin.vertex(matrixStack.peek().getPositionMatrix(), n4, n5, n6);
      BufferRenderer.drawWithGlobalProgram(begin.end());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
   }

   public static void renderLine(MatrixStack matrixStack, Color color, Vec3d vec3d, Vec3d vec3d2) {
      matrixStack.push();
      Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
      if (Phantom.enableMSAA.getValue()) {
         GL11.glEnable(32925);
         GL11.glEnable(2848);
         GL11.glHint(3154, 4354);
      }

      GL11.glDepthFunc(519);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableBlend();
      genericAABBRender(net.minecraft.client.render.VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR, GameRenderer::getPositionColorProgram, positionMatrix, vec3d, vec3d2.subtract(vec3d), color, (bufferBuilder, n, n3, n5, n7, n9, n11, n13, n15, n17, n19, matrix4f) -> {
         bufferBuilder.vertex(matrix4f, n, n3, n5).color(n13, n15, n17, n19);
         bufferBuilder.vertex(matrix4f, n7, n9, n11).color(n13, n15, n17, n19);
      });
      GL11.glDepthFunc(515);
      RenderSystem.disableBlend();
      if (Phantom.enableMSAA.getValue()) {
         GL11.glDisable(2848);
         GL11.glDisable(32925);
      }

      matrixStack.pop();
   }

   public static void drawItem(DrawContext drawContext, ItemStack itemStack, int n, int n2, float n3, int n4) {
      if (!itemStack.isEmpty()) {
         float n5 = n3 / 16.0F;
         MatrixStack matrices = drawContext.getMatrices();
         matrices.push();
         matrices.translate((float)n, (float)n2, (float)n4);
         matrices.scale(n5, n5, 1.0F);
         drawContext.drawItem(itemStack, 0, 0);
         matrices.pop();
      }
   }

   private static void genericAABBRender(VertexFormat.DrawMode mode, VertexFormat format, Supplier shader, Matrix4f stack, Vec3d start, Vec3d dimensions, Color color, RenderAction action) {
      float red = (float)color.getRed() / 255.0F;
      float green = (float)color.getGreen() / 255.0F;
      float blue = (float)color.getBlue() / 255.0F;
      float alpha = (float)color.getAlpha() / 255.0F;
      Vec3d end = start.add(dimensions);
      float x1 = (float)start.x;
      float y1 = (float)start.y;
      float z1 = (float)start.z;
      float x2 = (float)end.x;
      float y2 = (float)end.y;
      float z2 = (float)end.z;
      useBuffer(mode, format, shader, (bufferBuilder) -> action.run((net.minecraft.client.render.BufferBuilder)bufferBuilder, x1, y1, z1, x2, y2, z2, red, green, blue, alpha, stack));
   }

   private static void useBuffer(VertexFormat.DrawMode drawMode, VertexFormat vertexFormat, Supplier shader, Consumer consumer) {
      BufferBuilder begin = Tessellator.getInstance().begin(drawMode, vertexFormat);
      consumer.accept(begin);
      setup();
      RenderSystem.setShader(shader);
      BufferRenderer.drawWithGlobalProgram(begin.end());
      cleanup();
   }

   interface RenderAction {
      void run(BufferBuilder var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, Matrix4f var12);
   }
}
