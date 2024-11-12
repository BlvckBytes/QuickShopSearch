package me.blvckbytes.quick_shop_search.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class SubCommand {

  public final String label;
  public final String permission;

  public SubCommand(String label, String permission) {
    this.label = label;
    this.permission = permission;
  }

  public abstract ExitCode onCommand(CommandSender sender, String[] args);

  public abstract List<String> onTabComplete(CommandSender sender, String[] args);

  public abstract String getUsage(CommandSender sender);

  public abstract String getDescription(CommandSender sender);

  protected @Nullable Integer parseInteger(String input) {
    try {
      return Integer.parseInt(input);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
