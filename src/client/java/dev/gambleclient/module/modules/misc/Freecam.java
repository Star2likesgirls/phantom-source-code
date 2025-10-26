package dev.gambleclient.module.modules.misc;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.ChunkMarkClosedEvent;
import dev.gambleclient.event.events.KeyEvent;
import dev.gambleclient.event.events.MouseButtonEvent;
import dev.gambleclient.event.events.MouseScrolledEvent;
import dev.gambleclient.event.events.SetScreenEvent;
import dev.gambleclient.event.events.TickEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.KeyUtils;
import dev.gambleclient.utils.Utils;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.option.Perspective;
import org.joml.Vector3d;

public final class Freecam extends Module {
   private final NumberSetting speed = new NumberSetting(EncryptedString.of("Speed"), (double)1.0F, (double)10.0F, (double)1.0F, 0.1);
   public final Vector3d currentPosition = new Vector3d();
   public final Vector3d previousPosition = new Vector3d();
   private Perspective currentPerspective;
   private double movementSpeed;
   public float yaw;
   public float pitch;
   public float previousYaw;
   public float previousPitch;
   private boolean isMovingForward;
   private boolean isMovingBackward;
   private boolean isMovingRight;
   private boolean isMovingLeft;
   private boolean isMovingUp;
   private boolean isMovingDown;

   public Freecam() {
      super(EncryptedString.of("Freecam"), EncryptedString.of("Lets you move freely around the world without actually moving"), -1, Category.MISC);
      this.addSettings(new Setting[]{this.speed});
   }

   public void onEnable() {
      if (this.mc.player == null) {
         this.toggle();
      } else {
         this.mc.options.getFovEffectScale().setValue((double)0.0F);
         this.mc.options.getBobView().setValue(false);
         this.yaw = this.mc.player.getYaw();
         this.pitch = this.mc.player.getPitch();
         this.currentPerspective = this.mc.options.getPerspective();
         this.movementSpeed = this.speed.getValue();
         Utils.copyVector(this.currentPosition, this.mc.gameRenderer.getCamera().getPos());
         Utils.copyVector(this.previousPosition, this.mc.gameRenderer.getCamera().getPos());
         if (this.mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            this.yaw += 180.0F;
            this.pitch *= -1.0F;
         }

         this.previousYaw = this.yaw;
         this.previousPitch = this.pitch;
         this.isMovingForward = this.mc.options.forwardKey.isPressed();
         this.isMovingBackward = this.mc.options.backKey.isPressed();
         this.isMovingRight = this.mc.options.rightKey.isPressed();
         this.isMovingLeft = this.mc.options.leftKey.isPressed();
         this.isMovingUp = this.mc.options.jumpKey.isPressed();
         this.isMovingDown = this.mc.options.sneakKey.isPressed();
         this.resetMovementKeys();
         super.onEnable();
      }
   }

   public void onDisable() {
      this.resetMovementKeys();
      this.previousPosition.set(this.currentPosition);
      this.previousYaw = this.yaw;
      this.previousPitch = this.pitch;
      super.onDisable();
   }

   private void resetMovementKeys() {
      this.mc.options.forwardKey.setPressed(false);
      this.mc.options.backKey.setPressed(false);
      this.mc.options.rightKey.setPressed(false);
      this.mc.options.leftKey.setPressed(false);
      this.mc.options.jumpKey.setPressed(false);
      this.mc.options.sneakKey.setPressed(false);
   }

   @EventListener
   private void handleSetScreenEvent(SetScreenEvent setScreenEvent) {
      this.resetMovementKeys();
      this.previousPosition.set(this.currentPosition);
      this.previousYaw = this.yaw;
      this.previousPitch = this.pitch;
   }

   @EventListener
   private void handleTickEvent(TickEvent tickEvent) {
      if (this.mc.cameraEntity.isInsideWall()) {
         this.mc.getCameraEntity().noClip = true;
      }

      if (!this.currentPerspective.isFirstPerson()) {
         this.mc.options.setPerspective(Perspective.FIRST_PERSON);
      }

      Vec3d forwardVector = Vec3d.fromPolar(0.0F, this.yaw);
      Vec3d rightVector = Vec3d.fromPolar(0.0F, this.yaw + 90.0F);
      double xMovement = (double)0.0F;
      double yMovement = (double)0.0F;
      double zMovement = (double)0.0F;
      double speedMultiplier = (double)0.5F;
      if (this.mc.options.sprintKey.isPressed()) {
         speedMultiplier = (double)1.0F;
      }

      boolean isMovingHorizontally = false;
      if (this.isMovingForward) {
         xMovement += forwardVector.x * speedMultiplier * this.movementSpeed;
         zMovement += forwardVector.z * speedMultiplier * this.movementSpeed;
         isMovingHorizontally = true;
      }

      if (this.isMovingBackward) {
         xMovement -= forwardVector.x * speedMultiplier * this.movementSpeed;
         zMovement -= forwardVector.z * speedMultiplier * this.movementSpeed;
         isMovingHorizontally = true;
      }

      boolean isMovingLaterally = false;
      if (this.isMovingRight) {
         xMovement += rightVector.x * speedMultiplier * this.movementSpeed;
         zMovement += rightVector.z * speedMultiplier * this.movementSpeed;
         isMovingLaterally = true;
      }

      if (this.isMovingLeft) {
         xMovement -= rightVector.x * speedMultiplier * this.movementSpeed;
         zMovement -= rightVector.z * speedMultiplier * this.movementSpeed;
         isMovingLaterally = true;
      }

      if (isMovingHorizontally && isMovingLaterally) {
         double diagonalMultiplier = (double)1.0F / Math.sqrt((double)2.0F);
         xMovement *= diagonalMultiplier;
         zMovement *= diagonalMultiplier;
      }

      if (this.isMovingUp) {
         yMovement += speedMultiplier * this.movementSpeed;
      }

      if (this.isMovingDown) {
         yMovement -= speedMultiplier * this.movementSpeed;
      }

      this.previousPosition.set(this.currentPosition);
      this.currentPosition.set(this.currentPosition.x + xMovement, this.currentPosition.y + yMovement, this.currentPosition.z + zMovement);
   }

