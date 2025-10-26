package dev.gambleclient.auth;

public class UserData {
   private String username;
   private String email;
   private String licenseKey;
   private String expiryDate;
   private String licenseType;

   public String getUsername() {
      return this.username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getEmail() {
      return this.email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   public String getLicenseKey() {
      return this.licenseKey;
   }

   public void setLicenseKey(String licenseKey) {
      this.licenseKey = licenseKey;
   }

   public String getExpiryDate() {
      return this.expiryDate;
   }

   public void setExpiryDate(String expiryDate) {
      this.expiryDate = expiryDate;
   }

   public String getLicenseType() {
      return this.licenseType;
   }

   public void setLicenseType(String licenseType) {
      this.licenseType = licenseType;
   }

   public boolean isLicenseValid() {
      if (this.licenseType != null && this.licenseType.equalsIgnoreCase("Lifetime")) {
         return true;
      } else {
         return this.expiryDate != null && !"N/A".equals(this.expiryDate);
      }
   }
}
