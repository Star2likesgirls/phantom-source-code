package dev.gambleclient.module.modules.ai;

import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class RotationController {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final Random random = new Random();
   private float currentYaw;
   private float targetYaw;
   private float currentPitch;
   private float targetPitch;
   private float currentRotationSpeed;
   private float currentPitchSpeed;
   private boolean isRotating = false;
   private Runnable callback;
   private boolean smoothRotation = true;
   private double baseSpeed = (double)4.5F;
   private double acceleration = 0.8;
   private boolean humanLike = true;
   private double overshootChance = 0.3;
   private boolean preciseLanding = true;
   private double randomVariation = (double)0.0F;
   private double effectiveSpeed;
   private double effectiveAcceleration;

   public void updateSettings(boolean smooth, double speed, double accel, boolean human, double overshoot) {
      this.smoothRotation = smooth;
      this.baseSpeed = speed;
      this.acceleration = accel;
      this.humanLike = human;
      this.overshootChance = overshoot;
   }

   public void updateRandomVariation(double variation) {
      this.randomVariation = variation;
   }

   public void setPreciseLanding(boolean precise) {
      this.preciseLanding = precise;
   }

   public void startRotation(float targetYaw, Runnable onComplete) {
      this.startRotation(targetYaw, 0.0F, onComplete);
   }

   public void startRotation(float targetYaw, float targetPitch, Runnable onComplete) {
      this.targetYaw = targetYaw;
      this.targetPitch = targetPitch;
      this.callback = onComplete;
      this.calculateEffectiveValues();
      if (!this.smoothRotation) {
         this.setYawAngle(targetYaw);
         this.setPitchAngle(targetPitch);
         if (this.callback != null) {
            this.callback.run();
         }

      } else {
         this.isRotating = true;
         this.currentYaw = this.mc.player.getYaw();
         this.currentPitch = this.mc.player.getPitch();
         this.currentRotationSpeed = 0.0F;
         this.currentPitchSpeed = 0.0F;
      }
   }

   private void calculateEffectiveValues() {
      if (this.randomVariation <= (double)0.0F) {
         this.effectiveSpeed = this.baseSpeed;
         this.effectiveAcceleration = this.acceleration;
      } else {
         double speedMultiplier = this.random.nextDouble() * (double)2.0F - (double)1.0F;
         double accelMultiplier = this.random.nextDouble() * (double)2.0F - (double)1.0F;
         double speedVariation = this.randomVariation;
         this.effectiveSpeed = this.baseSpeed + speedMultiplier * speedVariation;
         double accelRatio = this.acceleration / this.baseSpeed;
         double accelVariation = this.randomVariation * accelRatio;
         this.effectiveAcceleration = this.acceleration + accelMultiplier * accelVariation;
         this.effectiveSpeed = Math.max((double)0.5F, Math.min(this.baseSpeed * (double)2.0F, this.effectiveSpeed));
         this.effectiveAcceleration = Math.max(0.1, Math.min(this.acceleration * (double)2.0F, this.effectiveAcceleration));
      }
   }

   public void update() {
      if (this.isRotating) {
         boolean yawComplete = this.updateYaw();
         boolean pitchComplete = this.updatePitch();
         if (yawComplete && pitchComplete) {
            this.isRotating = false;
            if (this.preciseLanding) {
               this.setYawAngle(this.targetYaw);
               this.setPitchAngle(this.targetPitch);
            }

            if (this.callback != null) {
               this.callback.run();
            }
         }

      }
   }

   private boolean updateYaw() {
      float deltaAngle = MathHelper.wrapDegrees(this.targetYaw - this.currentYaw);
      float distance = Math.abs(deltaAngle);
      float snapThreshold = this.preciseLanding ? 1.0F : 0.5F;
      if (distance < snapThreshold) {
         if (this.preciseLanding) {
            this.currentYaw = this.targetYaw;
            this.setYawAngle(this.targetYaw);
         }

         return true;
      } else {
         double speedToUse = this.effectiveSpeed;
         if (this.preciseLanding && distance < 5.0F && this.randomVariation > (double)0.0F) {
            double blendFactor = (double)distance / (double)5.0F;
            speedToUse = this.baseSpeed + (this.effectiveSpeed - this.baseSpeed) * blendFactor;
         }

         float targetSpeed;
         if (distance > 45.0F) {
            targetSpeed = (float)(speedToUse * (double)1.5F);
         } else if (distance > 15.0F) {
            targetSpeed = (float)speedToUse;
         } else {
            targetSpeed = (float)(speedToUse * (double)(distance / 15.0F));
            targetSpeed = Math.max(targetSpeed, this.preciseLanding ? 0.3F : 0.5F);
         }

         float accel = (float)this.effectiveAcceleration;
         if (this.currentRotationSpeed < targetSpeed) {
            this.currentRotationSpeed = Math.min(this.currentRotationSpeed + accel, targetSpeed);
         } else {
            this.currentRotationSpeed = Math.max(this.currentRotationSpeed - accel, targetSpeed);
         }

         float jitter = 0.0F;
         float speedVariation = 1.0F;
         if (this.humanLike && (!this.preciseLanding || distance > 5.0F)) {
            jitter = (this.random.nextFloat() - 0.5F) * 0.2F;
            speedVariation = 0.9F + this.random.nextFloat() * 0.2F;
            if ((double)this.random.nextFloat() < 0.02 && distance > 10.0F) {
               this.currentRotationSpeed *= 0.3F;
            }
         }

         float step = Math.min(distance, this.currentRotationSpeed * speedVariation);
         if (deltaAngle < 0.0F) {
            step = -step;
         }

         this.currentYaw += step + jitter;
         if (this.preciseLanding) {
            float newDelta = MathHelper.wrapDegrees(this.targetYaw - this.currentYaw);
            if (Math.signum(newDelta) != Math.signum(deltaAngle)) {
               this.currentYaw = this.targetYaw;
            }
         }

         this.setYawAngle(this.currentYaw);
         return false;
      }
   }

   private boolean updatePitch() {
      float deltaAngle = this.targetPitch - this.currentPitch;
      float distance = Math.abs(deltaAngle);
      float snapThreshold = this.preciseLanding ? 1.0F : 0.5F;
      if (distance < snapThreshold) {
         if (this.preciseLanding) {
            this.currentPitch = this.targetPitch;
            this.setPitchAngle(this.targetPitch);
         }

         return true;
      } else {
         double speedToUse = this.effectiveSpeed;
         if (this.preciseLanding && distance < 3.0F && this.randomVariation > (double)0.0F) {
            double blendFactor = (double)distance / (double)3.0F;
            speedToUse = this.baseSpeed + (this.effectiveSpeed - this.baseSpeed) * blendFactor;
         }

         float targetSpeed;
         if (distance > 30.0F) {
            targetSpeed = (float)(speedToUse * 1.2);
         } else if (distance > 10.0F) {
            targetSpeed = (float)(speedToUse * 0.8);
         } else {
            targetSpeed = (float)(speedToUse * 0.8 * (double)(distance / 10.0F));
            targetSpeed = Math.max(targetSpeed, this.preciseLanding ? 0.25F : 0.4F);
         }

         float accel = (float)(this.effectiveAcceleration * 0.8);
         if (this.currentPitchSpeed < targetSpeed) {
            this.currentPitchSpeed = Math.min(this.currentPitchSpeed + accel, targetSpeed);
         } else {
            this.currentPitchSpeed = Math.max(this.currentPitchSpeed - accel, targetSpeed);
         }

         float jitter = 0.0F;
         float speedVariation = 1.0F;
         if (this.humanLike && (!this.preciseLanding || distance > 3.0F)) {
            jitter = (this.random.nextFloat() - 0.5F) * 0.15F;
            speedVariation = 0.92F + this.random.nextFloat() * 0.16F;
         }

         float step = Math.min(distance, this.currentPitchSpeed * speedVariation);
         if (deltaAngle < 0.0F) {
            step = -step;
         }

         this.currentPitch += step + jitter;
         if (this.preciseLanding) {
            float newDelta = this.targetPitch - this.currentPitch;
            if (Math.signum(newDelta) != Math.signum(deltaAngle)) {
               this.currentPitch = this.targetPitch;
            }
         }

         this.setPitchAngle(this.currentPitch);
         return false;
      }
   }

   private void setYawAngle(float yawAngle) {
      this.mc.player.setYaw(yawAngle);
      this.mc.player.headYaw = yawAngle;
      this.mc.player.bodyYaw = yawAngle;
   }

   private void setPitchAngle(float pitchAngle) {
      pitchAngle = MathHelper.clamp(pitchAngle, -90.0F, 90.0F);
      this.mc.player.setPitch(pitchAngle);
   }

   public boolean isRotating() {
      return this.isRotating;
   }

   public void resetPitch(Runnable onComplete) {
      if (Math.abs(this.mc.player.getPitch()) > 1.0F) {
         this.startRotation(this.mc.player.getYaw(), 0.0F, onComplete);
      } else {
         if (this.preciseLanding) {
            this.setPitchAngle(0.0F);
         }

         if (onComplete != null) {
            onComplete.run();
         }
      }

   }
}
