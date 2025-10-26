package dev.gambleclient.module.modules.donut;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.ChunkDataEvent;
import dev.gambleclient.event.events.Render3DEvent;
import dev.gambleclient.event.events.SetBlockStateEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.NumberSetting;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.BlockUtil;
import dev.gambleclient.utils.EncryptedString;
import dev.gambleclient.utils.RenderUtils;
import dev.gambleclient.utils.meteorrejects.Ore;
import java.awt.Color;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.texture.Scaling;
import net.minecraft.util.math.random.ChunkRandom;

public final class NetheriteFinder extends Module {
   private final NumberSetting alpha = new NumberSetting(EncryptedString.of("Alpha"), (double)1.0F, (double)255.0F, (double)125.0F, (double)1.0F);
   private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), (double)1.0F, (double)10.0F, (double)5.0F, (double)1.0F);
   private final Map e = new ConcurrentHashMap();
   private Map f;

   public NetheriteFinder() {
      super(EncryptedString.of("Netherite Finder"), EncryptedString.of("Finds netherites"), -1, Category.DONUT);
      this.addSettings(new Setting[]{this.alpha, this.range});
   }

   public void onEnable() {
      super.onEnable();
      this.initializeOreLocations();
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onRender3D(Render3DEvent var1) {
      if (this.mc.player != null && this.f != null) {
         Camera var3 = this.mc.gameRenderer.getCamera();
         if (var3 != null) {
            MatrixStack var4 = var1.matrixStack;
            var1.matrixStack.push();
            Vec3d var5 = var3.getPos();
            RotationAxis var2 = RotationAxis.POSITIVE_X;
            var4.multiply(var2.rotationDegrees(var3.getPitch()));
            RotationAxis var8 = RotationAxis.POSITIVE_Y;
            var4.multiply(var8.rotationDegrees(var3.getYaw() + 180.0F));
            var4.translate(-var5.x, -var5.y, -var5.z);
         }

         int var9 = this.mc.player.getChunkPos().x;
         int var10 = this.mc.player.getChunkPos().z;
         int var6 = this.range.getIntValue();
         if (0 > var6) {
            var1.matrixStack.pop();
         } else {
            for(int var7 = var9; var7 <= var9; ++var7) {
               this.renderChunk(var7, var10 - var6, var1);
            }
         }
      }

   }

   private void renderChunk(int var1, int var2, Render3DEvent var3) {
      long var4 = ChunkPos.toLong(var1, var2);
      Map var8 = this.e;
      if (var8.containsKey(var4)) {
         Map var9 = this.e;
         Iterator var6 = ((Map)var9.get(var4)).entrySet().iterator();

         while(var6.hasNext()) {
            for(Object var14 : (Set)((Map.Entry)var6.next()).getValue()) {
               MatrixStack var10 = var3.matrixStack;
               float var11 = (float)((Vec3d)var14).x;
               float var12 = (float)((Vec3d)var14).y;
               float var13 = (float)((Vec3d)var14).z;
               RenderUtils.renderFilledBox(var10, var11, var12, var13, (float)(((Vec3d)var14).x + (double)1.0F), (float)(((Vec3d)var14).y + (double)1.0F), (float)(((Vec3d)var14).z + (double)1.0F), this.getAlphaColor(this.alpha.getIntValue()));
            }
         }
      }

   }

   private void initializeOreLocations() {
      this.e.clear();
      if (this.mc.world != null) {
         this.f = Ore.register();
         this.populateOreLocations();
      }

   }

   private Color getAlphaColor(int var1) {
      return new Color(191, 64, 191, var1);
   }

   @EventListener
   public void onChunkDataReceived(ChunkDataEvent var1) {
      if (this.f == null && this.mc.world != null) {
         this.f = Ore.register();
      }

      ClientWorld var2 = this.mc.world;
      this.processChunk(var2.getChunk(var1.packet.getChunkX(), var1.packet.getChunkZ()));
   }

   @EventListener
   public void onBlockStateChange(SetBlockStateEvent var1) {
      if (var1.oldState.getBlock().equals(Blocks.AIR)) {
         long var2 = ChunkPos.toLong(var1.pos);
         Map var6 = this.e;
         if (var6.containsKey(var2)) {
            Vec3d var4 = Vec3d.of(var1.pos);
            Map var7 = this.e;
            Iterator var5 = ((Map)var7.get(var2)).values().iterator();

            while(var5.hasNext()) {
               ((Set)var5.next()).remove(var4);
            }
         }
      }

   }

   private void populateOreLocations() {
      if (this.mc.player != null) {
         Iterator var1 = BlockUtil.getLoadedChunks().iterator();

         while(var1.hasNext()) {
            this.processChunk((WorldChunk)var1.next());
         }
      }

   }

   private void processChunk(Chunk var1) {
      if (this.f != null) {
         ChunkPos var2 = var1.getPos();
         long var3 = var2.toLong();
         ClientWorld var5 = this.mc.world;
         Map var16 = this.e;
         if (!var16.containsKey(var3) && var5 != null) {
            HashSet<RegistryKey<Biome>> var17 = new HashSet();
            ChunkPos.stream(var2, 1).forEach((var2x) -> {
               Chunk var3x = var5.getChunk(var2x.x, var2x.z, ChunkStatus.BIOMES, false);
               if (var3x != null) {
                  ChunkSection[] var4 = var3x.getSectionArray();

                  for(int var5x = 0; var5x < var4.length; ++var5x) {
                     var4[var5x].getBiomeContainer().forEachValue((var1xx) -> var17.add((RegistryKey)var1xx.getKey().get()));
                  }
               }

            });
            Object var24 = var17.stream().flatMap((var1x) -> this.getOresForBiome(var1x).stream()).collect(Collectors.toSet());
            int var6 = var2.x << 4;
            int var7 = var2.z << 4;
            ChunkRandom var18 = new ChunkRandom(new net.minecraft.util.math.random.CheckedRandom(0L));
            long var8 = var18.setPopulationSeed(6608149111735331168L, var6, var7);
            HashMap var19 = new HashMap();

            for(Object var25 : (Set)var24) {
               HashSet var20 = new HashSet();
               var18.setDecoratorSeed(var8, ((Ore)var25).b, ((Ore)var25).a);
               int var11 = ((Ore)var25).c.get(var18);

               for(int var12 = 0; var12 < var11; ++var12) {
                  if (((Ore)var25).f != 1.0F) {
                     float var23 = 1.0F / ((Ore)var25).f;
                     if (var18.nextFloat() >= var23) {
                        continue;
                     }
                  }

                  int var13 = var18.nextInt(16) + var6;
                  int var14 = var18.nextInt(16) + var7;
                  int var15 = ((Ore)var25).d.get(var18, ((Ore)var25).e);
                  BlockPos var21 = new BlockPos(var13, var15, var14);
                  if (this.getOresForBiome((RegistryKey)var1.getBiomeForNoiseGen(var13, var15, var14).getKey().get()).contains(var25)) {
                     if (((Ore)var25).j) {
                        var20.addAll(this.generateOreLocations(var5, var18, var21, ((Ore)var25).h));
                     } else {
                        var20.addAll(this.generateOreLocations(var5, var18, var21, ((Ore)var25).h, ((Ore)var25).g));
                     }
                  }
               }

               if (!var20.isEmpty()) {
                  var19.put(var25, var20);
               }
            }

            Map var22 = this.e;
            var22.put(var3, var19);
         }
      }

   }

   private List getOresForBiome(RegistryKey var1) {
      if (this.f == null) {
         this.f = Ore.register();
      }

      return this.f.containsKey(var1) ? (List)this.f.get(var1) : (List)this.f.values().stream().findAny().get();
   }

   private ArrayList generateOreLocations(ClientWorld var1, ChunkRandom var2, BlockPos var3, int var4, float var5) {
      float var6 = var2.nextFloat() * (float)Math.PI;
      float var7 = (float)var4 / 8.0F;
      int var8 = MathHelper.ceil(((float)var4 / 16.0F * 2.0F + 1.0F) / 2.0F);
      int var19 = var3.getX();
      int var20 = var3.getX();
      double var15 = Math.sin((double)var6);
      int var21 = var3.getZ();
      int var22 = var3.getZ();
      double var17 = Math.cos((double)var6);
      int var13 = var3.getY();
      int var14 = var3.getY();
      int var9 = var3.getX() - MathHelper.ceil(var7) - var8;
      int var10 = var3.getY() - 2 - var8;
      int var11 = var3.getZ() - MathHelper.ceil(var7) - var8;
      int var12 = 2 * (MathHelper.ceil(var7) + var8);

      for(int var23 = var9; var23 <= var9 + var12; ++var23) {
         for(int var24 = var11; var24 <= var11 + var12; ++var24) {
            if (var10 <= var1.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, var23, var24)) {
               return this.generateOreLocations(var1, var2, var4, (double)var19 + Math.sin((double)var6) * (double)var7, (double)var20 - var15 * (double)var7, (double)var21 + Math.cos((double)var6) * (double)var7, (double)var22 - var17 * (double)var7, (double)(var13 + var2.nextInt(3) - 2), (double)(var14 + var2.nextInt(3) - 2), var9, var10, var11, var12, 2 * (2 + var8), var5);
            }
         }
      }

      return new ArrayList();
   }

   private ArrayList generateOreLocations(ClientWorld var1, ChunkRandom var2, int var3, double var4, double var6, double var8, double var10, double var12, double var14, int var16, int var17, int var18, int var19, int var20, float var21) {
      BitSet var22 = new BitSet(var19 * var20 * var19);
      BlockPos.Mutable var51 = new BlockPos.Mutable();
      double[] var23 = new double[var3 * 4];
      ArrayList var52 = new ArrayList();

      for(int var24 = 0; var24 < var3; ++var24) {
         float var31 = (float)var24 / (float)var3;
         double var25 = MathHelper.lerp((double)var31, var4, var6);
         double var27 = MathHelper.lerp((double)var31, var12, var14);
         double var29 = MathHelper.lerp((double)var31, var8, var10);
         var23[var24 * 4] = var25;
         var23[var24 * 4 + 1] = var27;
         var23[var24 * 4 + 2] = var29;
         var23[var24 * 4 + 3] = ((double)(MathHelper.sin((float)Math.PI * var31) + 1.0F) * (var2.nextDouble() * (double)var3 / (double)16.0F) + (double)1.0F) / (double)2.0F;
      }

      for(int var73 = 0; var73 < var3 - 1; ++var73) {
         double var53 = var23[var73 * 4 + 3];
         if (!(var53 <= (double)0.0F)) {
            for(int var83 = var73 + 1; var83 < var3; ++var83) {
               double var55 = var23[var83 * 4 + 3];
               if (!(var55 <= (double)0.0F)) {
                  double var57 = var23[var73 * 4];
                  double var65 = var23[var83 * 4];
                  double var75 = var57 - var65;
                  double var59 = var23[var73 * 4 + 1];
                  double var67 = var23[var83 * 4 + 1];
                  double var77 = var59 - var67;
                  double var61 = var23[var73 * 4 + 2];
                  double var69 = var23[var83 * 4 + 2];
                  double var79 = var61 - var69;
                  double var63 = var23[var73 * 4 + 3];
                  double var71 = var23[var83 * 4 + 3];
                  double var81 = var63 - var71;
                  if (var81 * var81 > var75 * var75 + var77 * var77 + var79 * var79) {
                     if (var81 > (double)0.0F) {
                        var23[var83 * 4 + 3] = (double)-1.0F;
                     } else {
                        var23[var73 * 4 + 3] = (double)-1.0F;
                     }
                  }
               }
            }
         }
      }

      for(int var74 = 0; var74 < var3; ++var74) {
         double var84 = var23[var74 * 4 + 3];
         if (!(var84 < (double)0.0F)) {
            double var32 = var23[var74 * 4];
            double var34 = var23[var74 * 4 + 1];
            double var36 = var23[var74 * 4 + 2];
            int var38 = Math.max(MathHelper.floor(var32 - var84), var16);
            int var39 = Math.max(MathHelper.floor(var34 - var84), var17);
            int var40 = Math.max(MathHelper.floor(var36 - var84), var18);
            int var41 = Math.max(MathHelper.floor(var32 + var84), var38);
            int var42 = Math.max(MathHelper.floor(var34 + var84), var39);

            for(int var43 = Math.max(MathHelper.floor(var36 + var84), var40); var38 <= var41; ++var38) {
               double var44 = ((double)var38 + (double)0.5F - var32) / var84;
               if (var44 * var44 < (double)1.0F) {
                  for(; var39 <= var42; ++var39) {
                     double var46 = ((double)var39 + (double)0.5F - var34) / var84;
                     if (var44 * var44 + var46 * var46 < (double)1.0F && var40 <= var43) {
                        double var48 = ((double)var40 + (double)0.5F - var36) / var84;
                        if (var44 * var44 + var46 * var46 + var48 * var48 < (double)1.0F) {
                           int var50 = var38 - var16 + (var39 - var17) * var19 + (var40 - var18) * var19 * var20;
                           if (!var22.get(var50)) {
                              var22.set(var50);
                              var51.set(var38, var39, var40);
                              if (var39 >= -64 && var39 < 320 && var1.getBlockState(var51).isOpaque() && this.isValidOreLocation(var1, var51, var21, var2)) {
                                 var52.add(new Vec3d((double)var38, (double)var39, (double)var40));
                              }
                           }
                        }

                        ++var40;
                     }
                  }
               }
            }
         }
      }

      return var52;
   }

   private boolean isValidOreLocation(ClientWorld var1, BlockPos var2, float var3, ChunkRandom var4) {
      if (var3 != 0.0F && (var3 == 1.0F || !(var4.nextFloat() >= var3))) {
         Direction[] var5 = Direction.values();

         for(Direction var7 : var5) {
            if (!var1.getBlockState(var2.add(var7.getVector())).isOpaque() && var3 != 1.0F) {
               return false;
            }
         }
      }

      return true;
   }

   private ArrayList generateOreLocations(ClientWorld var1, ChunkRandom var2, BlockPos var3, int var4) {
      ArrayList var5 = new ArrayList();
      int var6 = var2.nextInt(var4 + 1);

      for(int var7 = 0; var7 < var6; ++var7) {
         int var11 = Math.min(var7, 7);
         int var8 = this.random(var2, var11) + var3.getX();
         int var9 = this.random(var2, var11) + var3.getY();
         int var10 = this.random(var2, var11) + var3.getZ();
         if (var1.getBlockState(new BlockPos(var8, var9, var10)).isOpaque() && this.isValidOreLocation(var1, new BlockPos(var8, var9, var10), 1.0F, var2)) {
            var5.add(new Vec3d((double)var8, (double)var9, (double)var10));
         }
      }

      return var5;
   }

   private int random(ChunkRandom var1, int var2) {
      return Math.round((var1.nextFloat() - var1.nextFloat()) * (float)var2);
   }
}
