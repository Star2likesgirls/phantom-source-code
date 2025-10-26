package dev.gambleclient.gui;

import dev.gambleclient.Gamble;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.modules.client.Phantom;
import dev.gambleclient.module.setting.BindSetting;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.ColorSetting;
import dev.gambleclient.module.setting.ItemSetting;
import dev.gambleclient.module.setting.MinMaxSetting;
import dev.gambleclient.module.setting.ModeSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.module.setting.StringSetting;
import dev.gambleclient.utils.ColorUtil;
import dev.gambleclient.utils.KeyUtils;
import dev.gambleclient.utils.MathUtil;
import dev.gambleclient.utils.RenderUtils;
import dev.gambleclient.utils.TextRenderer;
import java.awt.Color;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.ColorHelper;

public final class ClickGUI extends Screen {
   private Category selectedCategory;
   private Module selectedModule;
   private String searchQuery;
   public Color currentColor;
   private boolean searchFocused;
   private boolean draggingSlider;
   private Setting draggingSliderSetting;
   private BindSetting listeningBind = null;
   private boolean selectingItem = false;
   private ItemSetting activeItemSetting = null;
   private int itemScrollRowOffset = 0;
   private static final List ALL_ITEMS = new ArrayList();
   private String overlaySearchQuery = "";
   private boolean overlaySearchFocused = false;
   private StringSetting activeStringSetting = null;
   private ColorSetting activeColorSetting = null;
   private boolean draggingColorComponent = false;
   private int colorComponentIndex = -1;
   private static final int ICON_CELL_SIZE = 30;
   private static final int ICON_OVERLAY_ROWS_VISIBLE = 10;
   private static final int ICON_OVERLAY_WIDTH = 620;
   private static final int ICON_OVERLAY_TOP_PADDING = 66;
   private static final int OVERLAY_SEARCH_HEIGHT = 26;
   private static final int OVERLAY_SEARCH_PADDING_X = 12;
   private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
   private final Color PANEL_COLOR = new Color(30, 30, 35, 255);
   private final Color ACCENT_COLOR_FALLBACK = new Color(92, 250, 121, 255);
   private final Color TEXT_COLOR = new Color(220, 220, 220, 255);
   private final Color SEARCH_BG = new Color(40, 40, 45, 255);
   private final Color SEARCH_BG_FOCUSED = new Color(55, 55, 62, 255);
   private final Color HOVER_NEUTRAL = new Color(255, 255, 255, 28);
   private final Color OVERLAY_BG = new Color(18, 18, 22, 242);
   private final Color OVERLAY_BORDER = new Color(92, 250, 121, 110);
   private final Color OVERLAY_HOVER = new Color(92, 250, 121, 70);
   private final Color OVERLAY_SELECTED = new Color(92, 250, 121, 160);
   private static final int SETTINGS_PANEL_WIDTH = 380;
   private static final int CATEGORY_PANEL_WIDTH = 180;
   private static final int MODULE_PANEL_WIDTH = 350;
   private static final int HEADER_HEIGHT = 45;
   private static final int ITEM_HEIGHT = 32;
   private static final int SLIDER_ITEM_HEIGHT = 52;
   private static final int COLOR_ITEM_HEIGHT = 100;
   private static final int PADDING = 15;
   private static final int PANEL_SPACING = 20;
   private static final int TOTAL_WIDTH = 950;
   private static final int TOTAL_HEIGHT = 500;
   private int settingsScrollOffset = 0;
   private int maxSettingsScroll = 0;
   private float openProgress = 0.0F;
   private final EnumMap categoryAnim = new EnumMap(Category.class);
   private final Map moduleSelectAnim = new HashMap();
   private final Map moduleEnabledAnim = new HashMap();
   private final Map settingHoverAnim = new HashMap();
   private final List settingLayouts = new ArrayList();
   private boolean closing = false;

   public ClickGUI() {
      super(Text.empty());
      this.selectedCategory = Category.COMBAT;
      this.searchQuery = "";
      this.searchFocused = false;
      this.draggingSlider = false;
      this.draggingSliderSetting = null;

      for(Category c : Category.values()) {
         this.categoryAnim.put(c, 0.0F);
      }

   }

