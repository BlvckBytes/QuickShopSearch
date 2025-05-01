package me.blvckbytes.quick_shop_search;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class OfflinePlayerRegistry implements Listener {

  private record NameEntry(
    String name,
    String nameLower
  ) {}

  private final Set<NameEntry> knownPlayerNamesLower;
  private final Map<String, OfflinePlayer> offlinePlayerByNameLower;

  public OfflinePlayerRegistry() {
    this.knownPlayerNamesLower = new HashSet<>();
    this.offlinePlayerByNameLower = new HashMap<>();

    for (var offlinePlayer : Bukkit.getOfflinePlayers())
      registerName(offlinePlayer);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    registerName(event.getPlayer());
  }

  public @Nullable OfflinePlayer getByName(String name) {
    return offlinePlayerByNameLower.get(name.toLowerCase());
  }

  public List<String> createSuggestions(String input, int limit) {
    var result = new ArrayList<String>();
    var lowerInput = input.toLowerCase();

    for (var playerName : knownPlayerNamesLower) {
      if (!playerName.nameLower.startsWith(lowerInput))
        continue;

      result.add(playerName.name);

      if (result.size() == limit)
        break;
    }

    return result;
  }

  private void registerName(OfflinePlayer player) {
    var name = player.getName();

    if (name == null)
      return;

    var nameLower = name.toLowerCase();

    this.knownPlayerNamesLower.add(new NameEntry(name, nameLower));
    this.offlinePlayerByNameLower.put(nameLower, player);
  }
}
