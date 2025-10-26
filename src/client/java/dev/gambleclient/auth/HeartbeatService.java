package dev.gambleclient.auth;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class HeartbeatService {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor((r) -> {
        Thread t = new Thread(r, "Phantom-Heartbeat");
        t.setDaemon(true);
        return t;
    });
    private static volatile boolean running = false;
    private static String licenseKey;
    private static String hwid;

    private HeartbeatService() {
    }

    public static synchronized void start(String key, String deviceHwid) {
        licenseKey = (String)Objects.requireNonNull(key);
        hwid = (String)Objects.requireNonNull(deviceHwid);
        if (!running) {
            running = true;
            // Start the heartbeat, but it won't do anything harmful
            scheduler.scheduleAtFixedRate(HeartbeatService::beat, 5L, 10L, TimeUnit.SECONDS);
            System.out.println("[Sakura] Shits cracked LOL");
        }
    }

    public static synchronized void stop() {
        running = false;
        System.out.println("[Heartbeat] Stopped");
    }

    private static void beat() {
        if (running) {
            // Heartbeat now just logs that it's running
            // No server validation, no crashes
            System.out.println("[Sakura Client Cracks] Tick - blocked phantom crash");
        }
    }
}