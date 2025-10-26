package dev.gambleclient.gui.components;

import dev.gambleclient.gui.Component;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.MathUtil;
import dev.gambleclient.utils.RenderUtils;
import dev.gambleclient.utils.TextRenderer;
import dev.gambleclient.utils.Utils;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public final class Checkbox extends Component {
   private final BooleanSetting setting;
   private float hoverAnimation = 0.0F;
   private float enabledAnimation = 0.0F;
   private final float CORNER_RADIUS = 3.0F;
   private final Color TEXT_COLOR = new Color(230, 230, 230);
   private final Color HOVER_COLOR = new Color(255, 255, 255, 20);
   private final Color BOX_BORDER = new Color(100, 100, 110);
   private final Color BOX_BG = new Color(40, 40, 45);
   private final int BOX_SIZE = 13;
   private final float HOVER_ANIMATION_SPEED = 0.005F;
   private final float TOGGLE_ANIMATION_SPEED = 0.002F;

   public Checkbox(ModuleButton moduleButton, Setting setting, int n) {
      super(moduleButton, setting, n);
      this.setting = (BooleanSetting)setting;
      float enabledAnimation;
      if (this.setting.getValue()) {
         enabledAnimation = 1.0F;
      } else {
         enabledAnimation = 0.0F;
      }

      this.enabledAnimation = enabledAnimation;
   }

   public void render(DrawContext drawContext, int n, int n2, float n3) {
      super.render(drawContext, n, n2, n3);
      this.updateAnimations(n, n2, n3);
      if (!this.parent.parent.dragging) {
         drawContext.fill(this.parentX(), this.parentY() + this.parentOffset() + this.offset, this.parentX() + this.parentWidth(), this.parentY() + this.parentOffset() + this.offset + this.parentHeight(), (new Color(this.HOVER_COLOR.getRed(), this.HOVER_COLOR.getGreen(), this.HOVER_COLOR.getBlue(), (int)((float)this.HOVER_COLOR.getAlpha() * this.hoverAnimation))).getRGB());
      }

      TextRenderer.drawString(this.setting.getName(), drawContext, this.parentX() + 27, this.parentY() + this.parentOffset() + this.offset + this.parentHeight() / 2 - 6, this.TEXT_COLOR.getRGB());
      this.renderModernCheckbox(drawContext);
   }

   private void updateAnimations(int n, int n2, float n3) {
      float n4 = n3 * 0.05F;
      float n5;
      if (this.isHovered((double)n, (double)n2) && !this.parent.parent.dragging) {
         n5 = 1.0F;
      } else {
         n5 = 0.0F;
      }

      this.hoverAnimation = (float)MathUtil.exponentialInterpolate((double)this.hoverAnimation, (double)n5, (double)0.005F, (double)n4);
      float n6;
      if (this.setting.getValue()) {
         n6 = 1.0F;
      } else {
         n6 = 0.0F;
      }

      this.enabledAnimation = (float)MathUtil.exponentialInterpolate((double)this.enabledAnimation, (double)n6, (double)0.002F, (double)n4);
      this.enabledAnimation = (float)MathUtil.clampValue((double)this.enabledAnimation, (double)0.0F, (double)1.0F);
   }

   private void renderModernCheckbox(DrawContext drawContext) {
      int n = this.parentX() + 8;
      int n2 = this.parentY() + this.parentOffset() + this.offset + this.parentHeight() / 2 - 6;
      Color mainColor = Utils.getMainColor(255, this.parent.settings.indexOf(this));
      RenderUtils.renderRoundedQuad(drawContext.getMatrices(), this.BOX_BORDER, (double)n, (double)n2, (double)(n + 13), (double)(n2 + 13), (double)3.0F, (double)3.0F, (double)3.0F, (double)3.0F, (double)50.0F);
      RenderUtils.renderRoundedQuad(drawContext.getMatrices(), this.BOX_BG, (double)(n + 1), (double)(n2 + 1), (double)(n + 13 - 1), (double)(n2 + 13 - 1), (double)2.5F, (double)2.5F, (double)2.5F, (double)2.5F, (double)50.0F);
      if (this.enabledAnimation > 0.01F) {
         Color color = new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), (int)(255.0F * this.enabledAnimation));
         float n3 = (float)(n + 2) + 9.0F * (1.0F - this.enabledAnimation) / 2.0F;
         float n4 = (float)(n2 + 2) + 9.0F * (1.0F - this.enabledAnimation) / 2.0F;
         RenderUtils.renderRoundedQuad(drawContext.getMatrices(), color, (double)n3, (double)n4, (double)(n3 + 9.0F * this.enabledAnimation), (double)(n4 + 9.0F * this.enabledAnimation), (double)1.5F, (double)1.5F, (double)1.5F, (double)1.5F, (double)50.0F);
         if (this.enabledAnimation > 0.7F) {
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), (int)(40.0F * (this.enabledAnimation - 0.7F) * 3.33F)), (double)(n - 1), (double)(n2 - 1), (double)(n + 13 + 1), (double)(n2 + 13 + 1), (double)3.5F, (double)3.5F, (double)3.5F, (double)3.5F, (double)50.0F);
         }
      }

   }

   public void keyPressed(int n, int n2, int n3) {
      if (this.mouseOver && this.parent.extended && n == 259) {
         this.setting.setValue(this.setting.getDefaultValue());
      }

      super.keyPressed(n, n2, n3);
   }

   public void mouseClicked(double n, double n2, int n3) {
      if (this.isHovered(n, n2) && n3 == 0) {
         this.setting.toggle();
      }

      super.mouseClicked(n, n2, n3);
   }

   public void onGuiClose() {
      super.onGuiClose();
      this.hoverAnimation = 0.0F;
      float enabledAnimation;
      if (this.setting.getValue()) {
         enabledAnimation = 1.0F;
      } else {
         enabledAnimation = 0.0F;
      }

      this.enabledAnimation = enabledAnimation;
   }
}
