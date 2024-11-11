package me.blvckbytes.quick_shop_search.cache;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.*;

public class OfflinePlayerCache implements Listener {

  private final Set<String> knownPlayerNames;
  private final Map<String, OfflinePlayer> offlinePlayerByLowerName;
  private final Map<UUID, String> offlinePlayerLowerNameByUuid;
  private final Map<UUID, OfflinePlayer> offlinePlayerById;

  public OfflinePlayerCache() {
    this.knownPlayerNames = new HashSet<>();
    this.offlinePlayerByLowerName = new WeakHashMap<>();
    this.offlinePlayerLowerNameByUuid = new HashMap<>();
    this.offlinePlayerById = new WeakHashMap<>();

    for (var player : Bukkit.getOfflinePlayers()) {
      var playerName = player.getName();

      if (playerName == null)
        continue;

      offlinePlayerByLowerName.put(playerName.toLowerCase(), player);
      knownPlayerNames.add(player.getName());
    }
  }

  @SuppressWarnings("deprecation")
  public OfflinePlayer getByName(String name) {
    var lowerName = name.toLowerCase();

    OfflinePlayer result;

    if ((result = offlinePlayerByLowerName.get(lowerName)) != null)
      return result;

    result = Bukkit.getOfflinePlayer(lowerName);

    offlinePlayerByLowerName.put(lowerName, result);
    offlinePlayerLowerNameByUuid.put(result.getUniqueId(), lowerName);

    return result;
  }

  public OfflinePlayer getById(UUID id) {
    return offlinePlayerById.computeIfAbsent(id, Bukkit::getOfflinePlayer);
  }

  public List<String> createSuggestions(String input) {
    return knownPlayerNames
      .stream()
      .filter(name -> StringUtils.containsIgnoreCase(name, input))
      .limit(20)
      .toList();
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();

    knownPlayerNames.add(player.getName());

    // Try to account for name-changes

    var cacheNameEntry = offlinePlayerLowerNameByUuid.remove(player.getUniqueId());

    if (cacheNameEntry != null)
      offlinePlayerByLowerName.remove(cacheNameEntry);
  }
}
