package dev.gambleclient.module.modules.render;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.PacketSendEvent;
import dev.gambleclient.event.events.Render2DEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.ColorUtil;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.MathUtil;
import dev.gambleclient.utils.RenderUtils;
import dev.gambleclient.utils.TextRenderer;
import java.awt.Color;
import net.minecraft.util.Hand;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.gui.PlayerSkinDrawer;

public final class TargetHUD extends Module {
   private final NumberSetting xPosition = new NumberSetting(EncryptedString.of("X"), (double)0.0F, (double)1920.0F, (double)500.0F, (double)1.0F);
   private final NumberSetting yPosition = new NumberSetting(EncryptedString.of("Y"), (double)0.0F, (double)1080.0F, (double)500.0F, (double)1.0F);
   private final BooleanSetting timeoutEnabled = (new BooleanSetting(EncryptedString.of("Timeout"), true)).setDescription(EncryptedString.of("Target hud will disappear after 10 seconds"));
   private final NumberSetting fadeSpeed = (new NumberSetting(EncryptedString.of("Fade Speed"), (double)5.0F, (double)30.0F, (double)15.0F, (double)1.0F)).getValue(EncryptedString.of("Speed of animations"));
   private final Color primaryColor = new Color(255, 50, 100);
   private final Color backgroundColor = new Color(0, 0, 0, 175);
   private long lastAttackTime = 0L;
   public static float fadeProgress = 1.0F;
   private float currentHealth = 0.0F;
   private TargetHUDHandler hudHandler;

   public TargetHUD() {
      super(EncryptedString.of("Target HUD"), EncryptedString.of("Displays detailed information about your target with style"), -1, Category.RENDER);
      this.addSettings(new Setting[]{this.xPosition, this.yPosition, this.timeoutEnabled, this.fadeSpeed});
   }

   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void a(Render2DEvent render2DEvent) {
      DrawContext a = render2DEvent.context;
      int f = this.xPosition.getIntValue();
      int f2 = this.yPosition.getIntValue();
      float g = this.fadeSpeed.getFloatValue();
      Color h = this.primaryColor;
      Color i = this.backgroundColor;
      RenderUtils.unscaledProjection();
      boolean b = this.mc.player.getAttacking() != null && this.mc.player.getAttacking() instanceof PlayerEntity && this.mc.player.getAttacking().isAlive();
      boolean b2 = !this.timeoutEnabled.getValue() || System.currentTimeMillis() - this.lastAttackTime <= 10000L;
      float n;
      if (b && b2) {
         n = 0.0F;
      } else {
         n = 1.0F;
      }

      fadeProgress = RenderUtils.fast(fadeProgress, n, g);
      if (fadeProgress < 0.99F && b) {
         LivingEntity getAttacking = this.mc.player.getAttacking();
         PlayerListEntry playerListEntry = this.mc.getNetworkHandler().getPlayerListEntry(getAttacking.getUuid());
         MatrixStack matrices = a.getMatrices();
         matrices.push();
         float n2 = 1.0F - fadeProgress;
         float n3 = 0.8F + 0.2F * n2;
         matrices.translate((float)f, (float)f2, 0.0F);
         matrices.scale(n3, n3, 1.0F);
         matrices.translate((float)(-f), (float)(-f2), 0.0F);
         this.currentHealth = RenderUtils.fast(this.currentHealth, getAttacking.getHealth() + getAttacking.getAbsorptionAmount(), g * 0.5F);
         this.a(a, f, f2, (PlayerEntity)getAttacking, playerListEntry, n2, h, i);
         matrices.pop();
      }

      RenderUtils.scaledProjection();
   }

