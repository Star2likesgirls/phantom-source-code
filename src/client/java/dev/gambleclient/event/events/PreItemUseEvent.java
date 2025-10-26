package dev.gambleclient.event.events;

import dev.gambleclient.event.CancellableEvent;

public class PreItemUseEvent extends CancellableEvent {
   public int cooldown;

   public PreItemUseEvent(int cooldown) {
      this.cooldown = cooldown;
   }
}