   private static int toMCColor(Color c) {
      return (c.getAlpha() << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
   }

   private Color getAccent() {
      try {
         return Phantom.enableRainbowEffect.getValue() ? ColorUtil.a(1, 255) : new Color(Phantom.redColor.getIntValue(), Phantom.greenColor.getIntValue(), Phantom.blueColor.getIntValue(), 255);
      } catch (Throwable var2) {
         return this.ACCENT_COLOR_FALLBACK;
      }
   }

   private int panelAlpha() {
      try {
         return Phantom.windowAlpha.getIntValue();
      } catch (Throwable var2) {
         return 200;
      }
   }

   private float lerp(float current, float target, double speed) {
      return current + (float)((double)(target - current) * Math.min((double)1.0F, speed));
   }

   private Color withAlpha(Color c, int a) {
      return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
   }

   private Color blend(Color a, Color b, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int r = (int)((float)a.getRed() + (float)(b.getRed() - a.getRed()) * t);
      int g = (int)((float)a.getGreen() + (float)(b.getGreen() - a.getGreen()) * t);
      int bl = (int)((float)a.getBlue() + (float)(b.getBlue() - a.getBlue()) * t);
      int al = (int)((float)a.getAlpha() + (float)(b.getAlpha() - a.getAlpha()) * t);
      return new Color(r, g, bl, al);
   }

   public boolean isDraggingAlready() {
      return this.draggingSlider;
   }

   public void setTooltip(CharSequence tooltipText, int tooltipX, int tooltipY) {
   }

   public void setInitialFocus() {
      if (this.client != null) {
         super.setInitialFocus();
      }

   }

   public void render(DrawContext drawContext, int n, int n2, float n3) {
      if (Gamble.mc.currentScreen == this) {
         if (this.currentColor == null) {
            this.currentColor = new Color(0, 0, 0, 0);
         }

         int targetAlpha = Phantom.renderBackground.getValue() ? 200 : 0;
         if (this.currentColor.getAlpha() != targetAlpha) {
            this.currentColor = ColorUtil.a(0.05F, targetAlpha, this.currentColor);
         }

         drawContext.fill(0, 0, Gamble.mc.getWindow().getWidth(), Gamble.mc.getWindow().getHeight(), this.currentColor.getRGB());
         RenderUtils.unscaledProjection();
         int scaledX = n * (int)MinecraftClient.getInstance().getWindow().getScaleFactor();
         int scaledY = n2 * (int)MinecraftClient.getInstance().getWindow().getScaleFactor();
         super.render(drawContext, scaledX, scaledY, n3);
         this.openProgress = this.lerp(this.openProgress, 1.0F, 0.15);
         this.renderBackground(drawContext);
         this.renderSettingsPanel(drawContext, scaledX, scaledY);
         this.renderCategoryPanel(drawContext, scaledX, scaledY);
         this.renderModulePanel(drawContext, scaledX, scaledY);
         if (this.selectingItem && this.activeItemSetting != null) {
            this.renderItemIconOverlay(drawContext, scaledX, scaledY);
         }

         RenderUtils.scaledProjection();
      }
   }

   private void renderBackground(DrawContext drawContext) {
      if (Phantom.renderBackground.getValue()) {
         drawContext.fill(0, 0, Gamble.mc.getWindow().getWidth(), Gamble.mc.getWindow().getHeight(), toMCColor(new Color(0, 0, 0, (int)(100.0F * this.openProgress))));
      }

   }

   private void renderSettingsPanel(DrawContext ctx, int mouseX, int mouseY) {
      int sw = Gamble.mc.getWindow().getWidth();
      int sh = Gamble.mc.getWindow().getHeight();
      int startX = (sw - 950) / 2;
      int startY = (sh - 500) / 2;
      int endX = startX + 380;
      RenderUtils.renderRoundedQuad(ctx.getMatrices(), this.withAlpha(this.PANEL_COLOR, (int)((float)this.panelAlpha() * this.openProgress)), (double)startX, (double)startY, (double)endX, (double)(startY + 500), (double)8.0F, (double)8.0F, (double)8.0F, (double)8.0F, (double)50.0F);
      if (this.selectedModule == null) {
         TextRenderer.drawCenteredString("SELECT A MODULE", ctx, startX + 190, startY + 100, toMCColor(new Color(120, 120, 120, (int)(255.0F * this.openProgress))));
      } else {
         TextRenderer.drawString("SETTINGS: " + this.selectedModule.getName().toString().toUpperCase(), ctx, startX + 15, startY + 15, toMCColor(this.getAccent()));
         int availableHeight = 425;
         int contentStartY = startY + 45 + 15;
         this.settingLayouts.clear();
         int builderY = 0;

         for(Object o : this.selectedModule.getSettings()) {
            if (o instanceof Setting) {
               Setting s = (Setting)o;
               int h;
               if (!(s instanceof NumberSetting) && !(s instanceof MinMaxSetting)) {
                  if (s instanceof ColorSetting) {
                     h = 100;
                  } else {
                     h = 32;
                  }
               } else {
                  h = 52;
               }

               this.settingLayouts.add(new SettingLayout(s, builderY, h));
               builderY += h + 6;
            }
         }

         this.maxSettingsScroll = Math.max(0, builderY - availableHeight);
         this.settingsScrollOffset = Math.max(0, Math.min(this.settingsScrollOffset, this.maxSettingsScroll));

         for(int i = 0; i < this.settingLayouts.size(); ++i) {
            SettingLayout layout = (SettingLayout)this.settingLayouts.get(i);
            int drawY = contentStartY + layout.relativeY - this.settingsScrollOffset;
            if (drawY + layout.height >= contentStartY - 4 && drawY <= contentStartY + availableHeight + 4) {
               boolean hovered = this.isHoveredInRect(mouseX, mouseY, startX, drawY, 380, layout.height);
               boolean editingString = layout.setting instanceof StringSetting && layout.setting == this.activeStringSetting;
               boolean editingColor = layout.setting instanceof ColorSetting && layout.setting == this.activeColorSetting;
               float prev = (Float)this.settingHoverAnim.getOrDefault(layout.setting, 0.0F);
               float anim = this.lerp(prev, (!hovered || this.draggingSlider || this.selectingItem) && !editingString && !editingColor ? 0.0F : 1.0F, (double)0.25F);
               this.settingHoverAnim.put(layout.setting, anim);
               if (anim > 0.0F) {
                  Color hover = new Color(255, 255, 255, (int)(28.0F * anim));
                  RenderUtils.renderRoundedQuad(ctx.getMatrices(), hover, (double)(startX + 5), (double)drawY, (double)(endX - 5), (double)(drawY + layout.height), (double)6.0F, (double)6.0F, (double)6.0F, (double)6.0F, (double)20.0F);
               }

               TextRenderer.drawString(layout.setting.getName().toString().toUpperCase(), ctx, startX + 15, drawY + 8, toMCColor(this.TEXT_COLOR));
               this.renderSettingValue(ctx, layout.setting, startX, endX, drawY);
               if (i < this.settingLayouts.size() - 1) {
                  ctx.fill(startX + 10, drawY + layout.height + 2, endX - 10, drawY + layout.height + 3, toMCColor(new Color(255, 255, 255, 12)));
               }
            }
         }

         if (this.maxSettingsScroll > 0) {
            int barX = endX - 6;
            ctx.fill(barX, contentStartY, barX + 3, contentStartY + availableHeight, toMCColor(new Color(60, 60, 65, 130)));
            double ratio = (double)this.settingsScrollOffset / (double)this.maxSettingsScroll;
            int knobH = Math.max(24, (int)((double)availableHeight * ((double)availableHeight / (double)builderY)));
            int knobY = contentStartY + (int)((double)(availableHeight - knobH) * ratio);
            ctx.fill(barX, knobY, barX + 3, knobY + knobH, toMCColor(this.getAccent()));
         }

      }
   }

   private void renderSettingValue(DrawContext ctx, Setting setting, int startX, int endX, int y) {
      Color a = this.getAccent();
      if (setting instanceof BooleanSetting b) {
         String v = b.getValue() ? "ON" : "OFF";
         Color c = b.getValue() ? a : new Color(130, 130, 130, 255);
         TextRenderer.drawString(v, ctx, endX - 15 - TextRenderer.getWidth(v), y + 8, toMCColor(c));
      } else if (setting instanceof NumberSetting n) {
         String v = String.format("%.2f", n.getValue());
         TextRenderer.drawString(v, ctx, endX - 15 - TextRenderer.getWidth(v), y + 8, toMCColor(a));
         int sliderY = y + 28;
         int sx = startX + 15;
         int ex = endX - 15;
         int w = ex - sx;
         RenderUtils.renderRoundedQuad(ctx.getMatrices(), new Color(58, 58, 63, 255), (double)sx, (double)sliderY, (double)ex, (double)(sliderY + 5), (double)2.0F, (double)2.0F, (double)2.0F, (double)2.0F, (double)16.0F);
         double prog = (n.getValue() - n.getMin()) / (n.getMax() - n.getMin());
         prog = Math.max((double)0.0F, Math.min((double)1.0F, prog));
         int fill = (int)((double)w * prog);
         if (fill > 0) {
            RenderUtils.renderRoundedQuad(ctx.getMatrices(), a, (double)sx, (double)sliderY, (double)(sx + fill), (double)(sliderY + 5), (double)2.0F, (double)2.0F, (double)2.0F, (double)2.0F, (double)16.0F);
         }

         RenderUtils.renderRoundedQuad(ctx.getMatrices(), Color.WHITE, (double)(sx + fill - 4), (double)(sliderY - 3), (double)(sx + fill + 4), (double)(sliderY + 8), (double)4.0F, (double)4.0F, (double)4.0F, (double)4.0F, (double)12.0F);
      } else if (setting instanceof ModeSetting m) {
         String v = m.getValue().toString();
         TextRenderer.drawString(v, ctx, endX - 15 - TextRenderer.getWidth(v), y + 8, toMCColor(a));
      } else if (setting instanceof BindSetting bind) {
         String v = this.listeningBind == bind ? "..." : (bind.getValue() == -1 ? "NONE" : KeyUtils.getKey(bind.getValue()).toString());
         TextRenderer.drawString(v, ctx, endX - 15 - TextRenderer.getWidth(v), y + 8, toMCColor(a));
      } else if (setting instanceof StringSetting s) {
         boolean editing = s == this.activeStringSetting;
         String raw = s.getValue();
         String base = raw.isEmpty() ? "EMPTY" : raw;
         Color col = raw.isEmpty() && !editing ? new Color(150, 150, 150, 255) : a;
         if (editing && System.currentTimeMillis() % 1000L < 500L) {
            base = raw + "|";
         }

         TextRenderer.drawString(base, ctx, endX - 15 - TextRenderer.getWidth(base), y + 8, toMCColor(col));
      } else if (setting instanceof MinMaxSetting mm) {
         String v = String.format("%.1f - %.1f", mm.getCurrentMin(), mm.getCurrentMax());
         TextRenderer.drawString(v, ctx, endX - 15 - TextRenderer.getWidth(v), y + 8, toMCColor(a));
         int sliderY = y + 28;
         int sx = startX + 15;
         int ex = endX - 15;
         int w = ex - sx;
         RenderUtils.renderRoundedQuad(ctx.getMatrices(), new Color(58, 58, 63, 255), (double)sx, (double)sliderY, (double)ex, (double)(sliderY + 5), (double)2.0F, (double)2.0F, (double)2.0F, (double)2.0F, (double)16.0F);
         double minP = (mm.getCurrentMin() - mm.getMinValue()) / (mm.getMaxValue() - mm.getMinValue());
         double maxP = (mm.getCurrentMax() - mm.getMinValue()) / (mm.getMaxValue() - mm.getMinValue());
         minP = Math.max((double)0.0F, Math.min((double)1.0F, minP));
         maxP = Math.max((double)0.0F, Math.min((double)1.0F, maxP));
         int minX = sx + (int)((double)w * minP);
         int maxX = sx + (int)((double)w * maxP);
         if (maxX > minX) {
            RenderUtils.renderRoundedQuad(ctx.getMatrices(), a, (double)minX, (double)sliderY, (double)maxX, (double)(sliderY + 5), (double)2.0F, (double)2.0F, (double)2.0F, (double)2.0F, (double)16.0F);
         }

         RenderUtils.renderRoundedQuad(ctx.getMatrices(), Color.WHITE, (double)(minX - 4), (double)(sliderY - 3), (double)(minX + 4), (double)(sliderY + 8), (double)4.0F, (double)4.0F, (double)4.0F, (double)4.0F, (double)12.0F);
         RenderUtils.renderRoundedQuad(ctx.getMatrices(), Color.WHITE, (double)(maxX - 4), (double)(sliderY - 3), (double)(maxX + 4), (double)(sliderY + 8), (double)4.0F, (double)4.0F, (double)4.0F, (double)4.0F, (double)12.0F);
      } else if (setting instanceof ItemSetting is) {
         Item item = is.getItem();
         String id = item == null ? "NONE" : Registries.ITEM.getId(item).toString().toUpperCase();
         TextRenderer.drawString(id, ctx, endX - 15 - TextRenderer.getWidth(id), y + 8, toMCColor(a));
      } else if (setting instanceof ColorSetting cs) {
         Color val = cs.getValue();
         String hex = String.format("#%02X%02X%02X", val.getRed(), val.getGreen(), val.getBlue());
         int previewSize = 16;
         int previewX = endX - 15 - previewSize;
         int previewY = y + 7;
         RenderUtils.renderRoundedQuad(ctx.getMatrices(), val, (double)previewX, (double)previewY, (double)(previewX + previewSize), (double)(previewY + previewSize), (double)4.0F, (double)4.0F, (double)4.0F, (double)4.0F, (double)16.0F);
         ctx.fill(previewX - 1, previewY - 1, previewX + previewSize + 1, previewY, toMCColor(Color.DARK_GRAY));
         ctx.fill(previewX - 1, previewY + previewSize, previewX + previewSize + 1, previewY + previewSize + 1, toMCColor(Color.DARK_GRAY));
         ctx.fill(previewX - 1, previewY, previewX, previewY + previewSize, toMCColor(Color.DARK_GRAY));
         ctx.fill(previewX + previewSize, previewY, previewX + previewSize + 1, previewY + previewSize, toMCColor(Color.DARK_GRAY));
         TextRenderer.drawString(hex, ctx, previewX - 6 - TextRenderer.getWidth(hex), y + 8, toMCColor(a));
         if (this.activeColorSetting == cs) {
            int sliderStartX = startX + 15;
            int sliderEndX = endX - 15 - 20;
            int baseY = y + 32;
            this.drawColorComponentSlider(ctx, "R", 0, val.getRed(), 255, sliderStartX, sliderEndX, baseY, new Color(255, 80, 80, 255));
            this.drawColorComponentSlider(ctx, "G", 1, val.getGreen(), 255, sliderStartX, sliderEndX, baseY + 20, new Color(80, 255, 80, 255));
            this.drawColorComponentSlider(ctx, "B", 2, val.getBlue(), 255, sliderStartX, sliderEndX, baseY + 40, new Color(80, 80, 255, 255));
            String hint = "L-CLICK SLIDER | L-CLICK PREVIEW TO CLOSE | R-CLICK RESET";
            TextRenderer.drawString(hint, ctx, sliderStartX, baseY + 62, toMCColor(new Color(140, 140, 140, 200)));
         } else {
            String hint = "CLICK PREVIEW TO EDIT";
            TextRenderer.drawString(hint, ctx, startX + 15, y + 32, toMCColor(new Color(140, 140, 140, 160)));
         }
      }

   }

   private void drawColorComponentSlider(DrawContext ctx, String name, int index, int value, int max, int sx, int ex, int y, Color col) {
      TextRenderer.drawString(name + ":" + value, ctx, sx, y - 2, toMCColor(col));
      int barY = y + 8;
      RenderUtils.renderRoundedQuad(ctx.getMatrices(), new Color(50, 50, 55, 255), (double)sx, (double)barY, (double)ex, (double)(barY + 5), (double)2.0F, (double)2.0F, (double)2.0F, (double)2.0F, (double)14.0F);
      double prog = (double)value / (double)max;
      int fill = (int)((double)(ex - sx) * prog);
      if (fill > 0) {
         RenderUtils.renderRoundedQuad(ctx.getMatrices(), col, (double)sx, (double)barY, (double)(sx + fill), (double)(barY + 5), (double)2.0F, (double)2.0F, (double)2.0F, (double)2.0F, (double)14.0F);
      }

      RenderUtils.renderRoundedQuad(ctx.getMatrices(), Color.WHITE, (double)(sx + fill - 3), (double)(barY - 3), (double)(sx + fill + 3), (double)(barY + 8), (double)3.0F, (double)3.0F, (double)3.0F, (double)3.0F, (double)12.0F);
   }

   private void applyColorComponent(ColorSetting cs, int compIndex, int mx, int sx, int ex) {
      int clamped = Math.max(0, Math.min(255, (int)((double)(mx - sx) / (double)(ex - sx) * (double)255.0F)));
      Color old = cs.getValue();
      int r = old.getRed();
      int g = old.getGreen();
      int b = old.getBlue();
      if (compIndex == 0) {
         r = clamped;
      } else if (compIndex == 1) {
         g = clamped;
      } else if (compIndex == 2) {
         b = clamped;
      }

      cs.setValue(new Color(r, g, b, 255));
   }

   private void renderCategoryPanel(DrawContext ctx, int mouseX, int mouseY) {
      int sw = Gamble.mc.getWindow().getWidth();
      int sh = Gamble.mc.getWindow().getHeight();
      int startX = (sw - 950) / 2 + 380 + 20;
      int startY = (sh - 500) / 2;
      int endX = startX + 180;
      RenderUtils.renderRoundedQuad(ctx.getMatrices(), this.withAlpha(this.PANEL_COLOR, (int)((float)this.panelAlpha() * this.openProgress)), (double)startX, (double)startY, (double)endX, (double)(startY + 500), (double)8.0F, (double)8.0F, (double)8.0F, (double)8.0F, (double)50.0F);
      TextRenderer.drawString("Phantom+", ctx, startX + 15, startY + 15, toMCColor(this.getAccent()));
      int y = startY + 45 + 15;

      for(Category c : Category.values()) {
         boolean selected = c == this.selectedCategory;
         boolean hovered = this.isHoveredInRect(mouseX, mouseY, startX, y, 180, 32);
         float prev = (Float)this.categoryAnim.getOrDefault(c, 0.0F);
         float anim = this.lerp(prev, selected ? 1.0F : 0.0F, (double)0.25F);
         this.categoryAnim.put(c, anim);
         Color bg = null;
         if (selected) {
            bg = this.withAlpha(this.getAccent(), (int)((double)this.getAccent().getAlpha() * 0.55));
         } else if (hovered) {
            bg = this.HOVER_NEUTRAL;
         }

         if (bg != null) {
            RenderUtils.renderRoundedQuad(ctx.getMatrices(), bg, (double)(startX + 5), (double)y, (double)(endX - 5), (double)(y + 32), (double)6.0F, (double)6.0F, (double)6.0F, (double)6.0F, (double)24.0F);
         }

         Color textCol = selected ? Color.WHITE : this.TEXT_COLOR;
         TextRenderer.drawString(c.name.toString().toUpperCase(), ctx, startX + 15, y + 8, toMCColor(textCol));
         y += 37;
      }

   }

   private void renderModulePanel(DrawContext ctx, int mouseX, int mouseY) {
      int sw = Gamble.mc.getWindow().getWidth();
      int sh = Gamble.mc.getWindow().getHeight();
      int startX = (sw - 950) / 2 + 380 + 20 + 180 + 20;
      int startY = (sh - 500) / 2;
      int endX = startX + 350;
      RenderUtils.renderRoundedQuad(ctx.getMatrices(), this.withAlpha(this.PANEL_COLOR, (int)((float)this.panelAlpha() * this.openProgress)), (double)startX, (double)startY, (double)endX, (double)(startY + 500), (double)8.0F, (double)8.0F, (double)8.0F, (double)8.0F, (double)50.0F);
      TextRenderer.drawString("CATEGORY: " + this.selectedCategory.name.toString().toUpperCase(), ctx, startX + 15, startY + 15, toMCColor(this.TEXT_COLOR));
      int searchHeight = 25;
      int searchY = startY + 45 - 15;
      int searchStartX = startX + 15;
      int searchEndX = endX - 15;
      Color searchBgColor = this.searchFocused ? this.SEARCH_BG_FOCUSED : this.SEARCH_BG;
      RenderUtils.renderRoundedQuad(ctx.getMatrices(), searchBgColor, (double)searchStartX, (double)searchY, (double)searchEndX, (double)(searchY + 25), (double)6.0F, (double)6.0F, (double)6.0F, (double)6.0F, (double)30.0F);
      String searchText = this.searchQuery.isEmpty() ? "SEARCH..." : this.searchQuery;
      Color searchTextColor = this.searchQuery.isEmpty() ? new Color(120, 120, 120, 255) : this.TEXT_COLOR;
      Objects.requireNonNull(MinecraftClient.getInstance().textRenderer);
      int fontHeight = 9;
      int centeredTextY = searchY + (25 - fontHeight) / 2;
      TextRenderer.drawString(searchText, ctx, searchStartX + 10, centeredTextY, toMCColor(searchTextColor));
      if (this.searchFocused && System.currentTimeMillis() % 1000L < 500L) {
         int cursorX = searchStartX + 10 + TextRenderer.getWidth(this.searchQuery);
         ctx.fill(cursorX, centeredTextY - 1, cursorX + 1, centeredTextY + fontHeight, toMCColor(this.TEXT_COLOR));
      }

      List<Module> modules = Gamble.INSTANCE.getModuleManager().a(this.selectedCategory);
      int y = startY + 45 + 25;

      for(Module m : modules) {
         if (this.searchQuery.isEmpty() || m.getName().toString().toLowerCase().contains(this.searchQuery.toLowerCase())) {
            boolean selected = m == this.selectedModule;
            boolean hovered = this.isHoveredInRect(mouseX, mouseY, startX, y, 350, 32);
            boolean enabled = m.isEnabled();
            float prevSel = (Float)this.moduleSelectAnim.getOrDefault(m, 0.0F);
            float selAnim = this.lerp(prevSel, selected ? 1.0F : 0.0F, (double)0.25F);
            this.moduleSelectAnim.put(m, selAnim);
            float prevEn = (Float)this.moduleEnabledAnim.getOrDefault(m, 0.0F);
            float enAnim = this.lerp(prevEn, enabled ? 1.0F : 0.0F, 0.2);
            this.moduleEnabledAnim.put(m, enAnim);
            Color bg = null;
            if (selected) {
               bg = this.withAlpha(this.getAccent(), (int)((double)this.getAccent().getAlpha() * 0.55));
            } else if (hovered) {
               bg = this.HOVER_NEUTRAL;
            }

            if (bg != null) {
               RenderUtils.renderRoundedQuad(ctx.getMatrices(), bg, (double)(startX + 5), (double)y, (double)(endX - 5), (double)(y + 32), (double)6.0F, (double)6.0F, (double)6.0F, (double)6.0F, (double)24.0F);
            }

            Color textCol = enabled ? this.blend(this.getAccent(), Color.WHITE, selAnim * 0.4F) : this.TEXT_COLOR;
            TextRenderer.drawString(m.getName().toString().toUpperCase(), ctx, startX + 15, y + 8, toMCColor(textCol));
            int indicatorWidth = (int)(12.0F + 10.0F * enAnim);
            Color indicator = enabled ? this.withAlpha(this.getAccent(), 200) : new Color(80, 80, 85, 200);
            RenderUtils.renderRoundedQuad(ctx.getMatrices(), indicator, (double)(endX - 10 - indicatorWidth), (double)(y + 9), (double)(endX - 10), (double)(y + 23), (double)4.0F, (double)4.0F, (double)4.0F, (double)4.0F, (double)16.0F);
            y += 35;
         }
      }

   }

   private boolean isHoveredInRect(int mouseX, int mouseY, int x, int y, int width, int height) {
      return mouseX >= x && mouseX <= x + width && mouseY <= y + height && mouseY >= y;
   }

   private boolean isOverSettingsPanel(int mx, int my) {
      int sw = Gamble.mc.getWindow().getWidth();
      int sh = Gamble.mc.getWindow().getHeight();
      int startX = (sw - 950) / 2;
      int startY = (sh - 500) / 2;
      return this.isHoveredInRect(mx, my, startX, startY + 45, 380, 455);
   }

   private boolean isSearchBarHovered(int mouseX, int mouseY) {
      int sw = Gamble.mc.getWindow().getWidth();
      int sh = Gamble.mc.getWindow().getHeight();
      int startX = (sw - 950) / 2 + 380 + 20 + 180 + 20;
      int startY = (sh - 500) / 2;
      int searchY = startY + 45 - 15;
      int searchStartX = startX + 15;
      int searchEndX = startX + 350 - 15;
      return this.isHoveredInRect(mouseX, mouseY, searchStartX, searchY, searchEndX - searchStartX, 25);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.activeStringSetting != null) {
         if (keyCode == 259) {
            String current = this.activeStringSetting.getValue();
            if (!current.isEmpty()) {
               this.activeStringSetting.setValue(current.substring(0, current.length() - 1));
            }

            return true;
         }

         if (keyCode == 257 || keyCode == 335) {
            this.activeStringSetting = null;
            return true;
         }

         if (keyCode == 256) {
            this.activeStringSetting = null;
            return true;
         }
      }

      if (this.selectingItem) {
         if (this.overlaySearchFocused) {
            if (keyCode == 259 && !this.overlaySearchQuery.isEmpty()) {
               this.overlaySearchQuery = this.overlaySearchQuery.substring(0, this.overlaySearchQuery.length() - 1);
               return true;
            }

            if (keyCode == 256) {
               this.overlaySearchFocused = false;
               return true;
            }
         } else if (keyCode == 256) {
            this.closeItemOverlay();
            return true;
         }
      }

      if (this.listeningBind == null) {
         if (this.searchFocused) {
            if (keyCode == 259 && !this.searchQuery.isEmpty()) {
               this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
               return true;
            }

            if (keyCode == 256) {
               this.searchFocused = false;
               return true;
            }
         }

         return super.keyPressed(keyCode, scanCode, modifiers);
      } else {
         if (keyCode != 256 && keyCode != 259) {
            this.listeningBind.setValue(keyCode);
         } else {
            this.listeningBind.setValue(-1);
         }

         if (this.listeningBind.isModuleKey() && this.selectedModule != null && this.selectedModule.getSettings().contains(this.listeningBind)) {
            this.selectedModule.setKeybind(this.listeningBind.getValue());
         }

         this.listeningBind = null;
         return true;
      }
   }

