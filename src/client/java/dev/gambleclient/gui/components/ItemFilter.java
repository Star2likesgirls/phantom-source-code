package dev.gambleclient.gui.components;

import dev.gambleclient.Gamble;
import dev.gambleclient.module.modules.client.Phantom;
import dev.gambleclient.module.setting.ItemSetting;
import dev.gambleclient.utils.RenderUtils;
import dev.gambleclient.utils.TextRenderer;
import dev.gambleclient.utils.Utils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.Registries;

class ItemFilter extends Screen {
   private final ItemSetting setting;
   private String searchQuery;
   private final List allItems;
   private List filteredItems;
   private int scrollOffset;
   private final int ITEMS_PER_ROW = 11;
   private final int MAX_ROWS_VISIBLE = 6;
   private int selectedIndex;
   private final int ITEM_SIZE = 40;
   private final int ITEM_SPACING = 8;
   final ItemBox this$0;

   public ItemFilter(ItemBox this$0, ItemSetting setting) {
      super(Text.empty());
      this.this$0 = this$0;
      this.searchQuery = "";
      this.scrollOffset = 0;
      this.selectedIndex = -1;
      this.setting = setting;
      this.allItems = new ArrayList();
      Registries.ITEM.forEach((item) -> {
         if (item != Items.AIR) {
            this.allItems.add(item);
         }

      });
      this.filteredItems = new ArrayList(this.allItems);
      if (setting.getItem() != null && setting.getItem() != Items.AIR) {
         for(int i = 0; i < this.filteredItems.size(); ++i) {
            if (this.filteredItems.get(i) == setting.getItem()) {
               this.selectedIndex = i;
               break;
            }
         }
      }

   }

