package dev.gambleclient.module.modules.render;

import dev.gambleclient.Gamble;
import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.Render2DEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.ModeSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.RenderUtils;
import dev.gambleclient.utils.TextRenderer;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public final class HUD extends Module {
   private static final CharSequence watermarkText = EncryptedString.of("Phantom Cracked By Mommy Starry");
   private static final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
   private final BooleanSetting showWatermark = new BooleanSetting(EncryptedString.of("Watermark"), true);
   private final BooleanSetting showInfo = new BooleanSetting(EncryptedString.of("Info"), true);
   private final BooleanSetting showModules = new BooleanSetting("Modules", true);
   private final BooleanSetting showTime = new BooleanSetting("Time", true);
   private final BooleanSetting showCoordinates = new BooleanSetting("Coordinates", true);
   private final ModeSetting theme;
   private final ModeSetting moduleSortingMode;
   private final BooleanSetting shadow;
   private final BooleanSetting rainbow;

   public HUD() {
      super(EncryptedString.of("HUD"), EncryptedString.of("Clean centered HUD"), -1, Category.RENDER);
      this.theme = new ModeSetting("Theme", HUD.HUDTheme.DARK, HUDTheme.class);
      this.moduleSortingMode = new ModeSetting("Sort Mode", HUD.ModuleListSorting.LENGTH, ModuleListSorting.class);
      this.shadow = new BooleanSetting("Text Shadow", true);
      this.rainbow = new BooleanSetting("Rainbow", false);
      this.addSettings(new Setting[]{this.showWatermark, this.showInfo, this.showModules, this.showTime, this.showCoordinates, this.theme, this.moduleSortingMode, this.shadow, this.rainbow});
   }

   @EventListener
   public void onRender2D(Render2DEvent event) {
      if (this.mc.currentScreen != Gamble.INSTANCE.GUI) {
         DrawContext ctx = event.context;
         int width = this.mc.getWindow().getWidth();
         int height = this.mc.getWindow().getHeight();
         RenderUtils.unscaledProjection();
         HUDTheme currentTheme = (HUDTheme)this.theme.getValue();
         if (this.showWatermark.getValue()) {
            this.renderCenteredWatermark(ctx, width, currentTheme);
         }

         if (this.showInfo.getValue() && this.mc.player != null) {
            this.renderTopLeftInfo(ctx, currentTheme);
         }

         if (this.showCoordinates.getValue() && this.mc.player != null) {
            this.renderBottomLeftCoords(ctx, height, currentTheme);
         }

         if (this.showModules.getValue()) {
            this.renderRightModules(ctx, width, currentTheme);
         }

         RenderUtils.scaledProjection();
      }

   }

   private void renderCenteredWatermark(DrawContext ctx, int width, HUDTheme theme) {
      String watermark = watermarkText.toString();
      String time = timeFormatter.format(new Date());
      String combinedText = watermark + " | " + time;
      int textWidth = TextRenderer.getWidth(combinedText);
      int padding = 12;
      int bgWidth = textWidth + padding * 2;
      int bgX = width / 2 - bgWidth / 2;
      int y = 15;
      RenderUtils.renderRoundedQuad(ctx.getMatrices(), theme.background, (double)bgX, (double)(y - 6), (double)(bgX + bgWidth), (double)(y + 16), (double)4.0F, (double)4.0F);
      RenderUtils.renderRoundedQuad(ctx.getMatrices(), this.getAccentColor(theme), (double)bgX, (double)(y + 14), (double)(bgX + bgWidth), (double)(y + 16), (double)2.0F, (double)2.0F);
      Color textColor = this.rainbow.getValue() ? this.getRainbowColor(0) : theme.text;
      this.drawCenteredText(ctx, combinedText, bgX, y - 6, bgWidth, 22, textColor);
   }

   private void renderTopLeftInfo(DrawContext ctx, HUDTheme theme) {
      int x = 15;
      int y = 15;
      String fps = "FPS " + this.mc.getCurrentFps();
      int textWidth = TextRenderer.getWidth(fps);
      int boxWidth = textWidth + 16;
      int boxHeight = 16;
      RenderUtils.renderRoundedQuad(ctx.getMatrices(), theme.background, (double)(x - 8), (double)(y - 6), (double)(x - 8 + boxWidth), (double)(y - 6 + boxHeight), (double)4.0F, (double)4.0F);
      Color fpsColor = this.rainbow.getValue() ? this.getRainbowColor(2) : theme.primary;
      this.drawCenteredText(ctx, fps, x - 8, y - 6, boxWidth, boxHeight, fpsColor);
   }

   private void renderBottomLeftCoords(DrawContext ctx, int height, HUDTheme theme) {
      if (this.mc.player != null) {
         int x = 15;
         int y = height - 35;
         String coords = String.format("XYZ %.0f %.0f %.0f", this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
         String dimension = this.getDimensionCoords();
         if (!dimension.isEmpty()) {
            coords = coords + " " + dimension;
         }

         int textWidth = TextRenderer.getWidth(coords);
         int padding = 10;
         int boxWidth = textWidth + padding * 2;
         RenderUtils.renderRoundedQuad(ctx.getMatrices(), theme.background, (double)(x - padding), (double)(y - 6), (double)(x - padding + boxWidth), (double)(y + 16), (double)4.0F, (double)4.0F);
         Color coordColor = this.rainbow.getValue() ? this.getRainbowColor(4) : theme.accent;
         this.drawCenteredText(ctx, coords, x - padding, y - 6, boxWidth, 22, coordColor);
      }
   }

   private void renderRightModules(DrawContext ctx, int width, HUDTheme theme) {
      List<Module> modules = this.getSortedModules();
      if (!modules.isEmpty()) {
         int y = 50;
         int rightMargin = 15;

         for(int i = 0; i < modules.size(); ++i) {
            Module module = (Module)modules.get(i);
            String name = module.getName().toString();
            int textWidth = TextRenderer.getWidth(name);
            int padding = 8;
            int boxWidth = textWidth + padding * 2;
            int x = width - boxWidth - rightMargin;
            RenderUtils.renderRoundedQuad(ctx.getMatrices(), theme.background, (double)x, (double)(y - 4), (double)(x + boxWidth), (double)(y + 14), (double)3.0F, (double)3.0F);
            RenderUtils.renderRoundedQuad(ctx.getMatrices(), this.getModuleColor(theme, i), (double)(x + boxWidth - 3), (double)(y - 4), (double)(x + boxWidth), (double)(y + 14), (double)0.0F, (double)2.0F);
            Color moduleColor = this.rainbow.getValue() ? this.getRainbowColor(i + 5) : theme.text;
            this.drawCenteredText(ctx, name, x, y - 4, boxWidth, 18, moduleColor);
            y += 22;
         }

      }
   }

   private void drawCenteredText(DrawContext ctx, String text, int boxX, int boxY, int boxWidth, int boxHeight, Color color) {
      int textWidth = TextRenderer.getWidth(text);
      int textHeight = 10;
      int textX = boxX + (boxWidth - textWidth) / 2;
      int textY = boxY + (boxHeight - textHeight) / 2 - 1;
      if (this.shadow.getValue()) {
         TextRenderer.drawString(text, ctx, textX + 1, textY + 1, (new Color(0, 0, 0, 100)).getRGB());
      }

      TextRenderer.drawString(text, ctx, textX, textY, color.getRGB());
   }

   private String getDimensionCoords() {
      if (this.mc.world == null) {
         return "";
      } else {
         String dimension = this.mc.world.getRegistryKey().getValue().getPath();
         if (dimension.contains("nether")) {
            return String.format("[%.0f %.0f]", this.mc.player.getX() * (double)8.0F, this.mc.player.getZ() * (double)8.0F);
         } else {
            return dimension.contains("overworld") ? String.format("[%.0f %.0f]", this.mc.player.getX() / (double)8.0F, this.mc.player.getZ() / (double)8.0F) : "";
         }
      }
   }

   private Color getAccentColor(HUDTheme theme) {
      return this.rainbow.getValue() ? this.getRainbowColor(10) : theme.accent;
   }

   private Color getModuleColor(HUDTheme theme, int index) {
      if (this.rainbow.getValue()) {
         return this.getRainbowColor(index + 20);
      } else {
         Color var10000;
         switch (index % 3) {
            case 0 -> var10000 = theme.primary;
            case 1 -> var10000 = theme.secondary;
            default -> var10000 = theme.accent;
         }

         return var10000;
      }
   }

   private Color getRainbowColor(int offset) {
      long time = System.currentTimeMillis();
      float hue = (float)((time + (long)(offset * 100)) % 3600L) / 3600.0F;
      return Color.getHSBColor(hue, 0.8F, 1.0F);
   }

   private List getSortedModules() {
      List<Module> modules = Gamble.INSTANCE.getModuleManager().b();
      ModuleListSorting sorting = (ModuleListSorting)this.moduleSortingMode.getValue();
      List var10000;
      switch (sorting.ordinal()) {
         case 0 -> var10000 = modules.stream().sorted((a, b) -> Integer.compare(TextRenderer.getWidth(b.getName()), TextRenderer.getWidth(a.getName()))).toList();
         case 1 -> var10000 = modules.stream().sorted(Comparator.comparing((module) -> module.getName().toString())).toList();
         case 2 -> var10000 = modules.stream().sorted(Comparator.comparing(Module::getCategory).thenComparing((module) -> module.getName().toString())).toList();
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   static enum ModuleListSorting {
      LENGTH("Length"),
      ALPHABETICAL("Alphabetical"),
      CATEGORY("Category");

      private final String name;

      private ModuleListSorting(String name) {
         this.name = name;
      }

      public String toString() {
         return this.name;
      }

      // $FF: synthetic method
      private static ModuleListSorting[] $values() {
         return new ModuleListSorting[]{LENGTH, ALPHABETICAL, CATEGORY};
      }
   }

   static enum HUDTheme {
      DARK("Dark", new Color(25, 25, 28, 180), new Color(255, 255, 255, 220), new Color(100, 149, 237), new Color(255, 107, 107), new Color(152, 195, 121)),
      PURPLE("Purple", new Color(30, 20, 40, 180), new Color(255, 255, 255, 220), new Color(147, 112, 219), new Color(186, 85, 211), new Color(138, 43, 226)),
      BLUE("Blue", new Color(15, 25, 35, 180), new Color(255, 255, 255, 220), new Color(64, 224, 255), new Color(30, 144, 255), new Color(0, 191, 255)),
      GREEN("Green", new Color(20, 30, 20, 180), new Color(255, 255, 255, 220), new Color(50, 205, 50), new Color(124, 252, 0), new Color(0, 255, 127));

      public final String name;
      public final Color background;
      public final Color text;
      public final Color primary;
      public final Color secondary;
      public final Color accent;

      private HUDTheme(String name, Color bg, Color text, Color primary, Color secondary, Color accent) {
         this.name = name;
         this.background = bg;
         this.text = text;
         this.primary = primary;
         this.secondary = secondary;
         this.accent = accent;
      }

      public String toString() {
         return this.name;
      }

      // $FF: synthetic method
      private static HUDTheme[] $values() {
         return new HUDTheme[]{DARK, PURPLE, BLUE, GREEN};
      }
   }
}
