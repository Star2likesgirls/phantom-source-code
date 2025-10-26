package dev.gambleclient.gui.components;

import dev.gambleclient.gui.Component;
import dev.gambleclient.module.setting.ModeSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.ColorUtil;
import dev.gambleclient.utils.MathUtil;
import dev.gambleclient.utils.RenderUtils;
import dev.gambleclient.utils.TextRenderer;
import dev.gambleclient.utils.Utils;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

public final class ModeBox extends Component {
   private final ModeSetting setting;
   private float hoverAnimation = 0.0F;
   private float selectAnimation = 0.0F;
   private float previousSelectAnimation = 0.0F;
   private boolean wasClicked = false;
   public Color currentColor;
   private final Color TEXT_COLOR = new Color(230, 230, 230);
   private final Color HOVER_COLOR = new Color(255, 255, 255, 20);
   private final Color SELECTOR_BG = new Color(40, 40, 45);
   private final float SELECTOR_HEIGHT = 4.0F;
   private final float SELECTOR_RADIUS = 2.0F;
   private final float HOVER_ANIMATION_SPEED = 0.25F;
   private final float SELECT_ANIMATION_SPEED = 0.15F;

   public ModeBox(ModuleButton moduleButton, Setting setting, int n) {
      super(moduleButton, setting, n);
      this.setting = (ModeSetting)setting;
   }

