package dev.gambleclient.event.events;

import dev.gambleclient.event.CancellableEvent;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class Render3DEvent extends CancellableEvent {
   public MatrixStack matrixStack;
   public Matrix4f matrix4f;
   public float tickDelta;

   public Render3DEvent(MatrixStack matrixStack, Matrix4f matrix4f, float tickDelta) {
      this.matrixStack = matrixStack;
      this.matrix4f = matrix4f;
      this.tickDelta = tickDelta;
   }
}
