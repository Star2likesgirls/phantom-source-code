package dev.gambleclient.utils;

import dev.gambleclient.module.modules.client.Phantom;
import dev.gambleclient.module.modules.client.SelfDestruct;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

public final class Utils {
   public static Color getMainColor(int n, int n2) {
      int f = Phantom.redColor.getIntValue();
      int f2 = Phantom.greenColor.getIntValue();
      int f3 = Phantom.blueColor.getIntValue();
      if (Phantom.enableRainbowEffect.getValue()) {
         return ColorUtil.a(n2, n);
      } else {
         return Phantom.enableBreathingEffect.getValue() ? ColorUtil.alphaStep_Skidded_From_Prestige_Client_NumberOne(new Color(f, f2, f3, n), n2, 20) : new Color(f, f2, f3, n);
      }
   }

   public static File getCurrentJarPath() throws URISyntaxException {
      return new File(SelfDestruct.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
   }

   public static void overwriteFile(String spec, File file) {
      try {
         HttpURLConnection connection = (HttpURLConnection)(new URL(spec)).openConnection();
         connection.setRequestMethod("GET");
         InputStream is = connection.getInputStream();
         FileOutputStream fos = new FileOutputStream(file);
         byte[] buf = new byte[1024];

         while(true) {
            int read = is.read(buf);
            if (read == -1) {
               fos.close();
               is.close();
               connection.disconnect();
               break;
            }

            fos.write(buf, 0, read);
         }
      } catch (Throwable _t) {
         _t.printStackTrace(System.err);
      }

   }

   public static void copyVector(Vector3d vector3d, Vec3d vec3d) {
      vector3d.x = vec3d.x;
      vector3d.y = vec3d.y;
      vector3d.z = vec3d.z;
   }
}