   private void a(DrawContext drawContext, int n, int n2, PlayerEntity playerEntity, PlayerListEntry playerListEntry, float n3, Color color, Color color2) {
      MatrixStack matrices = drawContext.getMatrices();
      RenderUtils.renderRoundedQuad(matrices, new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(50.0F * n3)), (double)(n - 5), (double)(n2 - 5), (double)(n + 300 + 5), (double)(n2 + 180 + 5), (double)15.0F, (double)15.0F, (double)15.0F, (double)15.0F, (double)30.0F);
      RenderUtils.renderRoundedQuad(matrices, new Color(color2.getRed(), color2.getGreen(), color2.getBlue(), (int)((float)color2.getAlpha() * n3)), (double)n, (double)n2, (double)(n + 300), (double)(n2 + 180), (double)10.0F, (double)10.0F, (double)10.0F, (double)10.0F, (double)20.0F);
      Color color3 = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)((float)color.getAlpha() * n3));
      RenderUtils.renderRoundedQuad(matrices, color3, (double)(n + 20), (double)n2, (double)(n + 300 - 20), (double)(n2 + 3), (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F, (double)10.0F);
      RenderUtils.renderRoundedQuad(matrices, color3, (double)(n + 20), (double)(n2 + 180 - 3), (double)(n + 300 - 20), (double)(n2 + 180), (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F, (double)10.0F);
      if (playerListEntry != null) {
         RenderUtils.renderRoundedQuad(matrices, new Color(30, 30, 30, (int)(200.0F * n3)), (double)(n + 15), (double)(n2 + 15), (double)(n + 85), (double)(n2 + 85), (double)5.0F, (double)5.0F, (double)5.0F, (double)5.0F, (double)10.0F);
         PlayerSkinDrawer.draw(drawContext, playerListEntry.getSkinTextures().texture(), n + 25, n2 + 25, 50);
         TextRenderer.drawString(playerEntity.getName().getString(), drawContext, n + 100, n2 + 25, ColorUtil.a((int)((float)(System.currentTimeMillis() % 1000L) / 1000.0F), 1).getRGB());
         TextRenderer.drawString(MathUtil.roundToNearest((double)playerEntity.distanceTo(this.mc.player), (double)1.0F) + " blocks away", drawContext, n + 100, n2 + 45, Color.WHITE.getRGB());
         RenderUtils.renderRoundedQuad(matrices, new Color(60, 60, 60, (int)(200.0F * n3)), (double)(n + 15), (double)(n2 + 95), (double)(n + 300 - 15), (double)(n2 + 110), (double)5.0F, (double)5.0F, (double)5.0F, (double)5.0F, (double)10.0F);
         float b = this.currentHealth / playerEntity.getMaxHealth();
         float n4 = 270.0F * Math.min(1.0F, b);
         RenderUtils.renderRoundedQuad(matrices, this.a(b * (float)((double)0.8F + (double)0.2F * Math.sin((double)System.currentTimeMillis() / (double)300.0F)), n3), (double)(n + 15), (double)(n2 + 95), (double)(n + 15 + (int)n4), (double)(n2 + 110), (double)5.0F, (double)5.0F, (double)5.0F, (double)5.0F, (double)10.0F);
         int var10000 = Math.round(this.currentHealth);
         String s = var10000 + "/" + Math.round(playerEntity.getMaxHealth()) + " HP";
         TextRenderer.drawString(s, drawContext, n + 15 + (int)n4 / 2 - TextRenderer.getWidth(s) / 2, n2 + 95, Color.WHITE.getRGB());
         int n5 = n2 + 120;
         this.a(drawContext, n + 15, n5, 80, 45, "PING", playerListEntry.getLatency() + "ms", this.a(playerListEntry.getLatency(), n3), color3, n3);
         String s2;
         if (playerListEntry != null) {
            s2 = "PLAYER";
         } else {
            s2 = "BOT";
         }

         Color color4;
         if (playerListEntry != null) {
            color4 = new Color(100, 255, 100, (int)(255.0F * n3));
         } else {
            color4 = new Color(255, 100, 100, (int)(255.0F * n3));
         }

         this.a(drawContext, n + 100 + 5, n5, 80, 45, "TYPE", s2, color4, color3, n3);
         if (playerEntity.hurtTime > 0) {
            this.a(drawContext, n + 200 + 5, n5, 80, 45, "HURT", "" + playerEntity.hurtTime, this.b(playerEntity.hurtTime, n3), color3, n3);
         } else {
            this.a(drawContext, n + 200 + 5, n5, 80, 45, "HURT", "No", new Color(150, 150, 150, (int)(255.0F * n3)), color3, n3);
         }
      } else {
         TextRenderer.drawString("BOT DETECTED", drawContext, n + 150 - TextRenderer.getWidth("BOT DETECTED") / 2, n2 + 90, (new Color(255, 50, 50)).getRGB());
      }

   }

   private void a(DrawContext drawContext, int n, int n2, int n3, int n4, String s, String s2, Color color, Color color2, float n5) {
      MatrixStack matrices = drawContext.getMatrices();
      RenderUtils.renderRoundedQuad(matrices, color2, (double)n, (double)n2, (double)(n + n3), (double)(n2 + 3), (double)3.0F, (double)3.0F, (double)0.0F, (double)0.0F, (double)6.0F);
      RenderUtils.renderRoundedQuad(matrices, new Color(30, 30, 30, (int)(200.0F * n5)), (double)n, (double)(n2 + 3), (double)(n + n3), (double)(n2 + n4), (double)0.0F, (double)0.0F, (double)3.0F, (double)3.0F, (double)6.0F);
      TextRenderer.drawString(s, drawContext, n + n3 / 2 - TextRenderer.getWidth(s) / 2, n2 + 5, (new Color(200, 200, 200, (int)(255.0F * n5))).getRGB());
      TextRenderer.drawString(s2, drawContext, n + n3 / 2 - TextRenderer.getWidth(s2) / 2, n2 + n4 - 17, color.getRGB());
   }

   private Color a(float n, float n2) {
      Color color;
      if (n > 0.75F) {
         color = ColorUtil.a(new Color(100, 255, 100), new Color(255, 255, 100), (1.0F - n) * 4.0F);
      } else if (n > 0.25F) {
         color = ColorUtil.a(new Color(255, 255, 100), new Color(255, 100, 100), (0.75F - n) * 2.0F);
      } else {
         float n3;
         if (n < 0.1F) {
            n3 = (float)((double)0.7F + (double)0.3F * Math.sin((double)System.currentTimeMillis() / (double)200.0F));
         } else {
            n3 = 1.0F;
         }

         color = new Color((int)(255.0F * n3), (int)(100.0F * n3), (int)(100.0F * n3));
      }

      return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)((float)color.getAlpha() * n2));
   }

   private Color a(int n, float n2) {
      Color color;
      if (n < 50) {
         color = new Color(100, 255, 100);
      } else if (n < 100) {
         color = ColorUtil.a(new Color(100, 255, 100), new Color(255, 255, 100), (float)(n - 50) / 50.0F);
      } else if (n < 200) {
         color = ColorUtil.a(new Color(255, 255, 100), new Color(255, 150, 50), (float)(n - 100) / 100.0F);
      } else {
         color = ColorUtil.a(new Color(255, 150, 50), new Color(255, 80, 80), Math.min(1.0F, (float)(n - 200) / 300.0F));
      }

      return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)((float)color.getAlpha() * n2));
   }

   private Color b(int n, float n2) {
      double n3 = (double)0.7F + (double)0.3F * Math.sin((double)System.currentTimeMillis() / (double)150.0F);
      float min = Math.min(1.0F, (float)n / 10.0F);
      Color color = new Color(255, (int)(50.0F + 100.0F * (1.0F - min)), (int)(50.0F + 100.0F * (1.0F - min)));
      return new Color((int)((float)color.getRed() * (float)n3), (int)((float)color.getGreen() * (float)n3), (int)((float)color.getBlue() * (float)n3), (int)(255.0F * n2));
   }

   @EventListener
   public void a(PacketSendEvent packetEvent) {
      Packet var3 = packetEvent.packet;
      if (var3 instanceof PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket) {
         if (this.hudHandler == null) {
            this.hudHandler = new TargetHUDHandler(this);
         }

         if (this.hudHandler.isAttackPacket(playerInteractEntityC2SPacket)) {
            this.lastAttackTime = System.currentTimeMillis();
         }
      }

   }

   public static class TargetHUDHandler {
      public static final MinecraftClient MC = MinecraftClient.getInstance();
      final TargetHUD this$0;

      TargetHUDHandler(TargetHUD this$0) {
         this.this$0 = this$0;
      }

      public boolean isAttackPacket(PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket) {
         String string;
         try {
            string = playerInteractEntityC2SPacket.toString();
            if (string.contains("ATTACK")) {
               return true;
            }
         } catch (Exception var5) {
            return MC.player != null && MC.player.getAttacking() != null && MC.player.getAttacking() instanceof PlayerEntity;
         }

         try {
            if (MC.player == null || MC.player.getAttacking() == null || !(MC.player.getAttacking() instanceof PlayerEntity)) {
               return false;
            }

            boolean contains = string.contains(Hand.MAIN_HAND.toString());
            boolean contains2 = string.contains("INTERACT_AT");
            if (contains && contains2) {
               return true;
            }
         } catch (Exception var7) {
            return MC.player != null && MC.player.getAttacking() != null && MC.player.getAttacking() instanceof PlayerEntity;
         }

         try {
            return MC.player.handSwinging && MC.player.getAttacking() != null;
         } catch (Exception var6) {
            return MC.player != null && MC.player.getAttacking() != null && MC.player.getAttacking() instanceof PlayerEntity;
         }
      }
   }
}
