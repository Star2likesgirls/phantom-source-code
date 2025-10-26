package dev.gambleclient.gui;

import dev.gambleclient.Gamble;
import dev.gambleclient.gui.components.ModuleButton;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.modules.client.Phantom;
import dev.gambleclient.utils.Animation;
import dev.gambleclient.utils.ColorUtil;
import dev.gambleclient.utils.MathUtil;
import dev.gambleclient.utils.RenderUtils;
import dev.gambleclient.utils.TextRenderer;
import dev.gambleclient.utils.Utils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public final class CategoryWindow {
   public List moduleButtons = new ArrayList();
   public int x;
   public int y;
   private final int width;
   private final int height;
   public Color currentColor;
   private final Category category;
   public boolean dragging;
   public boolean extended;
   private int dragX;
   private int dragY;
   private int prevX;
   private int prevY;
   public ClickGUI parent;
   private float hoverAnimation = 0.0F;

   public CategoryWindow(int x, int y, int width, int height, Category category, ClickGUI parent) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.dragging = false;
      this.extended = true;
      this.height = height;
      this.category = category;
      this.parent = parent;
      this.prevX = x;
      this.prevY = y;
      List<Module> modules = new ArrayList(Gamble.INSTANCE.getModuleManager().a(category));
      int offset = height;

      for(Module module : modules) {
         this.moduleButtons.add(new ModuleButton(this, module, offset));
         offset += height;
      }

   }

   public void render(DrawContext context, int n, int n2, float n3) {
      Color base = new Color(25, 25, 30, Phantom.windowAlpha.getIntValue());
      if (this.currentColor == null) {
         this.currentColor = new Color(25, 25, 30, 0);
      } else {
         this.currentColor = ColorUtil.a(0.05F, base, this.currentColor);
      }

      float target = this.isHovered((double)n, (double)n2) && !this.dragging ? 1.0F : 0.0F;
      this.hoverAnimation = (float)MathUtil.approachValue(n3 * 0.1F, (double)this.hoverAnimation, (double)target);
      Color bg = ColorUtil.a(new Color(25, 25, 30, this.currentColor.getAlpha()), new Color(255, 255, 255, 20), this.hoverAnimation);
      double r = (double)Phantom.cornerRoundness.getIntValue();
      double br = this.extended ? (double)0.0F : r;
      double bl = this.extended ? (double)0.0F : r;
      RenderUtils.renderRoundedQuad(context.getMatrices(), bg, (double)this.prevX, (double)this.prevY, (double)(this.prevX + this.width), (double)(this.prevY + this.height), r, r, bl, br, (double)50.0F);
      Color mainColor = Utils.getMainColor(255, this.category.ordinal());
      CharSequence label = this.category.name;
      int tx = this.prevX + (this.width - TextRenderer.getWidth(this.category.name)) / 2;
      int ty = this.prevY + 8;
      TextRenderer.drawString(label, context, tx + 1, ty + 1, (new Color(0, 0, 0, 100)).getRGB());
      TextRenderer.drawString(label, context, tx, ty, mainColor.brighter().getRGB());
      context.fill(this.prevX + 6, this.prevY + 2, this.prevX + this.width - 6, this.prevY + 3, (new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 100)).getRGB());
      this.updateButtons(n3);
      if (this.extended) {
         this.renderModuleButtons(context, n, n2, n3);
      }

   }

   private void renderModuleButtons(DrawContext context, int n, int n2, float n3) {
      for(Object moduleObj : this.moduleButtons) {
         ModuleButton module = (ModuleButton)moduleObj;
         module.render(context, n, n2, n3);
      }

   }

   public void keyPressed(int n, int n2, int n3) {
      for(Object moduleButtonObj : this.moduleButtons) {
         ModuleButton moduleButton = (ModuleButton)moduleButtonObj;
         moduleButton.keyPressed(n, n2, n3);
      }

   }

   public void onGuiClose() {
      this.currentColor = null;

      for(Object moduleButtonObj : this.moduleButtons) {
         ModuleButton moduleButton = (ModuleButton)moduleButtonObj;
         moduleButton.onGuiClose();
      }

      this.dragging = false;
   }

   public void mouseClicked(double x, double y, int button) {
      if (this.isHovered(x, y)) {
         switch (button) {
            case 0:
               if (!this.parent.isDraggingAlready()) {
                  this.dragging = true;
                  this.dragX = (int)(x - (double)this.x);
                  this.dragY = (int)(y - (double)this.y);
               }
            case 1:
         }
      }

      if (this.extended) {
         for(Object moduleButtonObj : this.moduleButtons) {
            ModuleButton moduleButton = (ModuleButton)moduleButtonObj;
            moduleButton.mouseClicked(x, y, button);
         }
      }

   }

   public void mouseDragged(double n, double n2, int n3, double n4, double n5) {
      if (this.extended) {
         for(Object moduleButtonObj : this.moduleButtons) {
            ModuleButton moduleButton = (ModuleButton)moduleButtonObj;
            moduleButton.mouseDragged(n, n2, n3, n4, n5);
         }
      }

   }

   public void updateButtons(float n) {
      int height = this.height;

      for(Object nextObj : this.moduleButtons) {
         ModuleButton next = (ModuleButton)nextObj;
         Animation animation = next.animation;
         double target = next.extended ? (double)(this.height * (next.settings.size() + 1)) : (double)this.height;
         animation.animate((double)0.5F * (double)n, target);
         double anim = next.animation.getAnimation();
         next.offset = height;
         height += (int)anim;
      }

   }

   public void mouseReleased(double n, double n2, int n3) {
      if (n3 == 0 && this.dragging) {
         this.dragging = false;
      }

      for(Object moduleButtonObj : this.moduleButtons) {
         ModuleButton moduleButton = (ModuleButton)moduleButtonObj;
         moduleButton.mouseReleased(n, n2, n3);
      }

   }

   public void mouseScrolled(double n, double n2, double n3, double n4) {
      this.prevX = this.x;
      this.prevY = this.y;
      this.prevY += (int)(n4 * (double)20.0F);
      this.setY((int)((double)this.y + n4 * (double)20.0F));
   }

   public int getX() {
      return this.prevX;
   }

   public int getY() {
      return this.prevY;
   }

   public void setY(int y) {
      this.y = y;
   }

   public void setX(int x) {
      this.x = x;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public boolean isHovered(double n, double n2) {
      return n > (double)this.x && n < (double)(this.x + this.width) && n2 > (double)this.y && n2 < (double)(this.y + this.height);
   }

   public boolean isPrevHovered(double n, double n2) {
      return n > (double)this.prevX && n < (double)(this.prevX + this.width) && n2 > (double)this.prevY && n2 < (double)(this.prevY + this.height);
   }

   public void updatePosition(double n, double n2, float n3) {
      this.prevX = this.x;
      this.prevY = this.y;
      if (this.dragging) {
         double targetX = this.isHovered(n, n2) ? (double)this.x : (double)this.prevX;
         this.x = (int)MathUtil.approachValue(0.3F * n3, targetX, n - (double)this.dragX);
         double targetY = this.isHovered(n, n2) ? (double)this.y : (double)this.prevY;
         this.y = (int)MathUtil.approachValue(0.3F * n3, targetY, n2 - (double)this.dragY);
      }

   }

   private static byte[] vbfixpesqoeicux() {
      return new byte[]{9, 39, 37, 116, 77, 48, 79, 112, 77, 114, 96, 59, 15, 85, 93, 58, 76, 29, 27, 107, 82, 38, 14, 37, 19, 125, 30, 87, 69, 24, 57, 76, 124, 68, 96, 106, 110, 78, 64, 115, 6, 124, 79, 50, 8, 83, 37, 14, 61, 61, 66, 65, 123, 108, 11, 3, 12, 84, 21, 22, 91, 18, 2, 50, 88, 98, 4, 17, 114, 101, 101, 44, 107, 69, 101, 51, 89, 85, 28, 12, 87, 28, 27, 72, 70, 83, 76, 2, 102, 100, 57, 5, 111, 20, 25, 117, 28, 49, 84, 102, 71, 60, 24, 56, 42, 12, 59, 20, 59, 3, 23, 84, 77, 49, 30, 103, 45, 45, 35, 112, 56, 122, 25, 87};
   }
}