   public boolean charTyped(char chr, int modifiers) {
      if (this.activeStringSetting != null) {
         if (this.isAllowed(chr)) {
            String cur = this.activeStringSetting.getValue();
            if (cur.length() < 64) {
               this.activeStringSetting.setValue(cur + chr);
            }

            return true;
         } else {
            return super.charTyped(chr, modifiers);
         }
      } else if (this.selectingItem && this.overlaySearchFocused) {
         if (this.isAllowed(chr)) {
            this.overlaySearchQuery = this.overlaySearchQuery + chr;
            return true;
         } else {
            return super.charTyped(chr, modifiers);
         }
      } else if (this.searchFocused && this.isAllowed(chr)) {
         this.searchQuery = this.searchQuery + chr;
         return true;
      } else {
         return super.charTyped(chr, modifiers);
      }
   }

   private boolean isAllowed(char c) {
      return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == ' ' || c == '.';
   }

   public boolean mouseClicked(double mx, double my, int button) {
      double smx = mx * MinecraftClient.getInstance().getWindow().getScaleFactor();
      double smy = my * MinecraftClient.getInstance().getWindow().getScaleFactor();
      if (this.selectingItem) {
         if (this.handleOverlaySearchClick((int)smx, (int)smy)) {
            return true;
         } else if (!this.isInsideItemOverlay((int)smx, (int)smy)) {
            this.closeItemOverlay();
            return true;
         } else {
            this.handleItemIconOverlayClick((int)smx, (int)smy, button);
            return true;
         }
      } else {
         int sw = Gamble.mc.getWindow().getWidth();
         int sh = Gamble.mc.getWindow().getHeight();
         boolean clickedInsideStringRow = false;
         boolean clickedInsideColorRow = false;
         if (this.isSearchBarHovered((int)smx, (int)smy)) {
            this.searchFocused = true;
            this.activeStringSetting = null;
            this.activeColorSetting = null;
            return true;
         } else {
            this.searchFocused = false;
            int catX = (sw - 950) / 2 + 380 + 20;
            int catY = (sh - 500) / 2 + 45 + 15;

            for(Category c : Category.values()) {
               if (this.isHoveredInRect((int)smx, (int)smy, catX, catY, 180, 32)) {
                  this.selectedCategory = c;
                  this.selectedModule = null;
                  this.activeStringSetting = null;
                  this.activeColorSetting = null;
                  return true;
               }

               catY += 37;
            }

            int modX = (sw - 950) / 2 + 380 + 20 + 180 + 20;
            int modY = (sh - 500) / 2 + 45 + 25;

            for(Object mObj : Gamble.INSTANCE.getModuleManager().a(this.selectedCategory)) {
               Module m = (Module)mObj;
               if (this.searchQuery.isEmpty() || m.getName().toString().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                  if (this.isHoveredInRect((int)smx, (int)smy, modX, modY, 350, 32)) {
                     if (button == 0) {
                        m.toggle();
                     } else if (button == 1) {
                        this.selectedModule = m;
                     }

                     this.activeStringSetting = null;
                     this.activeColorSetting = null;
                     return true;
                  }

                  modY += 35;
               }
            }

            if (this.selectedModule != null) {
               int settingsX = (sw - 950) / 2;
               int startY = (sh - 500) / 2 + 45 + 15;

               for(Object lObj : this.settingLayouts) {
                  SettingLayout l = (SettingLayout)lObj;
                  int drawY = startY + l.relativeY - this.settingsScrollOffset;
                  if (this.isHoveredInRect((int)smx, (int)smy, settingsX, drawY, 380, l.height)) {
                     if (l.setting instanceof StringSetting) {
                        clickedInsideStringRow = true;
                     }

                     if (l.setting instanceof ColorSetting) {
                        clickedInsideColorRow = true;
                     }

                     this.handleSettingClick(l.setting, button, (int)smx, (int)smy, settingsX, drawY, l.height);
                     return true;
                  }
               }
            }

            if (!clickedInsideStringRow && this.activeStringSetting != null) {
               this.activeStringSetting = null;
            }

            if (!clickedInsideColorRow && this.activeColorSetting != null && !this.draggingColorComponent) {
               this.activeColorSetting = null;
            }

            return super.mouseClicked(smx, smy, button);
         }
      }
   }

