package me.blvckbytes.quick_shop_search;

import org.bukkit.entity.Player;

public enum PluginPermission {
  MAIN_COMMAND("command.qss"),
  LANGUAGE_COMMAND("command.qssl"),
  RELOAD_COMMAND("command.qssrl"),
  EMPTY_PREDICATE("empty-predicate"),
  FEATURE_SORT("feature.sort"),
  FEATURE_FILTER("feature.filter"),
  FEATURE_TELEPORT("feature.teleport"),
  FEATURE_TELEPORT_OTHER_WORLD("feature.teleport.other-world")
  ;

  private static final String PREFIX = "quickshopsearch";
  private final String node;

  PluginPermission(String node) {
    this.node = PREFIX + "." + node;
  }

  public boolean has(Player player) {
    return player.hasPermission(node);
  }
}
