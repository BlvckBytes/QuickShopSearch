package me.blvckbytes.quick_shop_search.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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

  protected <T extends Enum<T>> @Nullable T parseEnumConstant(Class<T> enumType, String input) {
    for (var constant : enumType.getEnumConstants()) {
      if (constant.name().equalsIgnoreCase(input))
        return constant;
    }

    return null;
  }

  protected List<String> suggestEnumConstants(Class<? extends Enum<?>> enumType, @Nullable String input) {
    var constants = enumType.getEnumConstants();
    var result = new ArrayList<String>(constants.length);
    var inputLower = input == null ? null : input.toLowerCase();

    for (var constant : constants) {
      var constantName = constant.name();

      if (inputLower != null && !constantName.toLowerCase().startsWith(inputLower))
        continue;

      result.add(constantName);
    }

    return result;
  }
}
