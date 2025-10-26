package dev.gambleclient.event.events;

import dev.gambleclient.event.CancellableEvent;

public class MouseScrolledEvent extends CancellableEvent {
   public double amount;

   public MouseScrolledEvent(double amount) {
      this.amount = amount;
   }
}
