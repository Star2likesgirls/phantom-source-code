package dev.gambleclient.module.modules.donut;

import dev.gambleclient.event.EventListener;
import dev.gambleclient.event.events.EntitySpawnEvent;
import dev.gambleclient.module.Category;
import dev.gambleclient.module.Module;
import dev.gambleclient.module.setting.Setting;
import dev.gambleclient.utils.EncryptedString;
import java.util.ArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Entity;

public final class AntiTrap extends Module {
   public AntiTrap() {
      super(EncryptedString.of("Anti Trap"), EncryptedString.of("Module that helps you escape Polish traps"), -1, Category.DONUT);
      this.addSettings(new Setting[0]);
   }

   public void onEnable() {
      this.removeTrapEntities();
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @EventListener
   public void onEntitySpawn(EntitySpawnEvent entitySpawnEvent) {
      if (this.isTrapEntity(entitySpawnEvent.packet.getEntityType())) {
         entitySpawnEvent.cancel();
      }

   }

   private void removeTrapEntities() {
      if (this.mc.world != null) {
         ArrayList<Entity> trapEntities = new ArrayList();
         this.mc.world.getEntities().forEach((entity) -> {
            if (entity != null && this.isTrapEntity(entity.getType())) {
               trapEntities.add(entity);
            }

         });
         trapEntities.forEach((trapEntity) -> {
            if (!trapEntity.isRemoved()) {
               trapEntity.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            }

         });
      }
   }

   private boolean isTrapEntity(EntityType entityType) {
      return entityType != null && (entityType == net.minecraft.entity.EntityType.ARMOR_STAND || entityType == net.minecraft.entity.EntityType.CHEST_MINECART);
   }
}
