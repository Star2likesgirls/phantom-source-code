package dev.gambleclient.gui.components;

import dev.gambleclient.gui.Component;
import dev.gambleclient.module.setting.ItemSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.ColorUtil;
import dev.gambleclient.utils.MathUtil;
import dev.gambleclient.utils.RenderUtils;
import dev.gambleclient.utils.TextRenderer;
import dev.gambleclient.utils.Utils;
import java.awt.Color;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.gui.DrawContext;

public final class ItemBox extends Component {
   private final ItemSetting setting;
   private float hoverAnimation = 0.0F;
   private Color currentColor;
   private final Color TEXT_COLOR = new Color(230, 230, 230);
   private final Color HOVER_COLOR = new Color(255, 255, 255, 20);
   private final Color ITEM_BG = new Color(30, 30, 35);
   private final Color ITEM_BORDER = new Color(60, 60, 65);
   private final float CORNER_RADIUS = 4.0F;
   private final float HOVER_ANIMATION_SPEED = 0.25F;

   public ItemBox(ModuleButton moduleButton, Setting setting, int n) {
      super(moduleButton, setting, n);
      this.setting = (ItemSetting)setting;
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
      if (!this.parent.parent.dragging) {
         drawContext.fill(this.parentX(), this.parentY() + this.parentOffset() + this.offset, this.parentX() + this.parentWidth(), this.parentY() + this.parentOffset() + this.offset + this.parentHeight(), (new Color(this.HOVER_COLOR.getRed(), this.HOVER_COLOR.getGreen(), this.HOVER_COLOR.getBlue(), (int)((float)this.HOVER_COLOR.getAlpha() * this.hoverAnimation))).getRGB());
      }

      int n4 = this.parentX() + 5;
      int n5 = this.parentY() + this.parentOffset() + this.offset + this.parentHeight() / 2;
      TextRenderer.drawString(String.valueOf(this.setting.getName()), drawContext, n4, n5 - 8, this.TEXT_COLOR.getRGB());
      int n6 = n4 + TextRenderer.getWidth(String.valueOf(this.setting.getName()) + ": ") + 5;
      int n7 = n5 - 11;
      RenderUtils.renderRoundedQuad(drawContext.getMatrices(), this.ITEM_BORDER, (double)n6, (double)n7, (double)(n6 + 22), (double)(n7 + 22), (double)4.0F, (double)4.0F, (double)4.0F, (double)4.0F, (double)50.0F);
      RenderUtils.renderRoundedQuad(drawContext.getMatrices(), this.ITEM_BG, (double)(n6 + 1), (double)(n7 + 1), (double)(n6 + 22 - 1), (double)(n7 + 22 - 1), (double)3.5F, (double)3.5F, (double)3.5F, (double)3.5F, (double)50.0F);
      Item a = this.setting.getItem();
      if (a != null && a != Items.AIR) {
         drawContext.drawItem(new ItemStack(a), n6 + 3, n7 + 3);
      } else {
         TextRenderer.drawCenteredString("?", drawContext, n6 + 11 - 1, n7 + 4, (new Color(150, 150, 150, 200)).getRGB());
      }

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

   public void mouseClicked(double n, double n2, int n3) {
      if (this.isHovered(n, n2) && n3 == 0) {
         this.mc.setScreen(new ItemFilter(this, this.setting));
      }

      super.mouseClicked(n, n2, n3);
   }

   public void onGuiClose() {
      this.currentColor = null;
      this.hoverAnimation = 0.0F;
      super.onGuiClose();
   }
}
