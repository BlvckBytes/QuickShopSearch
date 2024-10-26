package me.blvckbytes.quick_shop_search;

import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ReloadCommand implements CommandExecutor {

  private final Logger logger;
  private final ConfigKeeper<MainSection> config;

  public ReloadCommand(Logger logger, ConfigKeeper<MainSection> config) {
    this.logger = logger;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
    BukkitEvaluable message;

    if (!PluginPermission.RELOAD_COMMAND.has(sender)) {
      if ((message = config.rootSection.playerMessages.missingPermissionReloadCommand) != null)
        message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

      return true;
    }

    try {
      this.config.reload();

      if ((message = config.rootSection.playerMessages.pluginReloadedSuccess) != null)
        message.sendMessage(sender, config.rootSection.builtBaseEnvironment);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "An error occurred while trying to reload the config", e);

      if ((message = config.rootSection.playerMessages.pluginReloadedError) != null)
        message.sendMessage(sender, config.rootSection.builtBaseEnvironment);
    }

    return true;
  }
}
