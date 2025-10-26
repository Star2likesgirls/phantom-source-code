package dev.gambleclient.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.gambleclient.Gamble;
import dev.gambleclient.gui.CategoryWindow;
import dev.gambleclient.gui.Component;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.modules.client.Phantom;
import dev.gambleclient.module.setting.BindSetting;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.ItemSetting;
import dev.gambleclient.module.setting.MinMaxSetting;
import dev.gambleclient.module.setting.ModeSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.module.setting.StringSetting;
import dev.gambleclient.utils.Animation;
import dev.gambleclient.utils.ColorUtil;
import dev.gambleclient.utils.MathUtil;
import dev.gambleclient.utils.RenderUtils;
import dev.gambleclient.utils.TextRenderer;
import dev.gambleclient.utils.Utils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class ModuleButton {
   public List settings = new ArrayList();
   public CategoryWindow parent;
   public Module module;
   public int offset;
   public boolean extended;
   public int settingOffset;
   public Color currentColor;
   public Color currentAlpha;
   public Animation animation = new Animation((double)0.0F);
   private final float CORNER_RADIUS = 6.0F;
   private final Color ACCENT_COLOR = new Color(145, 70, 225);
   private final Color HOVER_COLOR = new Color(255, 255, 255, 24);
   private final Color ENABLED_COLOR = new Color(190, 170, 255);
   private final Color DISABLED_COLOR = new Color(195, 195, 205);
   private final Color DESCRIPTION_BG = new Color(40, 40, 40, 200);
   private float hoverAnimation = 0.0F;
   private float enabledAnimation = 0.0F;
   private final float expandAnimation = 0.0F;

   public ModuleButton(CategoryWindow parent, Module module, int offset) {
      this.parent = parent;
      this.module = module;
      this.offset = offset;
      this.extended = false;
      this.settingOffset = parent.getHeight();

      for(Object next : module.getSettings()) {
         if (next instanceof BooleanSetting) {
            this.settings.add(new Checkbox(this, (Setting)next, this.settingOffset));
         } else if (next instanceof NumberSetting) {
            this.settings.add(new NumberBox(this, (Setting)next, this.settingOffset));
         } else if (next instanceof ModeSetting) {
            this.settings.add(new ModeBox(this, (Setting)next, this.settingOffset));
         } else if (next instanceof BindSetting) {
            this.settings.add(new Keybind(this, (Setting)next, this.settingOffset));
         } else if (next instanceof StringSetting) {
            this.settings.add(new TextBox(this, (Setting)next, this.settingOffset));
         } else if (next instanceof MinMaxSetting) {
            this.settings.add(new Slider(this, (Setting)next, this.settingOffset));
         } else if (next instanceof ItemSetting) {
            this.settings.add(new ItemBox(this, (Setting)next, this.settingOffset));
         }

         this.settingOffset += parent.getHeight();
      }

   }

   public void render(DrawContext drawContext, int n, int n2, float n3) {
      if (this.parent.getY() + this.offset <= MinecraftClient.getInstance().getWindow().getHeight()) {
         Iterator<Component> iterator = this.settings.iterator();

         while(iterator.hasNext()) {
            ((Component)iterator.next()).onUpdate();
         }

         this.updateAnimations(n, n2, n3);
         int x = this.parent.getX();
         int n4 = this.parent.getY() + this.offset;
         int width = this.parent.getWidth();
         int height = this.parent.getHeight();
         this.renderButtonBackground(drawContext, x, n4, width, height);
         this.renderIndicator(drawContext, x, n4, height);
         this.renderModuleInfo(drawContext, x, n4, width, height);
         if (this.extended) {
            this.renderSettings(drawContext, n, n2, n3);
         }

         if (this.isHovered((double)n, (double)n2) && !this.parent.dragging) {
            Gamble.INSTANCE.GUI.setTooltip(this.module.getDescription(), n + 10, n2 + 10);
         }

      }
   }

   private void updateAnimations(int n, int n2, float n3) {
      float n4 = n3 * 0.05F;
      float n5 = this.isHovered((double)n, (double)n2) && !this.parent.dragging ? 1.0F : 0.0F;
      this.hoverAnimation = (float)MathUtil.exponentialInterpolate((double)this.hoverAnimation, (double)n5, (double)0.05F, (double)n4);
      float n6 = this.module.isEnabled() ? 1.0F : 0.0F;
      this.enabledAnimation = (float)MathUtil.exponentialInterpolate((double)this.enabledAnimation, (double)n6, (double)0.005F, (double)n4);
      this.enabledAnimation = (float)MathUtil.clampValue((double)this.enabledAnimation, (double)0.0F, (double)1.0F);
   }

   private void renderButtonBackground(DrawContext drawContext, int n, int n2, int n3, int n4) {
      Color a = ColorUtil.a(new Color(25, 25, 30, 230), this.HOVER_COLOR, this.hoverAnimation);
      boolean isLast = this.parent.moduleButtons.get(this.parent.moduleButtons.size() - 1) == this;
      if (isLast && !this.extended) {
         RenderUtils.renderRoundedQuad(drawContext.getMatrices(), a, (double)n, (double)n2, (double)(n + n3), (double)(n2 + n4), (double)0.0F, (double)0.0F, (double)Phantom.cornerRoundness.getIntValue(), (double)Phantom.cornerRoundness.getIntValue(), (double)50.0F);
      } else if (isLast && this.extended) {
         RenderUtils.renderRoundedQuad(drawContext.getMatrices(), a, (double)n, (double)n2, (double)(n + n3), (double)(n2 + n4), (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F, (double)50.0F);
      } else {
         drawContext.fill(n, n2, n + n3, n2 + n4, a.getRGB());
      }

      if (this.parent.moduleButtons.indexOf(this) > 0) {
         drawContext.fill(n + 4, n2, n + n3 - 4, n2 + 1, (new Color(60, 60, 65, 100)).getRGB());
      }

   }

   private void renderIndicator(DrawContext drawContext, int n, int n2, int n3) {
      Color color = this.module.isEnabled() ? Utils.getMainColor(255, Gamble.INSTANCE.getModuleManager().a(this.module.getCategory()).indexOf(this.module)) : this.ACCENT_COLOR;
      float w = 5.0F * this.enabledAnimation;
      if (w > 0.1F) {
         RenderUtils.renderRoundedQuad(drawContext.getMatrices(), ColorUtil.a(this.DISABLED_COLOR, color, this.enabledAnimation), (double)n, (double)(n2 + 2), (double)((float)n + w), (double)(n2 + n3 - 2), (double)1.5F, (double)1.5F, (double)1.5F, (double)1.5F, (double)60.0F);
      }

   }

   private void renderModuleInfo(DrawContext drawContext, int n, int n2, int n3, int n4) {
      TextRenderer.drawString(this.module.getName(), drawContext, n + 10, n2 + n4 / 2 - 6, ColorUtil.a(this.DISABLED_COLOR, this.ENABLED_COLOR, this.enabledAnimation).getRGB());
      int pillX = n + n3 - 40;
      int pillY = n2 + n4 / 2 - 6;
      Color trackOff = new Color(60, 60, 65, 200);
      Color trackOn = new Color(145, 70, 225, 110);
      RenderUtils.renderRoundedQuad(drawContext.getMatrices(), ColorUtil.a(trackOff, trackOn, this.enabledAnimation), (double)pillX, (double)pillY, (double)((float)pillX + 24.0F), (double)((float)pillY + 12.0F), (double)6.0F, (double)6.0F, (double)6.0F, (double)6.0F, (double)50.0F);
      float knobX = (float)pillX + 6.0F + 12.0F * this.enabledAnimation;
      RenderUtils.renderCircle(drawContext.getMatrices(), ColorUtil.a(new Color(180, 180, 180), this.ENABLED_COLOR, this.enabledAnimation), (double)knobX, (double)((float)pillY + 6.0F), (double)5.0F, 12);
      if (this.module.isEnabled()) {
         RenderUtils.renderCircle(drawContext.getMatrices(), new Color(this.ENABLED_COLOR.getRed(), this.ENABLED_COLOR.getGreen(), this.ENABLED_COLOR.getBlue(), 30), (double)knobX, (double)((float)pillY + 6.0F), (double)8.0F, 16);
      }

   }

   private void renderSettings(DrawContext drawContext, int n, int n2, float n3) {
      int n4 = this.parent.getY() + this.offset + this.parent.getHeight();
      double animation = this.animation.getAnimation();
      RenderSystem.enableScissor(this.parent.getX(), Gamble.mc.getWindow().getHeight() - (n4 + (int)animation), this.parent.getWidth(), (int)animation);
      Iterator<Component> iterator = this.settings.iterator();

      while(iterator.hasNext()) {
         ((Component)iterator.next()).render(drawContext, n, n2 - n4, n3);
      }

      this.renderSliderControls(drawContext);
      RenderSystem.disableScissor();
   }

   private void renderSliderControls(DrawContext drawContext) {
      for(Object nextObj : this.settings) {
         Component next = (Component)nextObj;
         if (next instanceof NumberBox numberBox) {
            this.renderModernSliderKnob(drawContext, (double)next.parentX() + Math.max(numberBox.lerpedOffsetX, (double)2.5F), (double)(next.parentY() + numberBox.offset + next.parentOffset()) + (double)27.5F, numberBox.currentColor1);
         } else if (next instanceof Slider) {
            this.renderModernSliderKnob(drawContext, (double)next.parentX() + Math.max(((Slider)next).lerpedOffsetMinX, (double)2.5F), (double)(next.parentY() + next.offset + next.parentOffset()) + (double)27.5F, ((Slider)next).accentColor1);
            this.renderModernSliderKnob(drawContext, (double)next.parentX() + Math.max(((Slider)next).lerpedOffsetMaxX, (double)2.5F), (double)(next.parentY() + next.offset + next.parentOffset()) + (double)27.5F, ((Slider)next).accentColor1);
         }
      }

   }

   private void renderModernSliderKnob(DrawContext drawContext, double n, double n2, Color color) {
      RenderUtils.renderCircle(drawContext.getMatrices(), new Color(0, 0, 0, 100), n, n2, (double)7.0F, 18);
      RenderUtils.renderCircle(drawContext.getMatrices(), color, n, n2, (double)5.5F, 16);
      RenderUtils.renderCircle(drawContext.getMatrices(), new Color(255, 255, 255, 70), n, n2 - (double)1.0F, (double)3.0F, 12);
   }

   public void onExtend() {
      for(Iterator<ModuleButton> iterator = this.parent.moduleButtons.iterator(); iterator.hasNext(); ((ModuleButton)iterator.next()).extended = false) {
      }

   }

   public void keyPressed(int n, int n2, int n3) {
      Iterator<Component> iterator = this.settings.iterator();

      while(iterator.hasNext()) {
         ((Component)iterator.next()).keyPressed(n, n2, n3);
      }

   }

   public void mouseDragged(double n, double n2, int n3, double n4, double n5) {
      if (this.extended) {
         Iterator<Component> iterator = this.settings.iterator();

         while(iterator.hasNext()) {
            ((Component)iterator.next()).mouseDragged(n, n2, n3, n4, n5);
         }
      }

   }

   public void mouseClicked(double n, double n2, int button) {
      if (this.isHovered(n, n2)) {
         if (button == 0) {
            int n4 = this.parent.getX() + this.parent.getWidth() - 30;
            int n5 = this.parent.getY() + this.offset + this.parent.getHeight() / 2 - 3;
            if (n >= (double)n4 && n <= (double)(n4 + 12) && n2 >= (double)n5 && n2 <= (double)(n5 + 6)) {
               this.module.toggle();
            } else if (!this.module.getSettings().isEmpty() && n > (double)(this.parent.getX() + this.parent.getWidth() - 25)) {
               if (!this.extended) {
                  this.onExtend();
               }

               this.extended = !this.extended;
            } else {
               this.module.toggle();
            }
         } else if (button == 1) {
            if (this.module.getSettings().isEmpty()) {
               return;
            }

            if (!this.extended) {
               this.onExtend();
            }

            this.extended = !this.extended;
         }
      }

      if (this.extended) {
         for(Object settingObj : this.settings) {
            Component setting = (Component)settingObj;
            setting.mouseClicked(n, n2, button);
         }
      }

   }

   public void onGuiClose() {
      this.currentAlpha = null;
      this.currentColor = null;
      this.hoverAnimation = 0.0F;
      this.enabledAnimation = this.module.isEnabled() ? 1.0F : 0.0F;
      Iterator<Component> iterator = this.settings.iterator();

      while(iterator.hasNext()) {
         ((Component)iterator.next()).onGuiClose();
      }

   }

   public void mouseReleased(double n, double n2, int n3) {
      Iterator<Component> iterator = this.settings.iterator();

      while(iterator.hasNext()) {
         ((Component)iterator.next()).mouseReleased(n, n2, n3);
      }

   }

   public boolean isHovered(double n, double n2) {
      return n > (double)this.parent.getX() && n < (double)(this.parent.getX() + this.parent.getWidth()) && n2 > (double)(this.parent.getY() + this.offset) && n2 < (double)(this.parent.getY() + this.offset + this.parent.getHeight());
   }
}
