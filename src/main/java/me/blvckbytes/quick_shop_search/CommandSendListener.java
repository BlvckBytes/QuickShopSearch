package me.blvckbytes.quick_shop_search;

import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.command.Command;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class CommandSendListener implements Listener {

  private final Map<Command, PluginPermission> pluginCommands;
  private final String lowerPluginName;

  public CommandSendListener(JavaPlugin plugin, ConfigKeeper<MainSection> config) {
    this.pluginCommands = new HashMap<>();
    this.lowerPluginName = plugin.getName().toLowerCase();

    pluginCommands.put(
      plugin.getCommand(config.rootSection.commands.quickShopSearch.evaluatedName),
      PluginPermission.MAIN_COMMAND
    );

    pluginCommands.put(
      plugin.getCommand(config.rootSection.commands.quickShopSearchLanguage.evaluatedName),
      PluginPermission.LANGUAGE_COMMAND
    );

    pluginCommands.put(
      plugin.getCommand(config.rootSection.commands.quickShopSearchReload.evaluatedName),
      PluginPermission.RELOAD_COMMAND
    );
  }

  @EventHandler
  public void onCommandSend(PlayerCommandSendEvent event) {
    var player = event.getPlayer();

    for (var pluginCommandEntry : pluginCommands.entrySet()) {
      if (pluginCommandEntry.getValue().has(player))
        continue;

      var pluginCommand = pluginCommandEntry.getKey();

      event.getCommands().remove(pluginCommand.getName());
      event.getCommands().remove(lowerPluginName + ":" + pluginCommand.getName());

      for (var alias : pluginCommand.getAliases()) {
        event.getCommands().remove(alias);
        event.getCommands().remove(lowerPluginName + ":" + alias);
      }
    }
  }
}
