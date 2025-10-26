package dev.gambleclient.event.events;

import dev.gambleclient.event.CancellableEvent;

public class MouseButtonEvent extends CancellableEvent {
   public int button;
   public int actions;
   public long window;

   public MouseButtonEvent(int button, long window, int actions) {
      this.button = button;
      this.window = window;
      this.actions = actions;
   }
}
