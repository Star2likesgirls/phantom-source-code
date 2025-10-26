package dev.gambleclient.event.events;

import dev.gambleclient.event.CancellableEvent;
import net.minecraft.client.gui.DrawContext;

public class Render2DEvent extends CancellableEvent {
   public DrawContext context;
   public float tickDelta;

   public Render2DEvent(DrawContext context, float tickDelta) {
      this.context = context;
      this.tickDelta = tickDelta;
   }
}
