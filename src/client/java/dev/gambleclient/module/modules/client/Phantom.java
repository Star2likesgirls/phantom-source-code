package dev.gambleclient.module.modules.client;

import dev.gambleclient.Gamble;
import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.PacketReceiveEvent;
import dev.gambleclient.gui.ClickGUI;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.ModeSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

public final class Phantom extends Module {
    public static final NumberSetting redColor = new NumberSetting(EncryptedString.of("Red"), (double)0.0F, (double)255.0F, (double)120.0F, (double)1.0F);
    public static final NumberSetting greenColor = new NumberSetting(EncryptedString.of("Green"), (double)0.0F, (double)255.0F, (double)190.0F, (double)1.0F);
    public static final NumberSetting blueColor = new NumberSetting(EncryptedString.of("Blue"), (double)0.0F, (double)255.0F, (double)255.0F, (double)1.0F);
    public static final NumberSetting windowAlpha = new NumberSetting(EncryptedString.of("Window Alpha"), (double)0.0F, (double)255.0F, (double)170.0F, (double)1.0F);
    public static final BooleanSetting enableBreathingEffect = (new BooleanSetting(EncryptedString.of("Breathing"), false)).setDescription(EncryptedString.of("Color breathing effect (only with rainbow off)"));
    public static final BooleanSetting enableRainbowEffect = (new BooleanSetting(EncryptedString.of("Rainbow"), false)).setDescription(EncryptedString.of("Enables LGBTQ mode"));
    public static final BooleanSetting renderBackground = (new BooleanSetting(EncryptedString.of("Background"), true)).setDescription(EncryptedString.of("Renders the background of the Click Gui"));
    public static final BooleanSetting useCustomFont = new BooleanSetting(EncryptedString.of("Custom Font"), true);
    private final BooleanSetting preventClose = (new BooleanSetting(EncryptedString.of("Prevent Close"), true)).setDescription(EncryptedString.of("For servers with freeze plugins that don't let you open the GUI"));
    public static final NumberSetting cornerRoundness = new NumberSetting(EncryptedString.of("Roundness"), (double)1.0F, (double)10.0F, (double)5.0F, (double)1.0F);
    public static final ModeSetting animationMode;
    public static final BooleanSetting enableMSAA;
    public boolean shouldPreventClose;

    public Phantom() {
        // Changed keybind from 344 (Right Shift) to 80 (P key)
        super(EncryptedString.of("Phantom++"), EncryptedString.of("Settings for the client"), 80, Category.CLIENT);
        this.addSettings(new Setting[]{redColor, greenColor, blueColor, windowAlpha, renderBackground, this.preventClose, cornerRoundness, animationMode, enableMSAA});
        System.out.println("[Phantom] Module initialized with keybind: 80 (P)");
    }

    public void onEnable() {
        System.out.println("[Phantom] onEnable() called");
        System.out.println("[Phantom] Current screen: " + (this.mc.currentScreen != null ? this.mc.currentScreen.getClass().getSimpleName() : "null"));
        System.out.println("[Phantom] GUI instance: " + (Gamble.INSTANCE.GUI != null ? "exists" : "null"));

        Gamble.INSTANCE.screen = this.mc.currentScreen;

        if (Gamble.INSTANCE.GUI != null) {
            System.out.println("[Phantom] Opening existing GUI");
            this.mc.setScreenAndRender(Gamble.INSTANCE.GUI);
        } else {
            System.out.println("[Phantom] No GUI instance found, creating new one");
            Gamble.INSTANCE.GUI = new ClickGUI();
            this.mc.setScreenAndRender(Gamble.INSTANCE.GUI);
        }

        if (this.mc.currentScreen instanceof InventoryScreen) {
            this.shouldPreventClose = true;
        }

        if ((new Random()).nextInt(3) == 1) {
            CompletableFuture.runAsync(() -> {
            });
        }

        super.onEnable();
        System.out.println("[Phantom] onEnable() completed");
    }

    public void onDisable() {
        System.out.println("[Phantom] onDisable() called");
        if (this.mc.currentScreen instanceof ClickGUI) {
            Gamble.INSTANCE.GUI.close();
            this.mc.setScreenAndRender(Gamble.INSTANCE.screen);
            Gamble.INSTANCE.GUI.onGuiClose();
        } else if (this.mc.currentScreen instanceof InventoryScreen) {
            this.shouldPreventClose = false;
        }

        super.onDisable();
    }

    @EventListener
    public void onPacketReceive(PacketReceiveEvent packetReceiveEvent) {
        if (this.shouldPreventClose && packetReceiveEvent.packet instanceof OpenScreenS2CPacket && this.preventClose.getValue()) {
            packetReceiveEvent.cancel();
        }

    }

    static {
        animationMode = new ModeSetting(EncryptedString.of("Animations"), Phantom.AnimationMode.NORMAL, AnimationMode.class);
        enableMSAA = (new BooleanSetting(EncryptedString.of("MSAA"), true)).setDescription(EncryptedString.of("Anti Aliasing | This can impact performance if you're using tracers but gives them a smoother look |"));
    }

    public static enum AnimationMode {
        NORMAL("Normal", 0),
        POSITIVE("Positive", 1),
        OFF("Off", 2);

        private AnimationMode(final String name, final int ordinal) {
        }

        // $FF: synthetic method
        private static AnimationMode[] $values() {
            return new AnimationMode[]{NORMAL, POSITIVE, OFF};
        }
    }
}