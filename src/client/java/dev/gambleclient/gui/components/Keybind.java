package dev.gambleclient.gui.components;

import dev.gambleclient.gui.Component;
import dev.gambleclient.module.setting.BindSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.ColorUtil;
import dev.gambleclient.utils.KeyUtils;
import dev.gambleclient.utils.MathUtil;
import dev.gambleclient.utils.RenderUtils;
import dev.gambleclient.utils.TextRenderer;
import dev.gambleclient.utils.Utils;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public final class Keybind extends Component {
   private final BindSetting keybind;
   private Color accentColor;
   private Color currentAlpha;
   private float hoverAnimation = 0.0F;
   private float listenAnimation = 0.0F;
   private static final Color TEXT_COLOR = new Color(230, 230, 230);
   private static final Color LISTENING_TEXT_COLOR = new Color(255, 255, 255);
   private static final Color HOVER_COLOR = new Color(255, 255, 255, 20);
   private static final Color BUTTON_BG_COLOR = new Color(60, 60, 65);
   private static final Color BUTTON_ACTIVE_BG_COLOR = new Color(80, 80, 85);
   private static final float BUTTON_RADIUS = 4.0F;
   private static final float ANIMATION_SPEED = 0.25F;
   private static final float LISTEN_ANIMATION_SPEED = 0.35F;
   private static final int BUTTON_MIN_WIDTH = 80;
   private static final int BUTTON_PADDING = 16;

   public Keybind(ModuleButton moduleButton, Setting setting, int n) {
      super(moduleButton, setting, n);
      this.keybind = (BindSetting)setting;
   }

   public void render(DrawContext drawContext, int n, int n2, float n3) {
      super.render(drawContext, n, n2, n3);
      MatrixStack matrices = drawContext.getMatrices();
      this.updateAnimations(n, n2, n3);
      if (!this.parent.parent.dragging) {
         drawContext.fill(this.parentX(), this.parentY() + this.parentOffset() + this.offset, this.parentX() + this.parentWidth(), this.parentY() + this.parentOffset() + this.offset + this.parentHeight(), (new Color(HOVER_COLOR.getRed(), HOVER_COLOR.getGreen(), HOVER_COLOR.getBlue(), (int)((float)HOVER_COLOR.getAlpha() * this.hoverAnimation))).getRGB());
      }

      TextRenderer.drawString(this.setting.getName(), drawContext, this.parentX() + 5, this.parentY() + this.parentOffset() + this.offset + 9, TEXT_COLOR.getRGB());
      String string;
      if (this.keybind.isListening()) {
         string = "Listening...";
      } else {
         string = KeyUtils.getKey(this.keybind.getValue()).toString();
      }

      int a = TextRenderer.getWidth(string);
      int max = Math.max(80, a + 16);
      int n4 = this.parentX() + this.parentWidth() - max - 5;
      int n5 = this.parentY() + this.parentOffset() + this.offset + (this.parentHeight() - 20) / 2;
      RenderUtils.renderRoundedQuad(matrices, ColorUtil.a(BUTTON_BG_COLOR, BUTTON_ACTIVE_BG_COLOR, this.listenAnimation), (double)n4, (double)n5, (double)(n4 + max), (double)(n5 + 20), (double)4.0F, (double)4.0F, (double)4.0F, (double)4.0F, (double)50.0F);
      float a2 = this.listenAnimation * 0.7F;
      float b;
      if (this.isButtonHovered((double)n, (double)n2, n4, n5, max, 20)) {
         b = 0.2F;
      } else {
         b = 0.0F;
      }

      float max2 = Math.max(a2, b);
      if (max2 > 0.0F) {
         RenderUtils.renderRoundedQuad(matrices, new Color(this.accentColor.getRed(), this.accentColor.getGreen(), this.accentColor.getBlue(), (int)((float)this.accentColor.getAlpha() * max2)), (double)n4, (double)n5, (double)(n4 + max), (double)(n5 + 20), (double)4.0F, (double)4.0F, (double)4.0F, (double)4.0F, (double)50.0F);
      }

      TextRenderer.drawString(string, drawContext, n4 + (max - a) / 2, n5 + 6 - 3, ColorUtil.a(TEXT_COLOR, LISTENING_TEXT_COLOR, this.listenAnimation).getRGB());
      if (this.keybind.isListening()) {
         RenderUtils.renderRoundedQuad(matrices, new Color(this.accentColor.getRed(), this.accentColor.getGreen(), this.accentColor.getBlue(), (int)((float)this.accentColor.getAlpha() * (float)Math.abs(Math.sin((double)System.currentTimeMillis() / (double)500.0F)) * 0.3F)), (double)n4, (double)n5, (double)(n4 + max), (double)(n5 + 20), (double)4.0F, (double)4.0F, (double)4.0F, (double)4.0F, (double)50.0F);
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
      float n6;
      if (this.keybind.isListening()) {
         n6 = 1.0F;
      } else {
         n6 = 0.0F;
      }

      this.listenAnimation = (float)MathUtil.exponentialInterpolate((double)this.listenAnimation, (double)n6, (double)0.35F, (double)n4);
   }

   private boolean isButtonHovered(double n, double n2, int n3, int n4, int n5, int n6) {
      return n >= (double)n3 && n <= (double)(n3 + n5) && n2 >= (double)n4 && n2 <= (double)(n4 + n6);
   }

   public void mouseClicked(double n, double n2, int n3) {
      String string;
      if (this.keybind.isListening()) {
         string = "Listening...";
      } else {
         string = KeyUtils.getKey(this.keybind.getValue()).toString();
      }

      int max = Math.max(80, TextRenderer.getWidth(string) + 16);
      if (this.isButtonHovered(n, n2, this.parentX() + this.parentWidth() - max - 5, this.parentY() + this.parentOffset() + this.offset + (this.parentHeight() - 20) / 2, max, 20)) {
         if (!this.keybind.isListening()) {
            if (n3 == 0) {
               this.keybind.toggleListening();
               this.keybind.setListening(true);
            }
         } else {
            if (this.keybind.isModuleKey()) {
               this.parent.module.setKeybind(n3);
            }

            this.keybind.setValue(n3);
            this.keybind.setListening(false);
         }
      }

      super.mouseClicked(n, n2, n3);
   }

   public void keyPressed(int n, int n2, int n3) {
      if (this.keybind.isListening()) {
         if (n == 256) {
            this.keybind.setListening(false);
         } else if (n == 259) {
            if (this.keybind.isModuleKey()) {
               this.parent.module.setKeybind(-1);
            }

            this.keybind.setValue(-1);
            this.keybind.setListening(false);
         } else {
            if (this.keybind.isModuleKey()) {
               this.parent.module.setKeybind(n);
            }

            this.keybind.setValue(n);
            this.keybind.setListening(false);
         }
      }

      super.keyPressed(n, n2, n3);
   }

   public void onUpdate() {
      Color mainColor = Utils.getMainColor(255, this.parent.settings.indexOf(this));
      if (this.accentColor == null) {
         this.accentColor = new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 0);
      } else {
         this.accentColor = new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), this.accentColor.getAlpha());
      }

      if (this.accentColor.getAlpha() != 255) {
         this.accentColor = ColorUtil.a(0.05F, 255, this.accentColor);
      }

      super.onUpdate();
   }

   public void onGuiClose() {
      this.accentColor = null;
      this.hoverAnimation = 0.0F;
      this.listenAnimation = 0.0F;
      super.onGuiClose();
   }
}
