package dev.gambleclient.module.modules.render;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.Render3DEvent;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.BooleanSetting;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public final class NoRender extends Module {
   private final BooleanSetting rain = (new BooleanSetting(EncryptedString.of("Rain"), true)).setDescription(EncryptedString.of("Disables rain rendering"));
   private final BooleanSetting snow = (new BooleanSetting(EncryptedString.of("Snow"), true)).setDescription(EncryptedString.of("Disables snow rendering"));
   private final BooleanSetting thunder = (new BooleanSetting(EncryptedString.of("Thunder"), true)).setDescription(EncryptedString.of("Disables thunder rendering"));
   private final BooleanSetting clouds = (new BooleanSetting(EncryptedString.of("Clouds"), false)).setDescription(EncryptedString.of("Disables cloud rendering"));
   private final BooleanSetting fog = (new BooleanSetting(EncryptedString.of("Fog"), false)).setDescription(EncryptedString.of("Disables fog rendering"));
   private final BooleanSetting fire = (new BooleanSetting(EncryptedString.of("Fire"), false)).setDescription(EncryptedString.of("Disables fire rendering"));
   private final BooleanSetting water = (new BooleanSetting(EncryptedString.of("Water"), false)).setDescription(EncryptedString.of("Disables water rendering"));
   private final BooleanSetting lava = (new BooleanSetting(EncryptedString.of("Lava"), false)).setDescription(EncryptedString.of("Disables lava rendering"));
   private final BooleanSetting portal = (new BooleanSetting(EncryptedString.of("Portal"), false)).setDescription(EncryptedString.of("Disables portal rendering"));
   private final BooleanSetting barrier = (new BooleanSetting(EncryptedString.of("Barrier"), true)).setDescription(EncryptedString.of("Disables barrier block rendering"));
   private final BooleanSetting structureVoid = (new BooleanSetting(EncryptedString.of("Structure Void"), true)).setDescription(EncryptedString.of("Disables structure void rendering"));
   private final BooleanSetting armorStands = (new BooleanSetting(EncryptedString.of("Armor Stands"), false)).setDescription(EncryptedString.of("Disables armor stand rendering"));
   private final BooleanSetting itemFrames = (new BooleanSetting(EncryptedString.of("Item Frames"), false)).setDescription(EncryptedString.of("Disables item frame rendering"));
   private final BooleanSetting paintings = (new BooleanSetting(EncryptedString.of("Paintings"), false)).setDescription(EncryptedString.of("Disables painting rendering"));
   private final BooleanSetting enderman = (new BooleanSetting(EncryptedString.of("Enderman"), false)).setDescription(EncryptedString.of("Disables enderman rendering"));
   private final BooleanSetting guardian = (new BooleanSetting(EncryptedString.of("Guardian"), false)).setDescription(EncryptedString.of("Disables guardian rendering"));
   private final BooleanSetting slime = (new BooleanSetting(EncryptedString.of("Slime"), false)).setDescription(EncryptedString.of("Disables slime rendering"));
   private final BooleanSetting particles = (new BooleanSetting(EncryptedString.of("Particles"), false)).setDescription(EncryptedString.of("Disables particle rendering"));
   private final BooleanSetting explosions = (new BooleanSetting(EncryptedString.of("Explosions"), false)).setDescription(EncryptedString.of("Disables explosion particle rendering"));
   private final BooleanSetting smoke = (new BooleanSetting(EncryptedString.of("Smoke"), false)).setDescription(EncryptedString.of("Disables smoke particle rendering"));
   private final BooleanSetting pumpkinBlur = (new BooleanSetting(EncryptedString.of("Pumpkin Blur"), true)).setDescription(EncryptedString.of("Disables pumpkin blur overlay"));
   private final BooleanSetting vignette = (new BooleanSetting(EncryptedString.of("Vignette"), false)).setDescription(EncryptedString.of("Disables vignette overlay"));
   private final BooleanSetting hurtCam = (new BooleanSetting(EncryptedString.of("Hurt Camera"), false)).setDescription(EncryptedString.of("Disables hurt camera effect"));
   private final BooleanSetting totemAnimation = (new BooleanSetting(EncryptedString.of("Totem Animation"), false)).setDescription(EncryptedString.of("Disables totem animation"));
   private final BooleanSetting noSwing = (new BooleanSetting(EncryptedString.of("No Swing"), false)).setDescription(EncryptedString.of("Disables hand swinging animation"));
   private final NumberSetting entityDistance = new NumberSetting(EncryptedString.of("Entity Distance"), (double)0.0F, (double)1000.0F, (double)64.0F, (double)1.0F);
   private final NumberSetting blockDistance = new NumberSetting(EncryptedString.of("Block Distance"), (double)0.0F, (double)1000.0F, (double)128.0F, (double)1.0F);

   public NoRender() {
      super(EncryptedString.of("NoRender"), EncryptedString.of("Disables various rendering elements to improve performance"), -1, Category.RENDER);
      this.entityDistance.setDescription(EncryptedString.of("Maximum distance to render entities"));
      this.blockDistance.setDescription(EncryptedString.of("Maximum distance to render blocks"));
      this.addSettings(new Setting[]{this.rain, this.snow, this.thunder, this.clouds, this.fog, this.fire, this.water, this.lava, this.portal, this.barrier, this.structureVoid, this.armorStands, this.itemFrames, this.paintings, this.enderman, this.guardian, this.slime, this.particles, this.explosions, this.smoke, this.pumpkinBlur, this.vignette, this.hurtCam, this.totemAnimation, this.noSwing, this.entityDistance, this.blockDistance});
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (this.mc.world != null) {
         if (this.rain.getValue()) {
            this.mc.world.setRainGradient(0.0F);
         }

         if (this.snow.getValue()) {
            this.mc.world.setThunderGradient(0.0F);
         }

         if (this.thunder.getValue()) {
            this.mc.world.setThunderGradient(0.0F);
         }

      }
   }

   @EventListener
   public void onRender3D(Render3DEvent event) {
      if (this.mc.world != null && this.mc.player != null) {
         Camera cam = this.mc.gameRenderer.getCamera();
         if (cam != null) {
            Vec3d camPos = cam.getPos();
            MatrixStack matrices = event.matrixStack;
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cam.getYaw() + 180.0F));
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);
         }

         double entityDist = this.entityDistance.getValue();
         double blockDist = this.blockDistance.getValue();

         for(Entity entity : this.mc.world.getEntities()) {
            double distance = (double)this.mc.player.distanceTo(entity);
            if (!(distance > entityDist) && (!this.armorStands.getValue() || !(entity instanceof ArmorStandEntity)) && (!this.itemFrames.getValue() || !(entity instanceof ItemFrameEntity)) && (!this.paintings.getValue() || !(entity instanceof PaintingEntity)) && (!this.enderman.getValue() || !(entity instanceof EndermanEntity)) && (!this.guardian.getValue() || !(entity instanceof GuardianEntity)) && this.slime.getValue() && entity instanceof SlimeEntity) {
            }
         }

         event.matrixStack.pop();
      }
   }

   public boolean shouldRenderRain() {
      return !this.rain.getValue();
   }

   public boolean shouldRenderSnow() {
      return !this.snow.getValue();
   }

   public boolean shouldRenderThunder() {
      return !this.thunder.getValue();
   }

   public boolean shouldRenderClouds() {
      return !this.clouds.getValue();
   }

   public boolean shouldRenderFog() {
      return !this.fog.getValue();
   }

   public boolean shouldRenderFire() {
      return !this.fire.getValue();
   }

   public boolean shouldRenderWater() {
      return !this.water.getValue();
   }

   public boolean shouldRenderLava() {
      return !this.lava.getValue();
   }

   public boolean shouldRenderPortal() {
      return !this.portal.getValue();
   }

   public boolean shouldRenderBarrier() {
      return !this.barrier.getValue();
   }

   public boolean shouldRenderStructureVoid() {
      return !this.structureVoid.getValue();
   }

   public boolean shouldRenderPumpkinBlur() {
      return !this.pumpkinBlur.getValue();
   }

   public boolean shouldRenderVignette() {
      return !this.vignette.getValue();
   }

   public boolean shouldRenderHurtCam() {
      return !this.hurtCam.getValue();
   }

   public boolean shouldRenderTotemAnimation() {
      return !this.totemAnimation.getValue();
   }

   public boolean shouldRenderParticles() {
      return !this.particles.getValue();
   }

   public boolean shouldRenderExplosions() {
      return !this.explosions.getValue();
   }

   public boolean shouldRenderSmoke() {
      return !this.smoke.getValue();
   }

   public boolean shouldRenderSwing() {
      return !this.noSwing.getValue();
   }

   public boolean shouldRenderEntity(Entity entity) {
      if (entity == null) {
         return true;
      } else if (this.armorStands.getValue() && entity instanceof ArmorStandEntity) {
         return false;
      } else if (this.itemFrames.getValue() && entity instanceof ItemFrameEntity) {
         return false;
      } else if (this.paintings.getValue() && entity instanceof PaintingEntity) {
         return false;
      } else if (this.enderman.getValue() && entity instanceof EndermanEntity) {
         return false;
      } else if (this.guardian.getValue() && entity instanceof GuardianEntity) {
         return false;
      } else if (this.slime.getValue() && entity instanceof SlimeEntity) {
         return false;
      } else {
         if (this.mc.player != null) {
            double distance = (double)this.mc.player.distanceTo(entity);
            if (distance > this.entityDistance.getValue()) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean shouldRenderBlock(BlockState state, BlockPos pos) {
      if (state == null) {
         return true;
      } else if (this.fire.getValue() && state.getBlock() == Blocks.FIRE) {
         return false;
      } else if (this.water.getValue() && state.getBlock() == Blocks.WATER) {
         return false;
      } else if (this.lava.getValue() && state.getBlock() == Blocks.LAVA) {
         return false;
      } else if (this.portal.getValue() && state.getBlock() == Blocks.NETHER_PORTAL) {
         return false;
      } else if (this.barrier.getValue() && state.getBlock() == Blocks.BARRIER) {
         return false;
      } else if (this.structureVoid.getValue() && state.getBlock() == Blocks.STRUCTURE_VOID) {
         return false;
      } else {
         if (this.mc.player != null) {
            double distance = this.mc.player.getBlockPos().getSquaredDistance(pos);
            if (distance > this.blockDistance.getValue() * this.blockDistance.getValue()) {
               return false;
            }
         }

         return true;
      }
   }
}
