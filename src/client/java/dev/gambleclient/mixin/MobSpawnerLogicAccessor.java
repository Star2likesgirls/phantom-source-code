package dev.gambleclient.mixin;

import net.minecraft.block.spawner.MobSpawnerLogic;
import net.minecraft.block.spawner.MobSpawnerEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({MobSpawnerLogic.class})
public interface MobSpawnerLogicAccessor {
   @Accessor("spawnEntry")
   MobSpawnerEntry getSpawnEntry();
}
