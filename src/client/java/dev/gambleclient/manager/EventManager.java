package dev.gambleclient.manager;

import dev.gambleclient.Gamble;
import dev.gambleclient.event.CancellableEvent;
import dev.gambleclient.event.Event;
import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.Listener;
import dev.gambleclient.event.events.KeyEvent;
import dev.gambleclient.module.Module;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventManager {
    private final Map EVENTS = new HashMap();

    public void register(Object o) {
        Method[] declaredMethods = o.getClass().getDeclaredMethods();

        for(Method method : declaredMethods) {
            if (method.isAnnotationPresent(EventListener.class) && method.getParameterCount() == 1 && Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                this.addListener(o, method, (EventListener)method.getAnnotation(EventListener.class));
            }
        }

    }

    private void addListener(Object o, Method method, EventListener eventListener) {
        Class<?> key = method.getParameterTypes()[0];
        method.setAccessible(true);
        ((List)this.EVENTS.computeIfAbsent(key, (p0) -> new CopyOnWriteArrayList())).add(new Listener(o, method, eventListener.priority()));
        ((List)this.EVENTS.get(key)).sort(Comparator.comparingInt((listener) -> ((Listener)listener).getPriority().getValue()));
    }

    public void unregister(Object v12) {
        for(Object listenersObj : this.EVENTS.values()) {
            List listeners = (List)listenersObj;
            listeners.removeIf((v1) -> ((Listener)v1).getInstance() == v12);
        }

    }

    public void clear() {
        this.EVENTS.clear();
    }

    public void a(Event event) {
        List<Listener> listeners = (List)this.EVENTS.get(event.getClass());
        if (listeners != null) {
            for(Listener listener : listeners) {
                try {
                    Object holder = listener.getInstance();

                    // CRITICAL: KeyEvents should always fire regardless of module enabled state
                    // This allows modules to be toggled on/off via keybinds
                    boolean shouldInvoke;
                    if (event instanceof KeyEvent) {
                        // KeyEvents always fire, even for disabled modules
                        shouldInvoke = !event.isCancelled() || event instanceof CancellableEvent;
                    } else {
                        // Other events only fire if module is enabled
                        shouldInvoke = (!(holder instanceof Module) || ((Module)holder).isEnabled()) && (!event.isCancelled() || event instanceof CancellableEvent);
                    }

                    if (shouldInvoke) {
                        listener.invoke(event);
                    }
                } catch (Throwable _t) {
                    PrintStream var10000 = System.err;
                    String var10001 = event.getClass().getSimpleName();
                    var10000.println("Error dispatching event " + var10001 + " to " + (listener.getInstance() != null ? listener.getInstance().getClass().getSimpleName() : "unknown"));
                    _t.printStackTrace(System.err);
                }
            }

        }
    }

    public static void b(Event evt) {
        if (Gamble.INSTANCE != null && Gamble.INSTANCE.getEventBus() != null) {
            Gamble.INSTANCE.getEventBus().a(evt);
        }
    }
}