package dev.gambleclient.gui.components;

import dev.gambleclient.gui.Component;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.ColorUtil;
import dev.gambleclient.utils.MathUtil;
import dev.gambleclient.utils.RenderUtils;
import dev.gambleclient.utils.TextRenderer;
import dev.gambleclient.utils.Utils;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public final class NumberBox extends Component {
   public boolean dragging;
   public double offsetX;
   public double lerpedOffsetX = (double)0.0F;
   private float hoverAnimation = 0.0F;
   private final NumberSetting setting;
   public Color currentColor1;
   private Color currentAlpha;
   private final Color TEXT_COLOR = new Color(230, 230, 230);
   private final Color HOVER_COLOR = new Color(255, 255, 255, 20);
   private final Color TRACK_BG_COLOR = new Color(60, 60, 65);
   private final float TRACK_HEIGHT = 4.0F;
   private final float TRACK_RADIUS = 2.0F;
   private final float ANIMATION_SPEED = 0.25F;

   public NumberBox(ModuleButton moduleButton, Setting setting, int n) {
      super(moduleButton, setting, n);
      this.setting = (NumberSetting)setting;
   }

   public void onUpdate() {
      Color mainColor = Utils.getMainColor(255, this.parent.settings.indexOf(this));
      if (this.currentColor1 == null) {
         this.currentColor1 = new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 0);
      } else {
         this.currentColor1 = new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), this.currentColor1.getAlpha());
      }

      if (this.currentColor1.getAlpha() != 255) {
         this.currentColor1 = ColorUtil.a(0.05F, 255, this.currentColor1);
      }

      super.onUpdate();
   }

   public void render(DrawContext drawContext, int n, int n2, float n3) {
      super.render(drawContext, n, n2, n3);
      this.updateAnimations(n, n2, n3);
      this.offsetX = (this.setting.getValue() - this.setting.getMin()) / (this.setting.getMax() - this.setting.getMin()) * (double)this.parentWidth();
      this.lerpedOffsetX = MathUtil.approachValue((float)((double)0.5F * (double)n3), this.lerpedOffsetX, this.offsetX);
      if (!this.parent.parent.dragging) {
         drawContext.fill(this.parentX(), this.parentY() + this.parentOffset() + this.offset, this.parentX() + this.parentWidth(), this.parentY() + this.parentOffset() + this.offset + this.parentHeight(), (new Color(this.HOVER_COLOR.getRed(), this.HOVER_COLOR.getGreen(), this.HOVER_COLOR.getBlue(), (int)((float)this.HOVER_COLOR.getAlpha() * this.hoverAnimation))).getRGB());
      }

      int n4 = this.parentY() + this.offset + this.parentOffset() + 25;
      int n5 = this.parentX() + 5;
      RenderUtils.renderRoundedQuad(drawContext.getMatrices(), this.TRACK_BG_COLOR, (double)n5, (double)n4, (double)(n5 + (this.parentWidth() - 10)), (double)((float)n4 + 4.0F), (double)2.0F, (double)2.0F, (double)2.0F, (double)2.0F, (double)50.0F);
      if (this.lerpedOffsetX > (double)2.5F) {
         RenderUtils.renderRoundedQuad(drawContext.getMatrices(), this.currentColor1, (double)n5, (double)n4, (double)n5 + Math.max(this.lerpedOffsetX - (double)5.0F, (double)0.0F), (double)((float)n4 + 4.0F), (double)2.0F, (double)2.0F, (double)2.0F, (double)2.0F, (double)50.0F);
      }

      String displayValue = this.getDisplayValue();
      TextRenderer.drawString(this.setting.getName(), drawContext, this.parentX() + 5, this.parentY() + this.parentOffset() + this.offset + 9, this.TEXT_COLOR.getRGB());
      TextRenderer.drawString(displayValue, drawContext, this.parentX() + this.parentWidth() - TextRenderer.getWidth(displayValue) - 5, this.parentY() + this.parentOffset() + this.offset + 9, this.currentColor1.getRGB());
   }

   private void updateAnimations(int n, int n2, float n3) {
      float n4;
      if (this.isHovered((double)n, (double)n2) && !this.parent.parent.dragging) {
         n4 = 1.0F;
      } else {
         n4 = 0.0F;
      }

      this.hoverAnimation = (float)MathUtil.exponentialInterpolate((double)this.hoverAnimation, (double)n4, (double)0.25F, (double)(n3 * 0.05F));
   }

   private String getDisplayValue() {
      double a = this.setting.getValue();
      double c = this.setting.getFormat();
      if (c == 0.1) {
         return String.format("%.1f", a);
      } else if (c == 0.01) {
         return String.format("%.2f", a);
      } else if (c == 0.001) {
         return String.format("%.3f", a);
      } else if (c == 1.0E-4) {
         return String.format("%.4f", a);
      } else {
         return c >= (double)1.0F ? String.format("%.0f", a) : String.valueOf(a);
      }
   }

   public void onGuiClose() {
      this.currentColor1 = null;
      this.hoverAnimation = 0.0F;
      super.onGuiClose();
   }

   private void slide(double n) {
      this.setting.getValue(MathUtil.roundToNearest(MathHelper.clamp((n - (double)(this.parentX() + 5)) / (double)(this.parentWidth() - 10), (double)0.0F, (double)1.0F) * (this.setting.getMax() - this.setting.getMin()) + this.setting.getMin(), this.setting.getFormat()));
   }

   public void keyPressed(int n, int n2, int n3) {
      if (this.mouseOver && this.parent.extended && n == 259) {
         this.setting.getValue(this.setting.getDefaultValue());
      }

      super.keyPressed(n, n2, n3);
   }

   public void mouseClicked(double n, double n2, int n3) {
      if (this.isHovered(n, n2) && n3 == 0) {
         this.dragging = true;
         this.slide(n);
      }

      super.mouseClicked(n, n2, n3);
   }

   public void mouseReleased(double n, double n2, int n3) {
      if (this.dragging && n3 == 0) {
         this.dragging = false;
      }

      super.mouseReleased(n, n2, n3);
   }

   public void mouseDragged(double n, double n2, int n3, double n4, double n5) {
      if (this.dragging) {
         this.slide(n);
      }

      super.mouseDragged(n, n2, n3, n4, n5);
   }
}
