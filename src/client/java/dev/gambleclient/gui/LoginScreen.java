package dev.gambleclient.gui;

import dev.gambleclient.auth.AuthenticationService;
import dev.gambleclient.auth.HeartbeatService;
import dev.gambleclient.auth.UserData;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.util.Util;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;

public class LoginScreen extends Screen {
   private String statusMessage = "Enter literally anything lol this shi cracked";
   private int statusColor = -5592406;
   private boolean isAuthenticating = false;
   private TextFieldWidget licenseField;
   private ButtonWidget loginButton;

   public LoginScreen() {
      super(Text.literal("Phantom Crack By Sakura"));
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.renderBackground(context, mouseX, mouseY, delta);
      int panelWidth = 280;
      int panelHeight = 160;
      int panelX = this.width / 2 - panelWidth / 2;
      int panelY = this.height / 2 - panelHeight / 2 - 10;
      context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, -1073741824);
      super.render(context, mouseX, mouseY, delta);
      context.drawCenteredTextWithShadow(this.textRenderer, "Phantom Authentication - Cracked by Sakura", this.width / 2, panelY + 10, -1);
      context.drawCenteredTextWithShadow(this.textRenderer, this.statusMessage, this.width / 2, panelY + 28, this.statusColor);
      context.drawCenteredTextWithShadow(this.textRenderer, "no need to purchase a key mommy starry did the job :3", this.width / 2, panelY + panelHeight - 12, -1);
   }

   protected void init() {
      super.init();
      int fieldWidth = 200;
      int fieldHeight = 20;
      int centerX = this.width / 2 - fieldWidth / 2;
      int startY = this.height / 2 - fieldHeight - 10;
      this.licenseField = new TextFieldWidget(this.textRenderer, centerX, startY, fieldWidth, fieldHeight, Text.literal(""));
      this.licenseField.setMaxLength(64);
      this.licenseField.setText("");
      this.licenseField.setPlaceholder(Text.of("starry owns phantom now :3 .gg/sakuraclient"));
      this.addDrawableChild(this.licenseField);
      this.loginButton = ButtonWidget.builder(Text.literal("Login"), (button) -> this.performLogin()).dimensions(centerX, startY + 30, fieldWidth, fieldHeight).build();
      this.addDrawableChild(this.loginButton);
      this.addDrawableChild(ButtonWidget.builder(Text.literal("Buy License"), (b) -> Util.getOperatingSystem().open("https://phantomclient.com/")).dimensions(centerX, startY + 60, fieldWidth, fieldHeight).build());
   }

   private void performLogin() {
      if (!this.isAuthenticating) {
         String licenseKey = this.licenseField.getText().trim();
         if (licenseKey.isEmpty()) {
            this.updateStatus("Enter license key", -48060);
         } else {
            this.isAuthenticating = true;
            this.updateStatus("Authenticating...", -256);
            this.loginButton.active = false;
            CompletableFuture.runAsync(() -> {
               try {
                  String hwid = getHWID();
                  AuthenticationService service = new AuthenticationService();
                  CompletableFuture<UserData> loginFuture = service.loginWithLicense(licenseKey, hwid);
                  UserData user = (UserData)loginFuture.get(15L, TimeUnit.SECONDS);
                  this.client.execute(() -> {
                     if (user != null) {
                        this.updateStatus("Authentication successful!", -16711936);
                        HeartbeatService.start(licenseKey, getHWID());
                        this.client.setScreen((Screen)null);
                     } else {
                        this.updateStatus("Authentication failed", -48060);
                        System.err.println("[Login] Login failed - invalid response");
                     }

                     this.isAuthenticating = false;
                     this.loginButton.active = true;
                  });
               } catch (Exception ex) {
                  System.err.println("[Login] Login error: " + ex.getMessage());
                  ex.printStackTrace();
                  this.client.execute(() -> {
                     this.updateStatus("Connection failed: " + ex.getMessage(), -48060);
                     this.isAuthenticating = false;
                     this.loginButton.active = true;
                  });
               }

            });
         }
      }
   }

   private void updateStatus(String message, int color) {
      this.statusMessage = message;
      this.statusColor = color;
   }

   private static String getHWID() {
      try {
         String var10000 = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
         String info = var10000 + System.getProperty("os.arch", "") + System.getProperty("user.name", "") + System.getProperty("user.home", "");
         MessageDigest md = MessageDigest.getInstance("MD5");
         byte[] digest = md.digest(info.getBytes(StandardCharsets.UTF_8));
         StringBuilder sb = new StringBuilder();

         for(byte b : digest) {
            String hex = Integer.toHexString(255 & b);
            if (hex.length() == 1) {
               sb.append('0');
            }

            sb.append(hex);
         }

         String hwid = sb.toString();
         return hwid;
      } catch (NoSuchAlgorithmException e) {
         System.err.println("[HWID] Error generating HWID: " + e.getMessage());
         return "default-hwid";
      }
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }
}
