package dev.gambleclient.mixin;

import net.minecraft.client.render.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Frustum.class})
public interface FrustumAccessor {
   @Accessor("x")
   double getX();

   @Accessor("x")
   void setX(double var1);

   @Accessor("y")
   double getY();

   @Accessor("y")
   void setY(double var1);

   @Accessor("z")
   double getZ();

   @Accessor("z")
   void setZ(double var1);
}
