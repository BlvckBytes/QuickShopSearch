package me.blvckbytes.quick_shop_search;

import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigManager;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ReloadCommand implements CommandExecutor {

  public static final String RELOAD_COMMAND_NAME = "quickshopsearchreload";

  private final Logger logger;
  private final ConfigManager configManager;
  private final ValuePusher<MainSection> configPusher;

  private MainSection mainSection;

  public ReloadCommand(Logger logger, ConfigManager configManager, ValuePusher<MainSection> configPusher) {
    this.logger = logger;
    this.configManager = configManager;
    this.configPusher = configPusher;

    this.mainSection = configPusher
      .subscribeToUpdates(value -> this.mainSection = value)
      .get();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    BukkitEvaluable message;

    if (sender instanceof Player player && !PluginPermission.RELOAD_COMMAND.has(player)) {
      if ((message = mainSection.playerMessages.missingPermissionReload) != null)
        player.sendMessage(message.stringify(mainSection.getBaseEnvironment().build()));

      return true;
    }

    try {
      this.configPusher.push(this.configManager.loadConfig("config.yml").mapSection(null, MainSection.class));

      if ((message = mainSection.playerMessages.pluginReloadedSuccess) != null)
        sender.sendMessage(message.stringify(mainSection.getBaseEnvironment().build()));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "An error occurred while trying to reload the config", e);

      if ((message = mainSection.playerMessages.pluginReloadedError) != null)
        sender.sendMessage(message.stringify(mainSection.getBaseEnvironment().build()));
    }

    return true;
  }
}
