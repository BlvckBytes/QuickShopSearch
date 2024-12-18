package me.blvckbytes.quick_shop_search;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public enum PluginPermission {
  MAIN_COMMAND("command.qss"),
  SUB_COMMAND_ADVERTISE("command.advertise"),
  SUB_COMMAND_ADVERTISE_OWNER_BYPASS("command.advertise.owner-bypass"),
  SUB_COMMAND_ADVERTISE_MANY("command.advertise-many"),
  SUB_COMMAND_ADVERTISE_MANY_OTHER("command.advertise-many.other"),
  SUB_COMMAND_GLOBAL("command.global"),
  SUB_COMMAND_PLAYER("command.player"),
  SUB_COMMAND_NEAR("command.near"),
  SUB_COMMAND_RELOAD("command.reload"),
  SUB_COMMAND_ABOUT("command.about"),

  EMPTY_PREDICATE("empty-predicate"),
  FEATURE_SORT("feature.sort"),
  FEATURE_FILTER("feature.filter"),
  FEATURE_TELEPORT("feature.teleport"),
  FEATURE_TELEPORT_OTHER_WORLD("feature.teleport.other-world"),
  FEATURE_INTERACT("feature.interact"),
  FEATURE_INTERACT_OTHER_WORLD("feature.interact.other-world"),
  FEATURE_LIVE_UPDATES("feature.live-updates"),
  OTHER_WORLD("other-world"),
  NON_ADVERTISE_BYPASS("bypass-non-advertise"),
  ACCESS_LIST_BASE("access-list"),
  ACCESS_LISTS_BYPASS("bypass-access-lists")
  ;

  private static final String PREFIX = "quickshopsearch";
  public final String node;

  PluginPermission(String node) {
    this.node = PREFIX + "." + node;
  }

  public String nodeWithSuffix(String suffix) {
    var nodeTrailing = node.charAt(node.length() - 1) == '.';
    var suffixLeading = suffix.charAt(0) == '.';

    if (nodeTrailing && suffixLeading)
      return node + suffix.substring(1);

    if (!nodeTrailing && !suffixLeading)
      return node + "." + suffix;

    return node + suffix;
  }

  public boolean has(CommandSender sender) {
    if (sender instanceof ConsoleCommandSender)
      return true;

    if (sender instanceof Player player)
      return player.hasPermission(node);

    return false;
  }
}