   @EventListener
   public void onKey(KeyEvent keyEvent) {
      if (!KeyUtils.isKeyPressed(292)) {
         boolean keyHandled = true;
         if (this.mc.options.forwardKey.matchesKey(keyEvent.key, 0)) {
            this.isMovingForward = keyEvent.mode != 0;
            this.mc.options.forwardKey.setPressed(false);
         } else if (this.mc.options.backKey.matchesKey(keyEvent.key, 0)) {
            this.isMovingBackward = keyEvent.mode != 0;
            this.mc.options.backKey.setPressed(false);
         } else if (this.mc.options.rightKey.matchesKey(keyEvent.key, 0)) {
            this.isMovingRight = keyEvent.mode != 0;
            this.mc.options.rightKey.setPressed(false);
         } else if (this.mc.options.leftKey.matchesKey(keyEvent.key, 0)) {
            this.isMovingLeft = keyEvent.mode != 0;
            this.mc.options.leftKey.setPressed(false);
         } else if (this.mc.options.jumpKey.matchesKey(keyEvent.key, 0)) {
            this.isMovingUp = keyEvent.mode != 0;
            this.mc.options.jumpKey.setPressed(false);
         } else if (this.mc.options.sneakKey.matchesKey(keyEvent.key, 0)) {
            this.isMovingDown = keyEvent.mode != 0;
            this.mc.options.sneakKey.setPressed(false);
         } else {
            keyHandled = false;
         }

         if (keyHandled) {
            keyEvent.cancel();
         }

      }
   }

   @EventListener
   private void handleMouseButtonEvent(MouseButtonEvent event) {
      boolean buttonHandled = true;
      if (this.mc.options.forwardKey.matchesMouse(event.button)) {
         this.isMovingForward = event.actions != 0;
         this.mc.options.forwardKey.setPressed(false);
      } else if (this.mc.options.backKey.matchesMouse(event.button)) {
         this.isMovingBackward = event.actions != 0;
         this.mc.options.backKey.setPressed(false);
      } else if (this.mc.options.rightKey.matchesMouse(event.button)) {
         this.isMovingRight = event.actions != 0;
         this.mc.options.rightKey.setPressed(false);
      } else if (this.mc.options.leftKey.matchesMouse(event.button)) {
         this.isMovingLeft = event.actions != 0;
         this.mc.options.leftKey.setPressed(false);
      } else if (this.mc.options.jumpKey.matchesMouse(event.button)) {
         this.isMovingUp = event.actions != 0;
         this.mc.options.jumpKey.setPressed(false);
      } else if (this.mc.options.sneakKey.matchesMouse(event.button)) {
         this.isMovingDown = event.actions != 0;
         this.mc.options.sneakKey.setPressed(false);
      } else {
         buttonHandled = false;
      }

      if (buttonHandled) {
         event.cancel();
      }

   }

   @EventListener
   private void handleMouseScrolledEvent(MouseScrolledEvent mouseScrolledEvent) {
      if (this.mc.currentScreen == null) {
         this.movementSpeed += mouseScrolledEvent.amount * (double)0.25F * this.movementSpeed;
         if (this.movementSpeed < 0.1) {
            this.movementSpeed = 0.1;
         }

         mouseScrolledEvent.cancel();
      }

   }

   @EventListener
   private void handleChunkMarkClosedEvent(ChunkMarkClosedEvent chunkMarkClosedEvent) {
      chunkMarkClosedEvent.cancel();
   }

   public void updateRotation(double deltaYaw, double deltaPitch) {
      this.previousYaw = this.yaw;
      this.previousPitch = this.pitch;
      this.yaw += (float)deltaYaw;
      this.pitch += (float)deltaPitch;
      this.pitch = MathHelper.clamp(this.pitch, -90.0F, 90.0F);
   }

   public double getInterpolatedX(float partialTicks) {
      return MathHelper.lerp((double)partialTicks, this.previousPosition.x, this.currentPosition.x);
   }

   public double getInterpolatedY(float partialTicks) {
      return MathHelper.lerp((double)partialTicks, this.previousPosition.y, this.currentPosition.y);
   }

   public double getInterpolatedZ(float partialTicks) {
      return MathHelper.lerp((double)partialTicks, this.previousPosition.z, this.currentPosition.z);
   }

   public double getInterpolatedYaw(float partialTicks) {
      return (double)MathHelper.lerp(partialTicks, this.previousYaw, this.yaw);
   }

   public double getInterpolatedPitch(float partialTicks) {
      return (double)MathHelper.lerp(partialTicks, this.previousPitch, this.pitch);
   }
}
