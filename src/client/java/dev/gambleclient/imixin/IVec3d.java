package dev.gambleclient.imixin;

import net.minecraft.util.math.Vec3i;
import org.joml.Vector3d;

public interface IVec3d {
   void set(double var1, double var3, double var5);

   default void a(Vec3i vec3i) {
      this.set((double)vec3i.getX(), (double)vec3i.getY(), (double)vec3i.getZ());
   }

   default void a(Vector3d vector3d) {
      this.set(vector3d.x, vector3d.y, vector3d.z);
   }

   void setXZ(double var1, double var3);

   void setY(double var1);
}
