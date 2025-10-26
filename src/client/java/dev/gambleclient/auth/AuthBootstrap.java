package dev.gambleclient.auth;

import dev.gambleclient.gui.LoginScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class AuthBootstrap {
    private static boolean registered = false;
    private static boolean shown = false;

    private AuthBootstrap() {
    }

    public static void init() {
        if (!registered) {
            registered = true;
            ClientTickEvents.END_CLIENT_TICK.register((ClientTickEvents.EndTick)(client) -> {
                if (!shown) {
                    if (client != null) {
                        if (client.getOverlay() == null) {
                            try {
                                if (!(client.currentScreen instanceof LoginScreen)) {
                                    client.setScreen(new LoginScreen());
                                }

                                shown = true;
                            } catch (Throwable var2) {
                            }

                        }
                    }
                }
            });
        }
    }
}