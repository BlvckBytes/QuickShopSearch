package me.blvckbytes.quick_shop_search.command;

import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class QuickShopSearchCommand implements CommandExecutor, TabExecutor {

  private final Map<SubCommandLabel, SubCommand> subCommandByLabel;
  private final ConfigKeeper<MainSection> config;

  public QuickShopSearchCommand(ConfigKeeper<MainSection> config) {
    this.subCommandByLabel = new HashMap<>();
    this.config = config;
  }

  public void registerSubCommand(SubCommand subCommand) {
    subCommandByLabel.put(subCommand.label, subCommand);
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!PluginPermission.MAIN_COMMAND.has(sender)) {
      config.rootSection.playerMessages.missingPermissionMainCommand.sendMessage(
        sender, config.rootSection.builtBaseEnvironment
      );

      return true;
    }

    if (args.length == 0) {
      printHelpScreen(sender, label);
      return true;
    }

    SubCommand subCommand;

    var normalizedSubCommandLabel = SubCommandLabel.matcher.matchFirst(args[0]);

    if (normalizedSubCommandLabel == null || (subCommand = subCommandByLabel.get(normalizedSubCommandLabel.constant)) == null) {
      config.rootSection.playerMessages.unknownSubCommand.sendMessage(
        sender,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("label", label)
          .withStaticVariable("sub_label", args[0])
          .build()
      );

      return true;
    }

    if (sender instanceof Player player && !player.hasPermission(subCommand.permission)) {
      config.rootSection.playerMessages.missingPermissionSubCommand.sendMessage(
        player,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("label", label)
          .withStaticVariable("sub_label", SubCommandLabel.matcher.getNormalizedName(subCommand.label))
          .build()
      );

      return true;
    }

    var subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
    var exitCode = subCommand.onCommand(sender, subCommandArgs);

    switch (exitCode) {
      case SUCCESS -> {
        return true;
      }

      case PLAYER_ONLY -> {
        config.rootSection.playerMessages.subCommandPlayerOnly.sendMessage(
          sender,
          config.rootSection.getBaseEnvironment()
            .withStaticVariable("label", label)
            .build()
        );
        return true;
      }

      case MALFORMED_USAGE -> {
        config.rootSection.playerMessages.subCommandUsage.sendMessage(
          sender,
          config.rootSection.getBaseEnvironment()
            .withStaticVariable("label", label)
            .withStaticVariable("sub_command_usage", subCommand.getUsage(sender))
            .build()
        );

        return true;
      }
    }

    return true;
  }

  private void printHelpScreen(CommandSender sender, String label) {
    BukkitEvaluable message;

    if ((message = config.rootSection.playerMessages.commandHelpScreenHeader) != null)
      message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

    for (var subCommandLabel : SubCommandLabel.values) {
      var subCommand = subCommandByLabel.get(subCommandLabel);

      if (sender instanceof Player player && !player.hasPermission(subCommand.permission))
        continue;

      config.rootSection.playerMessages.commandHelpScreenLine.sendMessage(
        sender,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("sub_command_usage", subCommand.getUsage(sender))
          .withStaticVariable("sub_command_description", subCommand.getDescription(sender))
          .withStaticVariable("label", label)
          .build()
      );
    }

    if ((message = config.rootSection.playerMessages.commandHelpScreenFooter) != null)
      message.sendMessage(sender, config.rootSection.builtBaseEnvironment);
  }

  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!PluginPermission.MAIN_COMMAND.has(sender) || args.length == 0)
      return List.of();

    if (args.length == 1)
      return SubCommandLabel.matcher.createCompletions(args[0]);

    SubCommand subCommand;

    var normalizedSubCommandLabel = SubCommandLabel.matcher.matchFirst(args[0]);

    if (normalizedSubCommandLabel == null || (subCommand = subCommandByLabel.get(normalizedSubCommandLabel.constant)) == null)
      return List.of();

    if (sender instanceof Player player && !player.hasPermission(subCommand.permission))
      return List.of();

    var subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
    return subCommand.onTabComplete(sender, subCommandArgs);
  }
}
