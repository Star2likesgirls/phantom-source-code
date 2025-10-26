package dev.gambleclient.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecurityUtils {
   private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
   private static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
   private static final int AES_KEY_SIZE = 256;
   private static final int GCM_IV_LENGTH = 12;
   private static final int GCM_TAG_LENGTH = 16;
   private static final SecureRandom secureRandom = new SecureRandom();

   public static SecretKey generateAESKey() throws Exception {
      KeyGenerator keyGen = KeyGenerator.getInstance("AES");
      keyGen.init(256, secureRandom);
      return keyGen.generateKey();
   }

   public static KeyPair generateRSAKeyPair() throws Exception {
      KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
      keyGen.initialize(2048, secureRandom);
      return keyGen.generateKeyPair();
   }

   public static byte[] encryptAES(byte[] data, SecretKey key) throws Exception {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      byte[] iv = new byte[12];
      secureRandom.nextBytes(iv);
      GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
      cipher.init(1, key, gcmSpec);
      byte[] encrypted = cipher.doFinal(data);
      byte[] result = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, result, 0, iv.length);
      System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
      return result;
   }

   public static byte[] decryptAES(byte[] encryptedData, SecretKey key) throws Exception {
      if (encryptedData.length < 12) {
         throw new IllegalArgumentException("Encrypted data too short");
      } else {
         byte[] iv = Arrays.copyOfRange(encryptedData, 0, 12);
         byte[] encrypted = Arrays.copyOfRange(encryptedData, 12, encryptedData.length);
         Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
         GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
         cipher.init(2, key, gcmSpec);
         return cipher.doFinal(encrypted);
      }
   }

   public static String encryptString(String data, SecretKey key) throws Exception {
      byte[] encrypted = encryptAES(data.getBytes(StandardCharsets.UTF_8), key);
      return Base64.getEncoder().encodeToString(encrypted);
   }

   public static String decryptString(String encryptedData, SecretKey key) throws Exception {
      byte[] encrypted = Base64.getDecoder().decode(encryptedData);
      byte[] decrypted = decryptAES(encrypted, key);
      return new String(decrypted, StandardCharsets.UTF_8);
   }

   public static byte[] encryptRSA(byte[] data, PublicKey publicKey) throws Exception {
      Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      cipher.init(1, publicKey);
      return cipher.doFinal(data);
   }

   public static byte[] decryptRSA(byte[] encryptedData, PrivateKey privateKey) throws Exception {
      Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      cipher.init(2, privateKey);
      return cipher.doFinal(encryptedData);
   }

   public static byte[] sign(byte[] data, PrivateKey privateKey) throws Exception {
      Signature signature = Signature.getInstance("SHA256withRSA");
      signature.initSign(privateKey);
      signature.update(data);
      return signature.sign();
   }

   public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey) throws Exception {
      Signature sig = Signature.getInstance("SHA256withRSA");
      sig.initVerify(publicKey);
      sig.update(data);
      return sig.verify(signature);
   }

   public static String generateRandomString(int length) {
      String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
      StringBuilder sb = new StringBuilder(length);

      for(int i = 0; i < length; ++i) {
         sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
      }

      return sb.toString();
   }

   public static String generateRandomHex(int length) {
      StringBuilder sb = new StringBuilder(length);

      for(int i = 0; i < length; ++i) {
         sb.append(String.format("%02x", secureRandom.nextInt(256)));
      }

      return sb.toString().toUpperCase();
   }

   public static String hashSHA256(String data) throws Exception {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(hash);
   }

   public static String hashSHA512(String data) throws Exception {
      MessageDigest digest = MessageDigest.getInstance("SHA-512");
      byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(hash);
   }

   public static String bytesToHex(byte[] bytes) {
      StringBuilder sb = new StringBuilder();

      for(byte b : bytes) {
         sb.append(String.format("%02x", b));
      }

      return sb.toString();
   }

   public static byte[] hexToBytes(String hex) {
      int len = hex.length();
      byte[] data = new byte[len / 2];

      for(int i = 0; i < len; i += 2) {
         data[i / 2] = (byte)((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
      }

      return data;
   }

   public static void saveKey(Key key, String filename) throws Exception {
      FileOutputStream fos = new FileOutputStream(filename);

      try {
         fos.write(key.getEncoded());
      } catch (Throwable var6) {
         try {
            fos.close();
         } catch (Throwable var5) {
            var6.addSuppressed(var5);
         }

         throw var6;
      }

      fos.close();
   }

   public static SecretKey loadAESKey(String filename) throws Exception {
      FileInputStream fis = new FileInputStream(filename);

      SecretKeySpec var3;
      try {
         byte[] keyBytes = fis.readAllBytes();
         var3 = new SecretKeySpec(keyBytes, "AES");
      } catch (Throwable var5) {
         try {
            fis.close();
         } catch (Throwable var4) {
            var5.addSuppressed(var4);
         }

         throw var5;
      }

      fis.close();
      return var3;
   }

   public static PrivateKey loadRSAPrivateKey(String filename) throws Exception {
      FileInputStream fis = new FileInputStream(filename);

      PrivateKey var5;
      try {
         byte[] keyBytes = fis.readAllBytes();
         PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
         KeyFactory factory = KeyFactory.getInstance("RSA");
         var5 = factory.generatePrivate(spec);
      } catch (Throwable var7) {
         try {
            fis.close();
         } catch (Throwable var6) {
            var7.addSuppressed(var6);
         }

         throw var7;
      }

      fis.close();
      return var5;
   }

   public static PublicKey loadRSAPublicKey(String filename) throws Exception {
      FileInputStream fis = new FileInputStream(filename);

      PublicKey var5;
      try {
         byte[] keyBytes = fis.readAllBytes();
         X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
         KeyFactory factory = KeyFactory.getInstance("RSA");
         var5 = factory.generatePublic(spec);
      } catch (Throwable var7) {
         try {
            fis.close();
         } catch (Throwable var6) {
            var7.addSuppressed(var6);
         }

         throw var7;
      }

      fis.close();
      return var5;
   }

   public static String obfuscateString(String input) {
      if (input != null && !input.isEmpty()) {
         byte[] key = new byte[16];
         secureRandom.nextBytes(key);
         byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
         byte[] output = new byte[inputBytes.length + 16];
         System.arraycopy(key, 0, output, 0, 16);

         for(int i = 0; i < inputBytes.length; ++i) {
            output[i + 16] = (byte)(inputBytes[i] ^ key[i % key.length]);
         }

         return Base64.getEncoder().encodeToString(output);
      } else {
         return input;
      }
   }

   public static String deobfuscateString(String obfuscated) {
      if (obfuscated != null && !obfuscated.isEmpty()) {
         try {
            byte[] data = Base64.getDecoder().decode(obfuscated);
            if (data.length < 16) {
               return obfuscated;
            } else {
               byte[] key = Arrays.copyOfRange(data, 0, 16);
               byte[] encrypted = Arrays.copyOfRange(data, 16, data.length);
               byte[] decrypted = new byte[encrypted.length];

               for(int i = 0; i < encrypted.length; ++i) {
                  decrypted[i] = (byte)(encrypted[i] ^ key[i % key.length]);
               }

               return new String(decrypted, StandardCharsets.UTF_8);
            }
         } catch (Exception var6) {
            return obfuscated;
         }
      } else {
         return obfuscated;
      }
   }

   public static String generateHardwareFingerprint() {
      try {
         StringBuilder fingerprint = new StringBuilder();
         fingerprint.append(System.getProperty("os.name"));
         fingerprint.append(System.getProperty("os.version"));
         fingerprint.append(System.getProperty("os.arch"));
         fingerprint.append(System.getProperty("user.name"));
         fingerprint.append(System.getProperty("user.home"));
         fingerprint.append(Runtime.getRuntime().availableProcessors());
         fingerprint.append(Runtime.getRuntime().maxMemory());
         NetworkInterface.getNetworkInterfaces().asIterator().forEachRemaining((networkInterface) -> {
            try {
               if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                  byte[] mac = networkInterface.getHardwareAddress();
                  if (mac != null) {
                     for(byte b : mac) {
                        fingerprint.append(String.format("%02X", b));
                     }
                  }
               }
            } catch (Exception var7) {
            }

         });
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] hash = digest.digest(fingerprint.toString().getBytes(StandardCharsets.UTF_8));
         return bytesToHex(hash).substring(0, 32).toUpperCase();
      } catch (Exception var3) {
         String var10000 = System.getProperty("user.name");
         return "HWID_" + var10000 + "_" + System.getProperty("os.name").replaceAll("\\s+", "") + "_" + System.currentTimeMillis();
      }
   }

   public static boolean isValidLicenseFormat(String license) {
      return license != null && license.length() == 32 ? license.matches("^[A-F0-9]{32}$") : false;
   }

   public static String generateLicenseKey() {
      return generateRandomHex(32);
   }

   public static String createChecksum(byte[] data) throws Exception {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(data);
      return Base64.getEncoder().encodeToString(hash);
   }

   public static boolean verifyChecksum(byte[] data, String expectedChecksum) throws Exception {
      String actualChecksum = createChecksum(data);
      return actualChecksum.equals(expectedChecksum);
   }

   public static boolean secureStringEquals(String a, String b) {
      if (a != null && b != null) {
         if (a.length() != b.length()) {
            return false;
         } else {
            int result = 0;

            for(int i = 0; i < a.length(); ++i) {
               result |= a.charAt(i) ^ b.charAt(i);
            }

            return result == 0;
         }
      } else {
         return a == b;
      }
   }

   public static void clearSensitiveData(byte[] data) {
      if (data != null) {
         Arrays.fill(data, (byte)0);
      }

   }

   public static void clearSensitiveData(String data) {
      if (data != null) {
         String var1 = null;
      }

   }
}
