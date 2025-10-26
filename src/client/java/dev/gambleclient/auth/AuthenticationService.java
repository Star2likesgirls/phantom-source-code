package dev.gambleclient.auth;

import java.util.concurrent.CompletableFuture;

public class AuthenticationService {
    /**
     * Modified to accept any license key without validation.
     * Always returns a valid UserData object.
     */
    public CompletableFuture<UserData> loginWithLicense(String licenseKey, String hwid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create a valid user without checking any server
                UserData user = new UserData();
                user.setLicenseKey(licenseKey);
                user.setLicenseType("Lifetime");
                user.setExpiryDate("N/A");

                // Use a default username or derive from license key
                if (licenseKey != null && !licenseKey.isEmpty()) {
                    user.setUsername("User_" + licenseKey.substring(0, Math.min(8, licenseKey.length())));
                } else {
                    user.setUsername("User");
                }

                System.out.println("[Sakura Phantom] Login successful for: " + user.getUsername());
                return user;

            } catch (Exception e) {
                System.err.println("[Auth] Unexpected error: " + e.getMessage());
                // Even on error, return a valid user
                UserData fallbackUser = new UserData();
                fallbackUser.setLicenseKey("default");
                fallbackUser.setLicenseType("Lifetime");
                fallbackUser.setExpiryDate("N/A");
                fallbackUser.setUsername("User");
                return fallbackUser;
            }
        });
    }
}