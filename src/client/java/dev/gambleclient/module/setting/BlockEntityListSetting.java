package dev.gambleclient.module.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.entity.BlockEntityType;

public class BlockEntityListSetting extends Setting {
   private final List value;
   private final List defaults;

   public BlockEntityListSetting(CharSequence name, List defaults) {
      super(name);
      this.defaults = new ArrayList(defaults);
      this.value = new ArrayList(defaults);
   }

   public List getValue() {
      return Collections.unmodifiableList(this.value);
   }

   public void setValue(List newList) {
      this.value.clear();
      this.value.addAll(newList);
   }

   public void reset() {
      this.value.clear();
      this.value.addAll(this.defaults);
   }

   public BlockEntityListSetting setDescription(CharSequence d) {
      super.setDescription(d);
      return this;
   }
}