   private void handleSettingClick(Setting setting, int button, int mx, int my, int panelX, int settingY, int rowHeight) {
      if (!(setting instanceof StringSetting) && this.activeStringSetting != null) {
         this.activeStringSetting = null;
      }

      if (!(setting instanceof ColorSetting) && this.activeColorSetting != null && !this.draggingColorComponent) {
         this.activeColorSetting = null;
      }

      if (setting instanceof BooleanSetting b) {
         if (button == 0) {
            b.toggle();
         }
      } else if (setting instanceof ModeSetting m) {
         if (button == 0) {
            m.cycleUp();
         } else if (button == 1) {
            m.cycleDown();
         }
      } else if (setting instanceof NumberSetting n) {
         int sliderY = settingY + 28;
         int sx = panelX + 15;
         int ex = panelX + 380 - 15;
         if (this.isHoveredInRect(mx, my, sx, sliderY - 6, ex - sx, 18)) {
            this.draggingSlider = true;
            this.draggingSliderSetting = n;
            this.updateSliderValue(n, mx, sx, ex);
         }
      } else if (setting instanceof MinMaxSetting mm) {
         int sliderY = settingY + 28;
         int sx = panelX + 15;
         int ex = panelX + 380 - 15;
         if (this.isHoveredInRect(mx, my, sx, sliderY - 6, ex - sx, 18)) {
            this.draggingSlider = true;
            this.draggingSliderSetting = mm;
            this.updateMinMaxSliderValue(mm, mx, sx, ex);
         }
      } else if (setting instanceof BindSetting bind) {
         if (button == 0) {
            this.listeningBind = bind;
         } else if (button == 1) {
            bind.setValue(-1);
            if (bind.isModuleKey() && this.selectedModule != null && this.selectedModule.getSettings().contains(bind)) {
               this.selectedModule.setKeybind(-1);
            }

            this.listeningBind = null;
         }
      } else if (setting instanceof ItemSetting is) {
         if (button == 0) {
            this.openItemOverlay(is);
         } else if (button == 1) {
            is.setItem((Item)null);
         }
      } else if (setting instanceof StringSetting s) {
         if (button == 0) {
            this.activeStringSetting = s;
            this.searchFocused = false;
         } else if (button == 1) {
            s.setValue("");
            if (this.activeStringSetting == s) {
               this.activeStringSetting = null;
            }
         }
      } else if (setting instanceof ColorSetting cs) {
         int previewSize = 16;
         int previewX = panelX + 380 - 15 - previewSize;
         int previewY = settingY + 7;
         boolean inPreview = mx >= previewX && mx <= previewX + previewSize && my >= previewY && my <= previewY + previewSize;
         if (button == 1) {
            cs.resetValue();
            if (this.activeColorSetting == cs) {
               this.activeColorSetting = null;
            }

            return;
         }

         if (button == 0) {
            if (inPreview) {
               if (this.activeColorSetting == cs) {
                  this.activeColorSetting = null;
               } else {
                  this.activeColorSetting = cs;
                  this.activeStringSetting = null;
               }
            } else if (this.activeColorSetting == cs) {
               int sliderStartX = panelX + 15;
               int sliderEndX = panelX + 380 - 15 - 20;
               int baseY = settingY + 32;

               for(int i = 0; i < 3; ++i) {
                  int barY = baseY + i * 20 + 8;
                  if (this.isHoveredInRect(mx, my, sliderStartX, barY - 6, sliderEndX - sliderStartX, 18)) {
                     this.draggingColorComponent = true;
                     this.colorComponentIndex = i;
                     this.applyColorComponent(cs, i, mx, sliderStartX, sliderEndX);
                     break;
                  }
               }
            } else {
               this.activeColorSetting = cs;
               this.activeStringSetting = null;
            }
         }
      }

   }

