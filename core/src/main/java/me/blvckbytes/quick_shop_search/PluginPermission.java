package me.blvckbytes.quick_shop_search;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public enum PluginPermission {
  MAIN_COMMAND("command.qss"),
  LANGUAGE_COMMAND("command.qssl"),
  RELOAD_COMMAND("command.qssrl"),
  ADVERTISE_COMMAND("command.advertise"),
  ADVERTISE_COMMAND_OWNER_BYPASS("command.advertise.owner-bypass"),
  ADVERTISE_COMMAND_ALLOWLIST_BYPASS("command.advertise.allowlist-bypass"),
  ADVERTISE_COMMAND_ALLOWLIST_BYPASS_OTHERS("command.advertise.allowlist-bypass.others"),
  EMPTY_PREDICATE("empty-predicate"),
  FEATURE_SORT("feature.sort"),
  FEATURE_FILTER("feature.filter"),
  FEATURE_TELEPORT_SHOP("feature.teleport.shop"),
  FEATURE_TELEPORT_PLAYER_WARP("feature.teleport.player-warp"),
  FEATURE_TELEPORT_ESSENTIALS_WARP("feature.teleport.essentials-warp"),
  FEATURE_TELEPORT_NEAREST_PLAYER_WARP_BAN_BYPASS("feature.teleport.nearest-player-warp.ban-bypass"),
  FEATURE_TELEPORT_BYPASS_COOLDOWN_SAME_SHOP("feature.teleport.bypass-cooldown.same-shop"),
  FEATURE_TELEPORT_BYPASS_COOLDOWN_ANY_SHOP("feature.teleport.bypass-cooldown.any-shop"),
  FEATURE_TELEPORT_OTHER_WORLD("feature.teleport.other-world"),
  FEATURE_TELEPORT_OTHER_WORLD_BYPASS_COOLDOWN_SAME_SHOP("feature.teleport.other-world.bypass-cooldown.same-shop"),
  FEATURE_TELEPORT_OTHER_WORLD_BYPASS_COOLDOWN_ANY_SHOP("feature.teleport.other-world.bypass-cooldown.any-shop"),
  FEATURE_INTERACT("feature.interact"),
  FEATURE_INTERACT_OTHER_WORLD("feature.interact.other-world"),
  FEATURE_LIVE_UPDATES("feature.live-updates"),
  FEATURE_SEARCH_FLAG_OWNER("feature.search-flag.owner"),
  FEATURE_SEARCH_FLAG_RADIUS("feature.search-flag.radius"),
  FEATURE_SEARCH_FLAG_PRICE("feature.search-flag.price"),
  FEATURE_SEARCH_FLAG_MIN_PRICE("feature.search-flag.min-price"),
  FEATURE_SEARCH_FLAG_MAX_PRICE("feature.search-flag.max-price"),
  FEATURE_FEES_BYPASS("feature.fees.bypass"),
  FEATURE_FEES_BYPASS_OTHER_WORLD("feature.fees.bypass.other-world"),
  FEATURE_FEES_PERMISSION_NAME_BASE("feature.fees.permission-name"),
  OTHER_WORLD("other-world"),
  NON_ADVERTISE_BYPASS("bypass-non-advertise"),
  ACCESS_LIST_BASE("access-list"),
  ACCESS_LISTS_BYPASS("bypass-access-lists"),
  TELEPORT_COOLDOWN_GROUP_BASE("teleport-cooldown"),
  SLOW_TELEPORT_BYPASS("bypass-slow-teleport")
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
