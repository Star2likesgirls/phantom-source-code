package dev.gambleclient.module;

import dev.gambleclient.Gamble;
import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.KeyEvent;
import dev.gambleclient.gui.ClickGUI;
import dev.gambleclient.module.modules.client.ConfigDebug;
import dev.gambleclient.module.modules.client.Phantom;
import dev.gambleclient.module.modules.client.SelfDestruct;
import dev.gambleclient.module.modules.combat.AnchorMacro;
import dev.gambleclient.module.modules.combat.AutoCrystal;
import dev.gambleclient.module.modules.combat.AutoInventoryTotem;
import dev.gambleclient.module.modules.combat.AutoTotem;
import dev.gambleclient.module.modules.combat.DoubleAnchor;
import dev.gambleclient.module.modules.combat.ElytraSwap;
import dev.gambleclient.module.modules.combat.Hitbox;
import dev.gambleclient.module.modules.combat.HoverTotem;
import dev.gambleclient.module.modules.combat.MaceSwap;
import dev.gambleclient.module.modules.combat.StaticHitboxes;
import dev.gambleclient.module.modules.donut.AntiTrap;
import dev.gambleclient.module.modules.donut.AuctionSniper;
import dev.gambleclient.module.modules.donut.AutoSell;
import dev.gambleclient.module.modules.donut.AutoSpawnerSell;
import dev.gambleclient.module.modules.donut.BStarPhantom;
import dev.gambleclient.module.modules.donut.BoneDropper;
import dev.gambleclient.module.modules.donut.ChunkFinder;
import dev.gambleclient.module.modules.donut.MemoryFinder;
import dev.gambleclient.module.modules.donut.NetheriteFinder;
import dev.gambleclient.module.modules.donut.RTPEndBaseFinder;
import dev.gambleclient.module.modules.donut.RtpBaseFinder;
import dev.gambleclient.module.modules.donut.ShulkerDropper;
import dev.gambleclient.module.modules.donut.TunnelBaseFinder;
import dev.gambleclient.module.modules.misc.AutoEat;
import dev.gambleclient.module.modules.misc.AutoFirework;
import dev.gambleclient.module.modules.misc.AutoMine;
import dev.gambleclient.module.modules.misc.AutoTPA;
import dev.gambleclient.module.modules.misc.AutoTool;
import dev.gambleclient.module.modules.misc.CordSnapper;
import dev.gambleclient.module.modules.misc.ElytraGlide;
import dev.gambleclient.module.modules.misc.FastPlace;
import dev.gambleclient.module.modules.misc.Freecam;
import dev.gambleclient.module.modules.misc.KeyPearl;
import dev.gambleclient.module.modules.misc.NameProtect;
import dev.gambleclient.module.modules.render.Animations;
import dev.gambleclient.module.modules.render.FullBright;
import dev.gambleclient.module.modules.render.HUD;
import dev.gambleclient.module.modules.render.KelpESP;
import dev.gambleclient.module.modules.render.NoRender;
import dev.gambleclient.module.modules.render.PlayerESP;
import dev.gambleclient.module.modules.render.StorageESP;
import dev.gambleclient.module.modules.render.SwingSpeed;
import dev.gambleclient.module.modules.render.TargetHUD;
import dev.gambleclient.module.setting.BindSetting;
import dev.gambleclient.utils.EncryptedString;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.client.gui.screen.ChatScreen;

public final class ModuleManager {
    private final List modules = new ArrayList();

    public ModuleManager() {
        this.a();
        this.d();
    }

    public void a() {
        this.add(new ElytraSwap());
        this.add(new Hitbox());
        this.add(new MaceSwap());
        this.add(new StaticHitboxes());
        this.add(new ConfigDebug());
        this.add(new Phantom());
        this.add(new SelfDestruct());
        this.add(new Animations());
        this.add(new ChunkFinder());
        this.add(new FullBright());
        this.add(new HUD());
        this.add(new KelpESP());
        this.add(new NoRender());
        this.add(new PlayerESP());
        this.add(new StorageESP());
        this.add(new SwingSpeed());
        this.add(new TargetHUD());
        this.add(new AutoEat());
        this.add(new AutoFirework());
        this.add(new AutoMine());
        this.add(new AutoTPA());
        this.add(new AutoTool());
        this.add(new CordSnapper());
        this.add(new ElytraGlide());
        this.add(new FastPlace());
        this.add(new Freecam());
        this.add(new KeyPearl());
        this.add(new NameProtect());
        this.add(new AnchorMacro());
        this.add(new AutoCrystal());
        this.add(new AutoInventoryTotem());
        this.add(new AutoTotem());
        this.add(new DoubleAnchor());
        this.add(new HoverTotem());
        this.add(new AntiTrap());
        this.add(new BStarPhantom());
        this.add(new MemoryFinder());
        this.add(new AuctionSniper());
        this.add(new AutoSell());
        this.add(new AutoSpawnerSell());
        this.add(new BoneDropper());
        this.add(new NetheriteFinder());
        this.add(new RtpBaseFinder());
        this.add(new RTPEndBaseFinder());
        this.add(new ShulkerDropper());
        this.add(new TunnelBaseFinder());
    }

    public List b() {
        return this.modules.stream().filter((obj) -> ((Module)obj).isEnabled()).toList();
    }

    public List c() {
        return this.modules;
    }

    public void d() {
        Gamble.INSTANCE.getEventBus().register(this);

        for(Object nextObj : this.modules) {
            Module next = (Module)nextObj;
            next.addSetting((new BindSetting(EncryptedString.of("Keybind"), next.getKeybind(), true)).setDescription(EncryptedString.of("Key to enabled the module")));
        }

    }

    public List a(Category category) {
        return this.modules.stream().filter((module) -> ((Module)module).getCategory() == category).toList();
    }

    public Module getModuleByClass(Class obj) {
        Objects.requireNonNull(obj);
        Stream var10000 = this.modules.stream();
        Objects.requireNonNull(obj);
        return (Module)var10000.filter(obj::isInstance).findFirst().orElse((Object)null);
    }

    public void add(Module module) {
        Gamble.INSTANCE.getEventBus().register(module);
        this.modules.add(module);
    }

    @EventListener
    public void a(KeyEvent keyEvent) {
        if (Gamble.mc.player != null && !(Gamble.mc.currentScreen instanceof ChatScreen)) {
            if (!(Gamble.mc.currentScreen instanceof ClickGUI)) {
                this.modules.forEach((module) -> {
                    if (((Module)module).getKeybind() == keyEvent.key && keyEvent.mode == 1) {
                        if (module instanceof SelfDestruct && SelfDestruct.isActive) {
                            return;
                        }

                        if (module instanceof Phantom && SelfDestruct.hasSelfDestructed) {
                            return;
                        }

                        ((Module)module).toggle();
                    }

                });
            }
        }
    }
}