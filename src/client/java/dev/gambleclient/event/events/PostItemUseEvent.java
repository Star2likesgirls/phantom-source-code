package dev.gambleclient.event.events;

import dev.gambleclient.event.CancellableEvent;

public class PostItemUseEvent extends CancellableEvent {
   public int cooldown;

   public PostItemUseEvent(int cooldown) {
      this.cooldown = cooldown;
   }
}
