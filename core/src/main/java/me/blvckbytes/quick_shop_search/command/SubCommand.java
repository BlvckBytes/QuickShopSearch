package me.blvckbytes.quick_shop_search.command;

import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class SubCommand {

  public final SubCommandLabel label;
  private final String permission;

  public SubCommand(SubCommandLabel label, String permission) {
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

  public boolean hasPermission(Permissible permissible) {
    if (permission == null)
      return true;

    return permissible.hasPermission(permission);
  }
}
