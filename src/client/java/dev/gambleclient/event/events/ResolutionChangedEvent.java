package dev.gambleclient.event.events;

import dev.gambleclient.event.CancellableEvent;
import net.minecraft.client.util.Window;

public class ResolutionChangedEvent extends CancellableEvent {
   public Window window;

   public ResolutionChangedEvent(Window window) {
      this.window = window;
   }
}