   private void updateSliderValue(NumberSetting setting, int mx, int sx, int ex) {
      double prog = Math.max((double)0.0F, Math.min((double)1.0F, (double)(mx - sx) / (double)(ex - sx)));
      double newVal = setting.getMin() + prog * (setting.getMax() - setting.getMin());
      setting.getValue(MathUtil.roundToNearest(newVal, setting.getFormat()));
   }

   private void updateMinMaxSliderValue(MinMaxSetting s, int mx, int sx, int ex) {
      double prog = Math.max((double)0.0F, Math.min((double)1.0F, (double)(mx - sx) / (double)(ex - sx)));
      double newVal = s.getMinValue() + prog * (s.getMaxValue() - s.getMinValue());
      double minProg = (s.getCurrentMin() - s.getMinValue()) / (s.getMaxValue() - s.getMinValue());
      double maxProg = (s.getCurrentMax() - s.getMinValue()) / (s.getMaxValue() - s.getMinValue());
      if (Math.abs(prog - minProg) < Math.abs(prog - maxProg)) {
         s.setCurrentMin(Math.min(newVal, s.getCurrentMax()));
      } else {
         s.setCurrentMax(Math.max(newVal, s.getCurrentMin()));
      }

   }

   public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
      if (this.draggingSlider && this.draggingSliderSetting != null) {
         double smx = mx * MinecraftClient.getInstance().getWindow().getScaleFactor();
         int sw = Gamble.mc.getWindow().getWidth();
         int sx = (sw - 950) / 2 + 15;
         int ex = (sw - 950) / 2 + 380 - 15;
         Setting var17 = this.draggingSliderSetting;
         if (var17 instanceof NumberSetting) {
            NumberSetting n = (NumberSetting)var17;
            this.updateSliderValue(n, (int)smx, sx, ex);
         } else {
            var17 = this.draggingSliderSetting;
            if (var17 instanceof MinMaxSetting) {
               MinMaxSetting mm = (MinMaxSetting)var17;
               this.updateMinMaxSliderValue(mm, (int)smx, sx, ex);
            }
         }

         return true;
      } else if (this.draggingColorComponent && this.activeColorSetting != null) {
         double smx = mx * MinecraftClient.getInstance().getWindow().getScaleFactor();
         int sw = Gamble.mc.getWindow().getWidth();
         int panelX = (sw - 950) / 2;
         int sliderStartX = panelX + 15;
         int sliderEndX = panelX + 380 - 15 - 20;
         this.applyColorComponent(this.activeColorSetting, this.colorComponentIndex, (int)smx, sliderStartX, sliderEndX);
         return true;
      } else {
         return super.mouseDragged(mx, my, button, dx, dy);
      }
   }

   public boolean mouseReleased(double mx, double my, int button) {
      if (this.draggingSlider) {
         this.draggingSlider = false;
         this.draggingSliderSetting = null;
         return true;
      } else if (this.draggingColorComponent) {
         this.draggingColorComponent = false;
         this.colorComponentIndex = -1;
         return true;
      } else {
         return super.mouseReleased(mx, my, button);
      }
   }

   public boolean mouseScrolled(double mx, double my, double ha, double va) {
      double smx = mx * MinecraftClient.getInstance().getWindow().getScaleFactor();
      double smy = my * MinecraftClient.getInstance().getWindow().getScaleFactor();
      if (this.selectingItem) {
         if (va != (double)0.0F) {
            this.itemScrollRowOffset -= (int)Math.signum(va);
            return true;
         }
      } else if (this.selectedModule != null && va != (double)0.0F && this.isOverSettingsPanel((int)smx, (int)smy)) {
         this.settingsScrollOffset += (int)(-va * (double)18.0F);
         this.settingsScrollOffset = Math.max(0, Math.min(this.settingsScrollOffset, this.maxSettingsScroll));
         return true;
      }

      return super.mouseScrolled(mx, my, ha, va);
   }

   private void openItemOverlay(ItemSetting s) {
      this.activeItemSetting = s;
      this.selectingItem = true;
      this.itemScrollRowOffset = 0;
      this.overlaySearchQuery = "";
      this.overlaySearchFocused = false;
      this.activeStringSetting = null;
      this.activeColorSetting = null;
   }

   private void closeItemOverlay() {
      this.selectingItem = false;
      this.activeItemSetting = null;
      this.itemScrollRowOffset = 0;
      this.overlaySearchQuery = "";
      this.overlaySearchFocused = false;
   }

   private OverlayBounds getItemOverlayBounds() {
      int sw = Gamble.mc.getWindow().getWidth();
      int sh = Gamble.mc.getWindow().getHeight();
      int w = 620;
      int cols = Math.max(12, (w - 30) / 30);
      int h = 390;
      int x = (sw - w) / 2;
      int y = (sh - h) / 2;
      return new OverlayBounds(x, y, w, h, cols);
   }

   private boolean isInsideItemOverlay(int mx, int my) {
      OverlayBounds b = this.getItemOverlayBounds();
      return mx >= b.x && mx <= b.x + b.w && my >= b.y && my <= b.y + b.h;
   }

   private List getOverlayFilteredItems() {
      String f = this.overlaySearchQuery == null ? "" : this.overlaySearchQuery.toLowerCase(Locale.ROOT);
      List<Item> list = new ArrayList();

      for(Object itObj : ALL_ITEMS) {
         Item it = (Item)itObj;
         Identifier id = Registries.ITEM.getId(it);
         if (id != null) {
            String path = id.getPath();
            if (f.isEmpty() || path.contains(f)) {
               list.add(it);
            }
         }
      }

      return list;
   }

   private void renderItemIconOverlay(DrawContext ctx, int mx, int my) {
      OverlayBounds b = this.getItemOverlayBounds();
      RenderUtils.renderRoundedQuad(ctx.getMatrices(), this.OVERLAY_BG, (double)b.x, (double)b.y, (double)(b.x + b.w), (double)(b.y + b.h), (double)10.0F, (double)10.0F, (double)10.0F, (double)10.0F, (double)60.0F);
      ctx.fill(b.x, b.y, b.x + b.w, b.y + 1, toMCColor(this.OVERLAY_BORDER));
      ctx.fill(b.x, b.y + b.h - 1, b.x + b.w, b.y + b.h, toMCColor(this.OVERLAY_BORDER));
      ctx.fill(b.x, b.y, b.x + 1, b.y + b.h, toMCColor(this.OVERLAY_BORDER));
      ctx.fill(b.x + b.w - 1, b.y, b.x + b.w, b.y + b.h, toMCColor(this.OVERLAY_BORDER));
      TextRenderer.drawString("SELECT ITEM (ESC / CLICK OUTSIDE TO CLOSE)", ctx, b.x + 10, b.y + 10, toMCColor(this.getAccent()));
      int sx = b.x + 12;
      int sy = b.y + 30;
      int sw = b.w - 24;
      int sh = 26;
      Color sbg = this.overlaySearchFocused ? this.SEARCH_BG_FOCUSED : this.SEARCH_BG;
      RenderUtils.renderRoundedQuad(ctx.getMatrices(), sbg, (double)sx, (double)sy, (double)(sx + sw), (double)(sy + sh), (double)6.0F, (double)6.0F, (double)6.0F, (double)6.0F, (double)40.0F);
      String sTxt = this.overlaySearchQuery.isEmpty() ? "SEARCH ITEM..." : this.overlaySearchQuery;
      Color sCol = this.overlaySearchQuery.isEmpty() ? new Color(130, 130, 130, 255) : this.TEXT_COLOR;
      Objects.requireNonNull(MinecraftClient.getInstance().textRenderer);
      int fontHeight = 9;
      int centeredTextY = sy + (sh - fontHeight) / 2;
      TextRenderer.drawString(sTxt, ctx, sx + 8, centeredTextY, toMCColor(sCol));
      if (this.overlaySearchFocused && System.currentTimeMillis() / 500L % 2L == 0L) {
         int cx = sx + 8 + TextRenderer.getWidth(this.overlaySearchQuery);
         ctx.fill(cx, centeredTextY - 1, cx + 1, centeredTextY + fontHeight, toMCColor(this.TEXT_COLOR));
      }

      int gridX = b.x + 10;
      int gridY = b.y + 66;
      List<Item> list = this.getOverlayFilteredItems();
      int cols = b.columns;
      int totalRows = (int)Math.ceil((double)list.size() / (double)cols);
      int maxOffset = Math.max(0, totalRows - 10);
      if (this.itemScrollRowOffset > maxOffset) {
         this.itemScrollRowOffset = maxOffset;
      }

      if (this.itemScrollRowOffset < 0) {
         this.itemScrollRowOffset = 0;
      }

      int startRow = this.itemScrollRowOffset;
      int endRow = Math.min(startRow + 10, totalRows);

      for(int row = startRow; row < endRow; ++row) {
         for(int col = 0; col < cols; ++col) {
            int index = row * cols + col;
            if (index >= list.size()) {
               break;
            }

            int x = gridX + col * 30;
            int y = gridY + (row - startRow) * 30;
            int ex = x + 30 - 4;
            int ey = y + 30 - 4;
            boolean hovered = mx >= x && mx < ex && my >= y && my < ey;
            Item item = (Item)list.get(index);
            boolean selected = this.activeItemSetting != null && this.activeItemSetting.getItem() == item;
            Color bg = hovered ? this.OVERLAY_HOVER : new Color(255, 255, 255, 18);
            if (selected) {
               bg = this.OVERLAY_SELECTED;
            }

            RenderUtils.renderRoundedQuad(ctx.getMatrices(), bg, (double)x, (double)y, (double)ex, (double)ey, (double)5.0F, (double)5.0F, (double)5.0F, (double)5.0F, (double)16.0F);
            ItemStack stack = new ItemStack(item);

            try {
               ctx.drawItem(stack, x + 15 - 8, y + 15 - 8);
               ctx.drawItemInSlot(this.textRenderer, stack, x + 15 - 8, y + 15 - 8);
            } catch (Throwable var35) {
            }
         }
      }

      if (maxOffset > 0) {
         int barX = b.x + b.w - 14;
         int barH = 300;
         ctx.fill(barX, gridY, barX + 6, gridY + barH, toMCColor(new Color(40, 40, 46, 180)));
         double ratio = (double)this.itemScrollRowOffset / (double)maxOffset;
         int knobH = Math.max(18, (int)((double)barH * ((double)10.0F / (double)totalRows)));
         int knobY = gridY + (int)((double)(barH - knobH) * ratio);
         ctx.fill(barX + 1, knobY, barX + 5, knobY + knobH, toMCColor(this.getAccent()));
      }

   }

   private boolean handleItemIconOverlayClick(int mx, int my, int button) {
      if (this.activeItemSetting != null && button == 0) {
         OverlayBounds b = this.getItemOverlayBounds();
         int gridX = b.x + 10;
         int gridY = b.y + 66;
         List<Item> list = this.getOverlayFilteredItems();
         int cols = b.columns;
         int totalRows = (int)Math.ceil((double)list.size() / (double)cols);
         int startRow = this.itemScrollRowOffset;
         int endRow = Math.min(startRow + 10, totalRows);

         for(int row = startRow; row < endRow; ++row) {
            for(int col = 0; col < cols; ++col) {
               int index = row * cols + col;
               if (index >= list.size()) {
                  break;
               }

               int x = gridX + col * 30;
               int y = gridY + (row - startRow) * 30;
               int ex = x + 30 - 4;
               int ey = y + 30 - 4;
               if (mx >= x && mx < ex && my >= y && my < ey) {
                  this.activeItemSetting.setItem((Item)list.get(index));
                  this.closeItemOverlay();
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean handleOverlaySearchClick(int mx, int my) {
      if (!this.selectingItem) {
         return false;
      } else {
         OverlayBounds b = this.getItemOverlayBounds();
         int sx = b.x + 12;
         int sy = b.y + 30;
         int sw = b.w - 24;
         int sh = 26;
         boolean inside = mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sh;
         if (inside) {
            this.overlaySearchFocused = true;
            return true;
         } else if (this.overlaySearchFocused && this.isInsideItemOverlay(mx, my)) {
            this.overlaySearchFocused = false;
            return true;
         } else {
            return false;
         }
      }
   }

   public boolean shouldPause() {
      return false;
   }

   public void close() {
      if (this.closing) {
         super.close();
      } else {
         this.closing = true;
         this.disablePhantomSafely();
         super.close();
         this.closing = false;
      }
   }

   public void removed() {
      this.onGuiClose();
      super.removed();
   }

   public void onGuiClose() {
      this.currentColor = null;
      this.searchFocused = false;
      this.draggingSlider = false;
      this.draggingSliderSetting = null;
      this.listeningBind = null;
      this.closeItemOverlay();
      this.openProgress = 0.0F;
      this.settingsScrollOffset = 0;
      this.activeStringSetting = null;
      this.activeColorSetting = null;
      this.draggingColorComponent = false;
      this.colorComponentIndex = -1;
   }

   private void disablePhantomSafely() {
      try {
         Object mm = Gamble.INSTANCE.getModuleManager();

         try {
            Method m = mm.getClass().getMethod("getModuleByClass", Class.class);
            Object mod = m.invoke(mm, Phantom.class);
            if (mod instanceof Module p) {
               if (p.isEnabled()) {
                  p.setEnabled(false);
               }

               return;
            }
         } catch (NoSuchMethodException var6) {
         }

         List<Module> clientMods = Gamble.INSTANCE.getModuleManager().a(Category.CLIENT);
         if (clientMods != null) {
            for(Module m : clientMods) {
               String name = m.getName().toString();
               if (m instanceof Phantom || name.equalsIgnoreCase("Phantom++") || name.equalsIgnoreCase("Phantom+") || name.equalsIgnoreCase("Phantom")) {
                  if (m.isEnabled()) {
                     m.setEnabled(false);
                  }

                  return;
               }
            }
         }
      } catch (Throwable var7) {
      }

   }

   static {
      try {
         for(Item item : Registries.ITEM) {
            ALL_ITEMS.add(item);
         }
      } catch (Throwable var2) {
      }

   }

   private static final class SettingLayout {
      final Setting setting;
      final int relativeY;
      final int height;

      SettingLayout(Setting s, int y, int h) {
         this.setting = s;
         this.relativeY = y;
         this.height = h;
      }
   }

   private static final class OverlayBounds {
      int x;
      int y;
      int w;
      int h;
      int columns;

      OverlayBounds(int x, int y, int w, int h, int c) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.columns = c;
      }
   }
}