   public void render(DrawContext drawContext, int n, int n2, float n3) {
      RenderUtils.unscaledProjection();
      int n4 = n * (int)MinecraftClient.getInstance().getWindow().getScaleFactor();
      int n5 = n2 * (int)MinecraftClient.getInstance().getWindow().getScaleFactor();
      super.render(drawContext, n4, n5, n3);
      int width = this.this$0.mc.getWindow().getWidth();
      int height = this.this$0.mc.getWindow().getHeight();
      int a;
      if (Phantom.renderBackground.getValue()) {
         a = 180;
      } else {
         a = 0;
      }

      drawContext.fill(0, 0, width, height, (new Color(0, 0, 0, a)).getRGB());
      int n6 = (this.this$0.mc.getWindow().getWidth() - 580) / 2;
      int n7 = (this.this$0.mc.getWindow().getHeight() - 450) / 2;
      RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(30, 30, 35, 240), (double)n6, (double)n7, (double)(n6 + 580), (double)(n7 + 450), (double)8.0F, (double)8.0F, (double)8.0F, (double)8.0F, (double)20.0F);
      RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(40, 40, 45, 255), (double)n6, (double)n7, (double)(n6 + 580), (double)(n7 + 30), (double)8.0F, (double)8.0F, (double)0.0F, (double)0.0F, (double)20.0F);
      drawContext.fill(n6, n7 + 30, n6 + 580, n7 + 31, Utils.getMainColor(255, 1).getRGB());
      TextRenderer.drawCenteredString("Select Item: " + String.valueOf(this.setting.getName()), drawContext, n6 + 290, n7 + 8, (new Color(245, 245, 245, 255)).getRGB());
      int n8 = n6 + 20;
      int n9 = n7 + 50;
      RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(20, 20, 25, 255), (double)n8, (double)n9, (double)(n8 + 540), (double)(n9 + 30), (double)5.0F, (double)5.0F, (double)5.0F, (double)5.0F, (double)20.0F);
      RenderUtils.renderRoundedOutline(drawContext, new Color(60, 60, 65, 255), (double)n8, (double)n9, (double)(n8 + 540), (double)(n9 + 30), (double)5.0F, (double)5.0F, (double)5.0F, (double)5.0F, (double)1.0F, (double)20.0F);
      String searchQuery = this.searchQuery;
      String s;
      if (System.currentTimeMillis() % 1000L > 500L) {
         s = "|";
      } else {
         s = "";
      }

      TextRenderer.drawString("Search: " + searchQuery + s, drawContext, n8 + 10, n9 + 9, (new Color(200, 200, 200, 255)).getRGB());
      int n10 = n6 + 20;
      int n11 = n9 + 30 + 15;
      int n12 = 450 - (n11 - n7) - 60;
      RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(25, 25, 30, 255), (double)n10, (double)n11, (double)(n10 + 540), (double)(n11 + n12), (double)5.0F, (double)5.0F, (double)5.0F, (double)5.0F, (double)20.0F);
      double ceil = Math.ceil((double)this.filteredItems.size() / (double)11.0F);
      int max = Math.max(0, (int)ceil - 6);
      this.scrollOffset = Math.min(this.scrollOffset, max);
      if ((int)ceil > 6) {
         int n13 = n10 + 540 - 6 - 5;
         int n14 = n11 + 5;
         int n15 = n12 - 10;
         RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(20, 20, 25, 150), (double)n13, (double)n14, (double)(n13 + 6), (double)(n14 + n15), (double)3.0F, (double)3.0F, (double)3.0F, (double)3.0F, (double)20.0F);
         float n16 = (float)this.scrollOffset / (float)max;
         float max2 = Math.max(40.0F, (float)n15 * (6.0F / (float)((int)ceil)));
         int n17 = n14 + (int)(((float)n15 - max2) * n16);
         RenderUtils.renderRoundedQuad(drawContext.getMatrices(), Utils.getMainColor(255, 1), (double)n13, (double)n17, (double)(n13 + 6), (double)((float)n17 + max2), (double)3.0F, (double)3.0F, (double)3.0F, (double)3.0F, (double)20.0F);
      }

      int i;
      for(int n18 = i = this.scrollOffset * 11; i < Math.min(n18 + Math.min(this.filteredItems.size(), 66), this.filteredItems.size()); ++i) {
         int n19 = n10 + 5 + (i - n18) % 11 * 48;
         int n20 = n11 + 5 + (i - n18) / 11 * 48;
         Color mainColor;
         if (i == this.selectedIndex) {
            mainColor = Utils.getMainColor(100, 1);
         } else {
            mainColor = new Color(35, 35, 40, 255);
         }

         RenderUtils.renderRoundedQuad(drawContext.getMatrices(), mainColor, (double)n19, (double)n20, (double)(n19 + 40), (double)(n20 + 40), (double)4.0F, (double)4.0F, (double)4.0F, (double)4.0F, (double)20.0F);
         RenderUtils.drawItem(drawContext, new ItemStack((ItemConvertible)this.filteredItems.get(i)), n19, n20, 40.0F, 0);
         if (n4 >= n19 && n4 <= n19 + 40 && n5 >= n20 && n5 <= n20 + 40) {
            RenderUtils.renderRoundedOutline(drawContext, Utils.getMainColor(200, 1), (double)n19, (double)n20, (double)(n19 + 40), (double)(n20 + 40), (double)4.0F, (double)4.0F, (double)4.0F, (double)4.0F, (double)1.0F, (double)20.0F);
         }
      }

      if (this.filteredItems.isEmpty()) {
         TextRenderer.drawCenteredString("No items found", drawContext, n10 + 270, n11 + n12 / 2 - 10, (new Color(150, 150, 150, 200)).getRGB());
      }

      int n21 = n7 + 450 - 45;
      int n22 = n6 + 580 - 80 - 20;
      int n23 = n22 - 80 - 10;
      int n24 = n23 - 80 - 10;
      RenderUtils.renderRoundedQuad(drawContext.getMatrices(), Utils.getMainColor(255, 1), (double)n22, (double)n21, (double)(n22 + 80), (double)(n21 + 30), (double)5.0F, (double)5.0F, (double)5.0F, (double)5.0F, (double)20.0F);
      TextRenderer.drawCenteredString("Save", drawContext, n22 + 40, n21 + 8, (new Color(245, 245, 245, 255)).getRGB());
      RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(60, 60, 65, 255), (double)n23, (double)n21, (double)(n23 + 80), (double)(n21 + 30), (double)5.0F, (double)5.0F, (double)5.0F, (double)5.0F, (double)20.0F);
      TextRenderer.drawCenteredString("Cancel", drawContext, n23 + 40, n21 + 8, (new Color(245, 245, 245, 255)).getRGB());
      RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(70, 40, 40, 255), (double)n24, (double)n21, (double)(n24 + 80), (double)(n21 + 30), (double)5.0F, (double)5.0F, (double)5.0F, (double)5.0F, (double)20.0F);
      TextRenderer.drawCenteredString("Reset", drawContext, n24 + 40, n21 + 8, (new Color(245, 245, 245, 255)).getRGB());
      RenderUtils.scaledProjection();
   }

   public boolean mouseClicked(double n, double n2, int n3) {
      double n4 = n * MinecraftClient.getInstance().getWindow().getScaleFactor();
      double n5 = n2 * MinecraftClient.getInstance().getWindow().getScaleFactor();
      int n6 = (this.this$0.mc.getWindow().getWidth() - 600) / 2;
      int n7 = (this.this$0.mc.getWindow().getHeight() - 450) / 2;
      int n8 = n7 + 450 - 45;
      int n9 = n6 + 600 - 80 - 20;
      int n10 = n9 - 80 - 10;
      if (this.isInBounds(n4, n5, n9, n8, 80, 30)) {
         if (this.selectedIndex >= 0 && this.selectedIndex < this.filteredItems.size()) {
            this.setting.setItem((Item)this.filteredItems.get(this.selectedIndex));
         }

         this.this$0.mc.setScreen(Gamble.INSTANCE.GUI);
         return true;
      } else if (this.isInBounds(n4, n5, n10, n8, 80, 30)) {
         this.this$0.mc.setScreen(Gamble.INSTANCE.GUI);
         return true;
      } else if (!this.isInBounds(n4, n5, n10 - 80 - 10, n8, 80, 30)) {
         int n11 = n6 + 20;
         int n12 = n7 + 50 + 30 + 15;
         if (this.isInBounds(n4, n5, n11, n12, 560, 450 - (n12 - n7) - 60)) {
            int n13 = this.scrollOffset * 11;
            int n14 = (int)(n4 - (double)n11 - (double)5.0F) / 48;
            if (n14 >= 0 && n14 < 11) {
               int selectedIndex = n13 + (int)(n5 - (double)n12 - (double)5.0F) / 48 * 11 + n14;
               if (selectedIndex >= 0 && selectedIndex < this.filteredItems.size()) {
                  this.selectedIndex = selectedIndex;
                  return true;
               }
            }
         }

         return super.mouseClicked(n4, n5, n3);
      } else {
         this.setting.setItem(this.setting.getDefaultValue());
         this.selectedIndex = -1;

         for(int i = 0; i < this.filteredItems.size(); ++i) {
            if (this.filteredItems.get(i) == this.setting.getDefaultValue()) {
               this.selectedIndex = i;
               break;
            }
         }

         return true;
      }
   }

   public boolean mouseScrolled(double n, double n2, double n3, double n4) {
      double n5 = n * MinecraftClient.getInstance().getWindow().getScaleFactor();
      double n6 = n2 * MinecraftClient.getInstance().getWindow().getScaleFactor();
      int width = this.this$0.mc.getWindow().getWidth();
      int n7 = (this.this$0.mc.getWindow().getHeight() - 450) / 2;
      int n8 = n7 + 50 + 30 + 15;
      if (this.isInBounds(n5, n6, (width - 600) / 2 + 20, n8, 560, 450 - (n8 - n7) - 60)) {
         int max = Math.max(0, (int)Math.ceil((double)this.filteredItems.size() / (double)11.0F) - 6);
         if (n4 > (double)0.0F) {
            this.scrollOffset = Math.max(0, this.scrollOffset - 1);
         } else if (n4 < (double)0.0F) {
            this.scrollOffset = Math.min(max, this.scrollOffset + 1);
         }

         return true;
      } else {
         return super.mouseScrolled(n5, n6, n3, n4);
      }
   }

   public boolean keyPressed(int n, int n2, int n3) {
      if (n == 256) {
         if (this.selectedIndex >= 0 && this.selectedIndex < this.filteredItems.size()) {
            this.setting.setItem((Item)this.filteredItems.get(this.selectedIndex));
         }

         this.this$0.mc.setScreen(Gamble.INSTANCE.GUI);
         return true;
      } else if (n == 259) {
         if (!this.searchQuery.isEmpty()) {
            this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
            this.updateFilteredItems();
         }

         return true;
      } else if (n == 265) {
         if (this.selectedIndex >= 11) {
            this.selectedIndex -= 11;
            this.ensureSelectedItemVisible();
         }

         return true;
      } else if (n == 264) {
         if (this.selectedIndex + 11 < this.filteredItems.size()) {
            this.selectedIndex += 11;
            this.ensureSelectedItemVisible();
         }

         return true;
      } else if (n == 263) {
         if (this.selectedIndex > 0) {
            --this.selectedIndex;
            this.ensureSelectedItemVisible();
         }

         return true;
      } else if (n == 262) {
         if (this.selectedIndex < this.filteredItems.size() - 1) {
            ++this.selectedIndex;
            this.ensureSelectedItemVisible();
         }

         return true;
      } else if (n == 257) {
         if (this.selectedIndex >= 0 && this.selectedIndex < this.filteredItems.size()) {
            this.setting.setItem((Item)this.filteredItems.get(this.selectedIndex));
            this.this$0.mc.setScreen(Gamble.INSTANCE.GUI);
         }

         return true;
      } else {
         return super.keyPressed(n, n2, n3);
      }
   }

   public boolean charTyped(char c, int n) {
      this.searchQuery = this.searchQuery + c;
      this.updateFilteredItems();
      return true;
   }

   private void updateFilteredItems() {
      if (this.searchQuery.isEmpty()) {
         this.filteredItems = new ArrayList(this.allItems);
      } else {
         this.filteredItems = (List)this.allItems.stream().filter((item) -> ((Item)item).getName().getString().toLowerCase().contains(this.searchQuery.toLowerCase())).collect(Collectors.toList());
      }

      this.scrollOffset = 0;
      this.selectedIndex = -1;
      Item a = this.setting.getItem();
      if (a != null) {
         for(int i = 0; i < this.filteredItems.size(); ++i) {
            if (this.filteredItems.get(i) == a) {
               this.selectedIndex = i;
               break;
            }
         }
      }

   }

   private void ensureSelectedItemVisible() {
      if (this.selectedIndex >= 0) {
         int scrollOffset = this.selectedIndex / 11;
         if (scrollOffset < this.scrollOffset) {
            this.scrollOffset = scrollOffset;
         } else if (scrollOffset >= this.scrollOffset + 6) {
            this.scrollOffset = scrollOffset - 6 + 1;
         }

      }
   }

   private boolean isInBounds(double n, double n2, int n3, int n4, int n5, int n6) {
      return n >= (double)n3 && n <= (double)(n3 + n5) && n2 >= (double)n4 && n2 <= (double)(n4 + n6);
   }

   public void renderBackground(DrawContext drawContext, int n, int n2, float n3) {
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }
}
