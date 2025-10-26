package dev.gambleclient.utils.meteorrejects;

import dev.gambleclient.mixin.CountPlacementModifierAccessor;
import dev.gambleclient.mixin.HeightRangePlacementModifierAccessor;
import dev.gambleclient.mixin.RarityFilterPlacementModifierAccessor;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.feature.ScatteredOreFeature;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.gen.heightprovider.HeightProvider;
import net.minecraft.world.gen.placementmodifier.CountPlacementModifier;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.RarityFilterPlacementModifier;
import net.minecraft.world.gen.feature.OrePlacedFeatures;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryKeys;

public class Ore {
   public int a;
   public int b;
   public IntProvider c = ConstantIntProvider.create(1);
   public HeightProvider d;
   public HeightContext e;
   public float f = 1.0F;
   public float g;
   public int h;
   public Color i;
   public boolean j;

   public static Map register() {
      RegistryWrapper.WrapperLookup wrapperLookup = BuiltinRegistries.createWrapperLookup();
      RegistryWrapper.Impl<PlacedFeature> impl = wrapperLookup.getWrapperOrThrow(RegistryKeys.PLACED_FEATURE);
      List<RegistryEntry<Biome>> l = ((DimensionOptions)((WorldPreset)wrapperLookup.getWrapperOrThrow(RegistryKeys.WORLD_PRESET).getOrThrow(WorldPresets.DEFAULT).value()).createDimensionsRegistryHolder().dimensions().get(DimensionOptions.NETHER)).chunkGenerator().getBiomeSource().getBiomes().stream().toList();
      List<PlacedFeatureIndexer.IndexedFeatures> l2 = PlacedFeatureIndexer.collectIndexedFeatures(l, (registryEntry) -> ((Biome)registryEntry.value()).getGenerationSettings().getFeatures(), true);
      Map<PlacedFeature, Ore> ores = new HashMap();
      RegistryKey<PlacedFeature> registry = OrePlacedFeatures.ORE_DEBRIS_SMALL;
      register(ores, l2, impl, registry, 7, new Color(209, 27, 245));
      RegistryKey<PlacedFeature> registryKey2 = OrePlacedFeatures.ORE_ANCIENT_DEBRIS_LARGE;
      register(ores, l2, impl, registryKey2, 7, new Color(209, 27, 245));
      Map<RegistryKey<Biome>, List<Ore>> hashMap2 = new HashMap();
      l.forEach((registryEntry) -> {
         hashMap2.put((RegistryKey)registryEntry.getKey().get(), new ArrayList());
         Stream<PlacedFeature> stream = ((Biome)registryEntry.value()).getGenerationSettings().getFeatures().stream().flatMap(RegistryEntryList::stream).map(RegistryEntry::value);
         Objects.requireNonNull(ores);
         Objects.requireNonNull(ores);
         stream.filter(ores::containsKey).forEach((placedFeature) -> ((List)hashMap2.get(registryEntry.getKey().get())).add((Ore)ores.get(placedFeature)));
      });
      return hashMap2;
   }

   private static void register(Map map, List indexer, RegistryWrapper.Impl oreRegistry, RegistryKey oreKey, int genStep, Color color) {
      PlacedFeature orePlacement = (PlacedFeature)oreRegistry.getOrThrow(oreKey).value();
      int index = ((PlacedFeatureIndexer.IndexedFeatures)indexer.get(genStep)).indexMapping().applyAsInt(orePlacement);
      Ore ore = new Ore(orePlacement, genStep, index, color);
      map.put(orePlacement, ore);
   }

   private Ore(PlacedFeature obj, int a, int b, Color i) {
      this.a = a;
      this.b = b;
      this.i = i;
      this.e = new HeightContext((ChunkGenerator)null, HeightLimitView.create(MinecraftClient.getInstance().world.getBottomY(), MinecraftClient.getInstance().world.getDimension().logicalHeight()));

      for(Object next : obj.placementModifiers()) {
         if (next instanceof CountPlacementModifier) {
            this.c = ((CountPlacementModifierAccessor)next).getCount();
         } else if (next instanceof HeightRangePlacementModifier) {
            this.d = ((HeightRangePlacementModifierAccessor)next).getHeight();
         } else if (next instanceof RarityFilterPlacementModifier) {
            this.f = (float)((RarityFilterPlacementModifierAccessor)next).getChance();
         }
      }

      FeatureConfig config = ((ConfiguredFeature)obj.feature().value()).config();
      if (config instanceof OreFeatureConfig) {
         this.g = ((OreFeatureConfig)config).discardOnAirChance;
         this.h = ((OreFeatureConfig)config).size;
         if (((ConfiguredFeature)obj.feature().value()).feature() instanceof ScatteredOreFeature) {
            this.j = true;
         }

      } else {
         throw new IllegalStateException("config for " + String.valueOf(obj) + "is not OreFeatureConfig.class");
      }
   }

   private static byte[] dwyrwvxcbpxjhuh() {
      return new byte[]{40, 4, 103, 33, 11, 101, 15, 97, 99, 53, 50, 44, 91, 16, 69, 71, 114, 86, 103, 108, 27, 65};
   }
}
