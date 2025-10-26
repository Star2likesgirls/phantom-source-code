package dev.gambleclient.module;

import dev.gambleclient.Gamble;
import dev.gambleclient.manager.EventManager;
import dev.gambleclient.module.setting.Setting;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.MinecraftClient;

public abstract class Module implements Serializable {
    private final List settings = new ArrayList();
    protected final EventManager EVENT_BUS;
    protected MinecraftClient mc;
    private CharSequence name;
    private CharSequence description;
    private boolean enabled;
    private int keybind;
    private Category category;
    private final boolean i;

    public Module(CharSequence name, CharSequence description, int keybind, Category category) {
        this.EVENT_BUS = Gamble.INSTANCE.getEventBus();
        this.mc = MinecraftClient.getInstance();
        this.i = false;
        this.name = name;
        this.description = description;
        this.enabled = false;
        this.keybind = keybind;
        this.category = category;
    }

    public void toggle() {
        this.enabled = !this.enabled;
        if (this.enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }

    public CharSequence getName() {
        return this.name;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public CharSequence getDescription() {
        return this.description;
    }

    public int getKeybind() {
        return this.keybind;
    }

    public Category getCategory() {
        return this.category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setName(CharSequence name) {
        this.name = name;
    }

    public void setDescription(CharSequence description) {
        this.description = description;
    }

    public void setKeybind(int keybind) {
        this.keybind = keybind;
    }

    public List getSettings() {
        return this.settings;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void addSetting(Setting setting) {
        this.settings.add(setting);
    }

    public void addSettings(Setting... a) {
        this.settings.addAll(Arrays.asList(a));
    }

    public void toggle(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                this.onEnable();
            } else {
                this.onDisable();
            }
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}