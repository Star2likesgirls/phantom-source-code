package dev.gambleclient.module.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.Render3DEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.modules.client.Phantom;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.ColorUtil;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.RenderUtils;
import dev.gambleclient.utils.Utils;
import java.awt.Color;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexFormat;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class PlayerESP extends Module {
   private final NumberSetting alpha = new NumberSetting(EncryptedString.of("Alpha"), (double)0.0F, (double)255.0F, (double)100.0F, (double)1.0F);
   private final NumberSetting lineWidth = new NumberSetting(EncryptedString.of("Line width"), (double)1.0F, (double)10.0F, (double)1.0F, (double)1.0F);
   private final BooleanSetting tracers = (new BooleanSetting(EncryptedString.of("Tracers"), false)).setDescription(EncryptedString.of("Draws a line from your player to the other"));

   public PlayerESP() {
      super(EncryptedString.of("Player ESP"), EncryptedString.of("Renders players through walls"), -1, Category.RENDER);
      this.addSettings(new Setting[]{this.alpha, this.lineWidth, this.tracers});
   }

   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onRender3D(Render3DEvent render3DEvent) {
      for(Object next : this.mc.world.getPlayers()) {
         if (next != this.mc.player) {
            Camera camera = RenderUtils.getCamera();
            if (camera != null) {
               MatrixStack a = render3DEvent.matrixStack;
               render3DEvent.matrixStack.push();
               Vec3d pos = RenderUtils.getCameraPos();
               a.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
               a.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
               a.translate(-pos.x, -pos.y, -pos.z);
            }

            double lerp = MathHelper.lerp((double)RenderTickCounter.ONE.getTickDelta(true), ((PlayerEntity)next).prevX, ((PlayerEntity)next).getX());
            double lerp2 = MathHelper.lerp((double)RenderTickCounter.ONE.getTickDelta(true), ((PlayerEntity)next).prevY, ((PlayerEntity)next).getY());
            double lerp3 = MathHelper.lerp((double)RenderTickCounter.ONE.getTickDelta(true), ((PlayerEntity)next).prevZ, ((PlayerEntity)next).getZ());
            RenderUtils.renderFilledBox(render3DEvent.matrixStack, (float)lerp - ((PlayerEntity)next).getWidth() / 2.0F, (float)lerp2, (float)lerp3 - ((PlayerEntity)next).getWidth() / 2.0F, (float)lerp + ((PlayerEntity)next).getWidth() / 2.0F, (float)lerp2 + ((PlayerEntity)next).getHeight(), (float)lerp3 + ((PlayerEntity)next).getWidth() / 2.0F, Utils.getMainColor(this.alpha.getIntValue(), 1).brighter());
            if (this.tracers.getValue()) {
               RenderUtils.renderLine(render3DEvent.matrixStack, Utils.getMainColor(255, 1), this.mc.crosshairTarget.getPos(), ((PlayerEntity)next).getLerpedPos(RenderTickCounter.ONE.getTickDelta(true)));
            }

            render3DEvent.matrixStack.pop();
         }
      }

   }

   private void renderPlayerOutline(PlayerEntity playerEntity, Color color, MatrixStack matrixStack) {
      float n = (float)color.brighter().getRed() / 255.0F;
      float n2 = (float)color.brighter().getGreen() / 255.0F;
      float n3 = (float)color.brighter().getBlue() / 255.0F;
      float n4 = (float)color.brighter().getAlpha() / 255.0F;
      Camera camera = this.mc.gameRenderer.getCamera();
      Vec3d subtract = playerEntity.getLerpedPos(RenderTickCounter.ONE.getTickDelta(true)).subtract(camera.getPos());
      float n5 = (float)subtract.x;
      float n6 = (float)subtract.y;
      float n7 = (float)subtract.z;
      double radians = Math.toRadians((double)(camera.getYaw() + 90.0F));
      double n8 = Math.sin(radians) * ((double)playerEntity.getWidth() / 1.7);
      double n9 = Math.cos(radians) * ((double)playerEntity.getWidth() / 1.7);
      matrixStack.push();
      Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      if (Phantom.enableMSAA.getValue()) {
         GL11.glEnable(32925);
         GL11.glEnable(2848);
         GL11.glHint(3154, 4354);
      }

      GL11.glDepthFunc(519);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableBlend();
      GL11.glLineWidth((float)this.lineWidth.getIntValue());
      BufferBuilder begin = Tessellator.getInstance().begin(net.minecraft.client.render.VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      begin.vertex(positionMatrix, n5 + (float)n8, n6, n7 + (float)n9).color(n, n2, n3, n4);
      begin.vertex(positionMatrix, n5 - (float)n8, n6, n7 - (float)n9).color(n, n2, n3, n4);
      begin.vertex(positionMatrix, n5 - (float)n8, n6, n7 - (float)n9).color(n, n2, n3, n4);
      begin.vertex(positionMatrix, n5 - (float)n8, n6 + playerEntity.getHeight(), n7 - (float)n9).color(n, n2, n3, n4);
      begin.vertex(positionMatrix, n5 - (float)n8, n6 + playerEntity.getHeight(), n7 - (float)n9).color(n, n2, n3, n4);
      begin.vertex(positionMatrix, n5 + (float)n8, n6 + playerEntity.getHeight(), n7 + (float)n9).color(n, n2, n3, n4);
      begin.vertex(positionMatrix, n5 + (float)n8, n6 + playerEntity.getHeight(), n7 + (float)n9).color(n, n2, n3, n4);
      begin.vertex(positionMatrix, n5 + (float)n8, n6, n7 + (float)n9).color(n, n2, n3, n4);
      begin.vertex(positionMatrix, n5 + (float)n8, n6, n7 + (float)n9).color(n, n2, n3, n4);
      BufferRenderer.drawWithGlobalProgram(begin.end());
      GL11.glDepthFunc(515);
      GL11.glLineWidth(1.0F);
      RenderSystem.disableBlend();
      if (Phantom.enableMSAA.getValue()) {
         GL11.glDisable(2848);
         GL11.glDisable(32925);
      }

      matrixStack.pop();
   }

   private Color getColorWithAlpha(int a) {
      int f = Phantom.redColor.getIntValue();
      int f2 = Phantom.greenColor.getIntValue();
      int f3 = Phantom.blueColor.getIntValue();
      return Phantom.enableRainbowEffect.getValue() ? ColorUtil.a(1, a) : new Color(f, f2, f3, a);
   }
}
