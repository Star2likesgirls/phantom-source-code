package dev.gambleclient;

import dev.gambleclient.auth.AuthBootstrap;
import dev.gambleclient.gui.ClickGUI;
import dev.gambleclient.manager.ConfigManager;
import dev.gambleclient.manager.EventManager;
import dev.gambleclient.module.ModuleManager;
import java.io.File;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class Gamble {
    public ConfigManager configManager;
    public ModuleManager MODULE_MANAGER;
    public EventManager EVENT_BUS;
    public static MinecraftClient mc;
    public String version;
    public static Gamble INSTANCE;
    public boolean shouldPreventClose;
    public ClickGUI GUI;
    public Screen screen;
    public long modified;
    public File jar;

    public Gamble() {
        try {
            System.out.println("[Gamble] Starting initialization...");

            // Set instance FIRST
            INSTANCE = this;
            this.version = " b1.3";
            this.screen = null;

            // Get MinecraftClient early
            mc = MinecraftClient.getInstance();
            System.out.println("[Gamble] MinecraftClient: " + (mc != null ? "OK" : "NULL"));

            // Initialize EventBus FIRST
            this.EVENT_BUS = new EventManager();
            System.out.println("[Gamble] EventBus initialized");

            // Initialize ModuleManager (this registers modules with EventBus)
            this.MODULE_MANAGER = new ModuleManager();
            System.out.println("[Gamble] ModuleManager initialized");

            // Initialize GUI
            this.GUI = new ClickGUI();
            System.out.println("[Gamble] ClickGUI initialized");

            // Initialize config manager
            this.configManager = new ConfigManager();
            System.out.println("[Gamble] ConfigManager initialized");

            // Get jar file info
            this.jar = new File(Gamble.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            this.modified = this.jar.lastModified();
            this.shouldPreventClose = false;

            // Auth bootstrap (no longer blocks)
            System.out.println("[Gamble] Running AuthBootstrap...");
            AuthBootstrap.init();
            System.out.println("[Gamble] AuthBootstrap complete");

            // Load config LAST (after everything is initialized)
            System.out.println("[Gamble] Loading config profile...");
            this.getConfigManager().loadProfile();
            System.out.println("[Gamble] Config loaded");

            System.out.println("[Gamble] Initialization complete!");
        } catch (Throwable _t) {
            System.err.println("[Gamble] FATAL ERROR during initialization:");
            _t.printStackTrace(System.err);
        }
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public ModuleManager getModuleManager() {
        return this.MODULE_MANAGER;
    }

    public EventManager getEventBus() {
        return this.EVENT_BUS;
    }

    public void resetModifiedDate() {
        this.jar.setLastModified(this.modified);
    }
}