package dev.gambleclient.module.modules.render;

import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public final class Animations extends Module {
   private final BooleanSetting enabled = (new BooleanSetting(EncryptedString.of("Enabled"), true)).setDescription(EncryptedString.of("Enable custom item animations"));
   private final NumberSetting swingSpeed = (NumberSetting)(new NumberSetting(EncryptedString.of("Swing Speed"), (double)1.0F, (double)10.0F, (double)1.0F, 0.1)).setDescription(EncryptedString.of("Speed of item swinging animation"));
   private final NumberSetting scale = (NumberSetting)(new NumberSetting(EncryptedString.of("Scale"), (double)0.5F, (double)2.0F, (double)1.0F, 0.1)).setDescription(EncryptedString.of("Scale of held items"));
   private final NumberSetting xOffset = (NumberSetting)(new NumberSetting(EncryptedString.of("X Offset"), (double)-1.0F, (double)1.0F, (double)0.0F, 0.1)).setDescription(EncryptedString.of("Horizontal offset of held items"));
   private final NumberSetting yOffset = (NumberSetting)(new NumberSetting(EncryptedString.of("Y Offset"), (double)-1.0F, (double)1.0F, (double)0.0F, 0.1)).setDescription(EncryptedString.of("Vertical offset of held items"));
   private final NumberSetting zOffset = (NumberSetting)(new NumberSetting(EncryptedString.of("Z Offset"), (double)-1.0F, (double)1.0F, (double)0.0F, 0.1)).setDescription(EncryptedString.of("Depth offset of held items"));

   public Animations() {
      super(EncryptedString.of("Animations"), EncryptedString.of("Custom item animations and transformations"), -1, Category.RENDER);
      this.addSettings(new Setting[]{this.enabled, this.swingSpeed, this.scale, this.xOffset, this.yOffset, this.zOffset});
   }

   public boolean shouldAnimate() {
      return this.isEnabled() && this.enabled.getValue();
   }

   public void applyTransformations(MatrixStack matrices, float swingProgress) {
      matrices.push();
      float scaleValue = this.scale.getFloatValue();
      matrices.scale(scaleValue, scaleValue, scaleValue);
      matrices.translate(this.xOffset.getFloatValue(), this.yOffset.getFloatValue(), this.zOffset.getFloatValue());
      float swingSpeedValue = this.swingSpeed.getFloatValue();
      float swingAngle = swingProgress * swingSpeedValue * 90.0F;
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(swingAngle));
   }
}