   public void onUpdate() {
      Color mainColor = Utils.getMainColor(255, this.parent.settings.indexOf(this));
      if (this.currentColor == null) {
         this.currentColor = new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 0);
      } else {
         this.currentColor = new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), this.currentColor.getAlpha());
      }

      if (this.currentColor.getAlpha() != 255) {
         this.currentColor = ColorUtil.a(0.05F, 255, this.currentColor);
      }

      super.onUpdate();
   }

   public void render(DrawContext drawContext, int n, int n2, float n3) {
      super.render(drawContext, n, n2, n3);
      this.updateAnimations(n, n2, n3);
      int index = this.setting.getPossibleValues().indexOf(this.setting.getValue());
      int size = this.setting.getPossibleValues().size();
      this.parentWidth();
      this.parentX();
      if (!this.parent.parent.dragging) {
         drawContext.fill(this.parentX(), this.parentY() + this.parentOffset() + this.offset, this.parentX() + this.parentWidth(), this.parentY() + this.parentOffset() + this.offset + this.parentHeight(), (new Color(this.HOVER_COLOR.getRed(), this.HOVER_COLOR.getGreen(), this.HOVER_COLOR.getBlue(), (int)((float)this.HOVER_COLOR.getAlpha() * this.hoverAnimation))).getRGB());
      }

      TextRenderer.drawString(String.valueOf(this.setting.getName()), drawContext, this.parentX() + 5, this.parentY() + this.parentOffset() + this.offset + 9, this.TEXT_COLOR.getRGB());
      TextRenderer.drawString(this.setting.getValue().name(), drawContext, this.parentX() + TextRenderer.getWidth(String.valueOf(this.setting.getName()) + ": ") + 8, this.parentY() + this.parentOffset() + this.offset + 9, this.currentColor.getRGB());
      int n4 = this.parentY() + this.offset + this.parentOffset() + 25;
      int n5 = this.parentX() + 5;
      int n6 = this.parentWidth() - 10;
      RenderUtils.renderRoundedQuad(drawContext.getMatrices(), this.SELECTOR_BG, (double)n5, (double)n4, (double)(n5 + n6), (double)((float)n4 + 4.0F), (double)2.0F, (double)2.0F, (double)2.0F, (double)2.0F, (double)50.0F);
      int n7 = index - 1;
      float n8 = (float)n7;
      if ((float)n7 < 0.0F) {
         n8 = (float)(size - 1);
      }

      int n9 = index + 1;
      float n10 = (float)n9;
      if ((float)n9 >= (float)size) {
         n10 = 0.0F;
      }

      int n11 = n6 / size;
      float n12;
      if (this.previousSelectAnimation > 0.01F) {
         n12 = (float)MathUtil.linearInterpolate((double)((float)n5 + n8 * (float)n11), (double)((float)n5 + (float)index * (float)n11), (double)(1.0F - this.previousSelectAnimation));
      } else if (this.selectAnimation > 0.01F) {
         n12 = (float)MathUtil.linearInterpolate((double)((float)n5 + (float)index * (float)n11), (double)((float)n5 + n10 * (float)n11), (double)this.selectAnimation);
      } else {
         n12 = (float)n5 + (float)index * (float)n11;
      }

      RenderUtils.renderRoundedQuad(drawContext.getMatrices(), this.currentColor, (double)n12, (double)n4, (double)(n12 + (float)n11), (double)((float)n4 + 4.0F), (double)2.0F, (double)2.0F, (double)2.0F, (double)2.0F, (double)50.0F);
      int n13 = this.parentY() + this.parentOffset() + this.offset + 9;
      int parentX = this.parentX();
      int parentX2 = this.parentX();
      int parentWidth = this.parentWidth();
      TextRenderer.drawString("◄", drawContext, parentX + this.parentWidth() - 25, n13, this.TEXT_COLOR.getRGB());
      TextRenderer.drawString("►", drawContext, parentX2 + parentWidth - 12, n13, this.TEXT_COLOR.getRGB());
      if (this.wasClicked) {
         this.wasClicked = false;
         this.previousSelectAnimation = 0.0F;
         this.selectAnimation = 0.01F;
      }

   }

   private void updateAnimations(int n, int n2, float n3) {
      float n4 = n3 * 0.05F;
      float n5;
      if (this.isHovered((double)n, (double)n2) && !this.parent.parent.dragging) {
         n5 = 1.0F;
      } else {
         n5 = 0.0F;
      }

      this.hoverAnimation = (float)MathUtil.exponentialInterpolate((double)this.hoverAnimation, (double)n5, (double)0.25F, (double)n4);
      if (this.selectAnimation > 0.01F) {
         this.selectAnimation = (float)MathUtil.exponentialInterpolate((double)this.selectAnimation, (double)0.0F, (double)0.15F, (double)n4);
         if (this.selectAnimation < 0.01F) {
            this.previousSelectAnimation = 0.99F;
         }
      }

      if (this.previousSelectAnimation > 0.01F) {
         this.previousSelectAnimation = (float)MathUtil.exponentialInterpolate((double)this.previousSelectAnimation, (double)0.0F, (double)0.15F, (double)n4);
      }

   }

   public void keyPressed(int n, int n2, int n3) {
      if (this.mouseOver && this.parent.extended) {
         if (n == 259) {
            this.setting.setModeIndex(this.setting.getOriginalValue());
         } else if (n == 262) {
            this.cycleModeForward();
         } else if (n == 263) {
            this.cycleModeBackward();
         }
      }

      super.keyPressed(n, n2, n3);
   }

   private void cycleModeForward() {
      this.setting.cycleUp();
      this.wasClicked = true;
   }

   private void cycleModeBackward() {
      this.setting.cycleDown();
      this.wasClicked = true;
   }

   public void mouseClicked(double n, double n2, int n3) {
      if (this.isHovered(n, n2)) {
         if (n3 == 0) {
            this.cycleModeForward();
         } else if (n3 == 1) {
            this.cycleModeBackward();
         } else if (n3 == 2) {
            this.setting.setModeIndex(this.setting.getOriginalValue());
         }
      }

      super.mouseClicked(n, n2, n3);
   }

   public void onGuiClose() {
      this.currentColor = null;
      this.hoverAnimation = 0.0F;
      this.selectAnimation = 0.0F;
      this.previousSelectAnimation = 0.0F;
      super.onGuiClose();
   }
}
