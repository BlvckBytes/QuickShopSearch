package me.blvckbytes.quick_shop_search.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import me.blvckbytes.quick_shop_search.PluginPermission;
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
    ComponentMarkup message;

    if (!PluginPermission.RELOAD_COMMAND.has(sender)) {
      if ((message = config.rootSection.playerMessages.missingPermissionReloadCommand) != null)
        message.sendMessage(sender);

      return true;
    }

    try {
      this.config.reload();

      if ((message = config.rootSection.playerMessages.pluginReloadedSuccess) != null)
        message.sendMessage(sender);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "An error occurred while trying to reload the config", e);

      if ((message = config.rootSection.playerMessages.pluginReloadedError) != null)
        message.sendMessage(sender);
    }

    return true;
  }
}
