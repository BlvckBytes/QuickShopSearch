package me.blvckbytes.quick_shop_search;

import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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

    if (sender instanceof Player player && !PluginPermission.RELOAD_COMMAND.has(player)) {
      if ((message = config.rootSection.playerMessages.missingPermissionReload) != null)
        player.sendMessage(message.stringify(config.rootSection.getBaseEnvironment().build()));

      return true;
    }

    try {
      this.config.reload();

      if ((message = config.rootSection.playerMessages.pluginReloadedSuccess) != null)
        sender.sendMessage(message.stringify(config.rootSection.getBaseEnvironment().build()));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "An error occurred while trying to reload the config", e);

      if ((message = config.rootSection.playerMessages.pluginReloadedError) != null)
        sender.sendMessage(message.stringify(config.rootSection.getBaseEnvironment().build()));
    }

    return true;
  }
}
