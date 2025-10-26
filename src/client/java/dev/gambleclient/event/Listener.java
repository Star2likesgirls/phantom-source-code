package dev.gambleclient.event;

import java.lang.reflect.Method;

public class Listener {
   private final Object instance;
   private final Method method;
   private final Priority priority;

   public Listener(Object instance, Method method, Priority priority) {
      this.instance = instance;
      this.method = method;
      this.priority = priority;
   }

   public void invoke(Event event) throws Throwable {
      this.method.invoke(this.instance, event);
   }

   public Object getInstance() {
      return this.instance;
   }

   public Priority getPriority() {
      return this.priority;
   }
}
