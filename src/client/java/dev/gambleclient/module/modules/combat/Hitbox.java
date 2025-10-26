package dev.gambleclient.module.modules.combat;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.TargetMarginEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import net.minecraft.entity.player.PlayerEntity;

public final class Hitbox extends Module {
   private final NumberSetting expand = new NumberSetting(EncryptedString.of("Expand"), (double)0.0F, (double)2.0F, (double)0.5F, 0.05);
   private final BooleanSetting enableRender = new BooleanSetting("Enable Render", true);

   public Hitbox() {
      super(EncryptedString.of("HitBox"), EncryptedString.of("Expands a player's hitbox."), -1, Category.COMBAT);
      this.addSettings(new Setting[]{this.enableRender, this.expand});
   }

   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onTargetMargin(TargetMarginEvent targetMarginEvent) {
      if (targetMarginEvent.entity instanceof PlayerEntity) {
         targetMarginEvent.cir.setReturnValue((float)this.expand.getValue());
      }

   }

   public double getHitboxExpansion() {
      return !this.enableRender.getValue() ? (double)0.0F : this.expand.getValue();
   }
}
