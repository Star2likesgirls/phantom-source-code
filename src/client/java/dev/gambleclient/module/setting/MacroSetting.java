package dev.gambleclient.module.setting;

import java.util.ArrayList;
import java.util.List;

public class MacroSetting extends Setting {
   private List commands;
   private final List defaultCommands;

   public MacroSetting(CharSequence name, List defaultCommands) {
      super(name);
      this.commands = new ArrayList(defaultCommands);
      this.defaultCommands = new ArrayList(defaultCommands);
   }

   public MacroSetting(CharSequence name) {
      this(name, new ArrayList());
   }

   public List getCommands() {
      return this.commands;
   }

   public void setCommands(List commands) {
      this.commands = new ArrayList(commands);
   }

   public void addCommand(String command) {
      this.commands.add(command);
   }

   public void removeCommand(int index) {
      if (index >= 0 && index < this.commands.size()) {
         this.commands.remove(index);
      }

   }

   public void clearCommands() {
      this.commands.clear();
   }

   public List getDefaultCommands() {
      return new ArrayList(this.defaultCommands);
   }

   public void resetValue() {
      this.commands = new ArrayList(this.defaultCommands);
   }

   public MacroSetting setDescription(CharSequence description) {
      super.setDescription(description);
      return this;
   }
}
