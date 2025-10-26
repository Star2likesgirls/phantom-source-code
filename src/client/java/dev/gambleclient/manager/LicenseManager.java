package dev.gambleclient.manager;

import dev.gambleclient.utils.SecurityUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class LicenseManager {
   private static final String LICENSE_CACHE_FILE = "phantom.license.cache";
   private static final String LICENSE_CONFIG_FILE = "phantom.license.config";
   private static final long CACHE_DURATION = 86400000L;
   private static final String OFFLINE_SIGNATURE = "OFFLINE_MODE_ENABLED";
   private final Properties config = new Properties();
   private final Map licenseCache = new HashMap();
   private String currentLicense = "";
   private boolean offlineMode = false;

   public LicenseManager() {
      this.loadConfig();
   }

   public CompletableFuture initialize() {
      return CompletableFuture.supplyAsync(() -> {
         try {
            this.loadLicenseCache();
            return true;
         } catch (Exception e) {
            System.err.println("[LicenseManager] Initialization failed: " + e.getMessage());
            return false;
         }
      });
   }

   public CompletableFuture validateLicense(String licenseKey) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            if (this.licenseCache.containsKey(licenseKey)) {
               LicenseInfo cached = (LicenseInfo)this.licenseCache.get(licenseKey);
               if (System.currentTimeMillis() < cached.getExpiryTime()) {
                  this.currentLicense = licenseKey;
                  return new LicenseValidationResult(true, "License valid (cached)", cached);
               }

               this.licenseCache.remove(licenseKey);
            }

            if (!SecurityUtils.isValidLicenseFormat(licenseKey)) {
               return new LicenseValidationResult(false, "Invalid license format", (LicenseInfo)null);
            } else {
               return this.isOfflineLicense(licenseKey) ? this.validateOfflineLicense(licenseKey) : this.simulateOnlineValidation(licenseKey);
            }
         } catch (Exception e) {
            System.err.println("[LicenseManager] License validation failed: " + e.getMessage());
            return new LicenseValidationResult(false, "Validation error: " + e.getMessage(), (LicenseInfo)null);
         }
      });
   }

   public boolean isOfflineLicense(String licenseKey) {
      return licenseKey.startsWith("OFFLINE_") || licenseKey.equals("OFFLINE_MODE_ENABLED");
   }

   private LicenseValidationResult validateOfflineLicense(String licenseKey) {
      try {
         String hwid = SecurityUtils.generateHardwareFingerprint();
         String expectedLicense = this.generateOfflineLicense(hwid);
         if (!licenseKey.equals(expectedLicense) && !licenseKey.equals("OFFLINE_MODE_ENABLED")) {
            return new LicenseValidationResult(false, "Invalid offline license", (LicenseInfo)null);
         } else {
            LicenseInfo licenseInfo = new LicenseInfo();
            licenseInfo.setLicenseKey(licenseKey);
            licenseInfo.setUsername("Offline User");
            licenseInfo.setPlan("Offline");
            licenseInfo.setExpiryTime(System.currentTimeMillis() + -1702967296L);
            licenseInfo.setHardwareId(hwid);
            licenseInfo.setOffline(true);
            this.licenseCache.put(licenseKey, licenseInfo);
            this.currentLicense = licenseKey;
            this.offlineMode = true;
            this.saveLicenseCache();
            return new LicenseValidationResult(true, "Offline license valid", licenseInfo);
         }
      } catch (Exception e) {
         return new LicenseValidationResult(false, "Offline validation failed: " + e.getMessage(), (LicenseInfo)null);
      }
   }

   public String generateOfflineLicense() {
      try {
         String hwid = SecurityUtils.generateHardwareFingerprint();
         return this.generateOfflineLicense(hwid);
      } catch (Exception var2) {
         return "OFFLINE_ERROR";
      }
   }

   private String generateOfflineLicense(String hwid) throws Exception {
      String base = "OFFLINE_" + hwid + "_" + System.getProperty("user.name");
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
      StringBuilder license = new StringBuilder();

      for(int i = 0; i < 16; ++i) {
         license.append(String.format("%02X", hash[i]));
      }

      return license.toString();
   }

   private LicenseValidationResult simulateOnlineValidation(String licenseKey) {
      try {
         if (SecurityUtils.isValidLicenseFormat(licenseKey)) {
            LicenseInfo licenseInfo = new LicenseInfo();
            licenseInfo.setLicenseKey(licenseKey);
            licenseInfo.setUsername("Demo User");
            licenseInfo.setPlan("Premium");
            licenseInfo.setExpiryTime(System.currentTimeMillis() + 1471228928L);
            licenseInfo.setHardwareId(SecurityUtils.generateHardwareFingerprint());
            licenseInfo.setOffline(false);
            this.licenseCache.put(licenseKey, licenseInfo);
            this.currentLicense = licenseKey;
            this.saveLicenseCache();
            return new LicenseValidationResult(true, "License validated successfully", licenseInfo);
         } else {
            return new LicenseValidationResult(false, "Invalid license format", (LicenseInfo)null);
         }
      } catch (Exception e) {
         return new LicenseValidationResult(false, "Online validation failed: " + e.getMessage(), (LicenseInfo)null);
      }
   }

   public boolean isLicenseValid() {
      if (this.currentLicense.isEmpty()) {
         return false;
      } else {
         LicenseInfo info = (LicenseInfo)this.licenseCache.get(this.currentLicense);
         if (info == null) {
            return false;
         } else {
            return System.currentTimeMillis() < info.getExpiryTime();
         }
      }
   }

   public LicenseInfo getCurrentLicenseInfo() {
      return this.currentLicense.isEmpty() ? null : (LicenseInfo)this.licenseCache.get(this.currentLicense);
   }

   public long getLicenseExpiryTime() {
      LicenseInfo info = this.getCurrentLicenseInfo();
      return info != null ? info.getExpiryTime() : 0L;
   }

   public boolean isLicenseExpired() {
      return System.currentTimeMillis() > this.getLicenseExpiryTime();
   }

   public int getDaysUntilExpiry() {
      long expiryTime = this.getLicenseExpiryTime();
      if (expiryTime == 0L) {
         return -1;
      } else {
         long days = (expiryTime - System.currentTimeMillis()) / 86400000L;
         return Math.max(0, (int)days);
      }
   }

   public void clearLicense() {
      this.currentLicense = "";
      this.offlineMode = false;
      this.saveConfig();
   }

   public void enableOfflineMode() {
      this.offlineMode = true;
      this.currentLicense = "OFFLINE_MODE_ENABLED";
      this.saveConfig();
   }

   public boolean isOfflineMode() {
      return this.offlineMode;
   }

   private void loadConfig() {
      try {
         File configFile = new File("phantom.license.config");
         if (configFile.exists()) {
            FileInputStream fis = new FileInputStream(configFile);

            try {
               this.config.load(fis);
            } catch (Throwable var6) {
               try {
                  fis.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }

               throw var6;
            }

            fis.close();
            this.currentLicense = this.config.getProperty("currentLicense", "");
            this.offlineMode = Boolean.parseBoolean(this.config.getProperty("offlineMode", "false"));
         }
      } catch (Exception e) {
         System.err.println("[LicenseManager] Failed to load config: " + e.getMessage());
      }

   }

   private void saveConfig() {
      try {
         this.config.setProperty("currentLicense", this.currentLicense);
         this.config.setProperty("offlineMode", String.valueOf(this.offlineMode));
         FileOutputStream fos = new FileOutputStream("phantom.license.config");

         try {
            this.config.store(fos, "Krypton License Configuration");
         } catch (Throwable var5) {
            try {
               fos.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         fos.close();
      } catch (Exception e) {
         System.err.println("[LicenseManager] Failed to save config: " + e.getMessage());
      }

   }

   private void loadLicenseCache() {
      try {
         File cacheFile = new File("phantom.license.cache");
         if (cacheFile.exists()) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cacheFile));

            try {
               Map<String, LicenseInfo> cached = (Map)ois.readObject();
               long currentTime = System.currentTimeMillis();

               for(Map.Entry entry : cached.entrySet()) {
                  if (((LicenseInfo)entry.getValue()).getExpiryTime() > currentTime) {
                     this.licenseCache.put((String)entry.getKey(), (LicenseInfo)entry.getValue());
                  }
               }
            } catch (Throwable var9) {
               try {
                  ois.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            ois.close();
         }
      } catch (Exception e) {
         System.err.println("[LicenseManager] Failed to load license cache: " + e.getMessage());
      }

   }

   private void saveLicenseCache() {
      try {
         ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("phantom.license.cache"));

         try {
            oos.writeObject(this.licenseCache);
         } catch (Throwable var5) {
            try {
               oos.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         oos.close();
      } catch (Exception e) {
         System.err.println("[LicenseManager] Failed to save license cache: " + e.getMessage());
      }

   }

   public void clearLicenseCache() {
      this.licenseCache.clear();

      try {
         File cacheFile = new File("phantom.license.cache");
         if (cacheFile.exists()) {
            cacheFile.delete();
         }
      } catch (Exception e) {
         System.err.println("[LicenseManager] Failed to clear license cache: " + e.getMessage());
      }

   }

   public static class LicenseValidationResult {
      private final boolean valid;
      private final String message;
      private final LicenseInfo licenseInfo;

      public LicenseValidationResult(boolean valid, String message, LicenseInfo licenseInfo) {
         this.valid = valid;
         this.message = message;
         this.licenseInfo = licenseInfo;
      }

      public boolean isValid() {
         return this.valid;
      }

      public String getMessage() {
         return this.message;
      }

      public LicenseInfo getLicenseInfo() {
         return this.licenseInfo;
      }
   }

   public static class LicenseInfo implements Serializable {
      private static final long serialVersionUID = 1L;
      private String licenseKey;
      private String username;
      private String plan;
      private long expiryTime;
      private String hardwareId;
      private boolean offline;

      public String getLicenseKey() {
         return this.licenseKey;
      }

      public void setLicenseKey(String licenseKey) {
         this.licenseKey = licenseKey;
      }

      public String getUsername() {
         return this.username;
      }

      public void setUsername(String username) {
         this.username = username;
      }

      public String getPlan() {
         return this.plan;
      }

      public void setPlan(String plan) {
         this.plan = plan;
      }

      public long getExpiryTime() {
         return this.expiryTime;
      }

      public void setExpiryTime(long expiryTime) {
         this.expiryTime = expiryTime;
      }

      public String getHardwareId() {
         return this.hardwareId;
      }

      public void setHardwareId(String hardwareId) {
         this.hardwareId = hardwareId;
      }

      public boolean isOffline() {
         return this.offline;
      }

      public void setOffline(boolean offline) {
         this.offline = offline;
      }

      public String toString() {
         return "LicenseInfo{licenseKey='" + this.licenseKey + "', username='" + this.username + "', plan='" + this.plan + "', expiryTime=" + this.expiryTime + ", hardwareId='" + this.hardwareId + "', offline=" + this.offline + "}";
      }
   }
}
