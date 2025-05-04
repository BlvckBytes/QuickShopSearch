package me.blvckbytes.quick_shop_search.integration.player_warps;

import com.olziedev.playerwarps.api.PlayerWarpsAPI;
import com.olziedev.playerwarps.api.events.warp.PlayerWarpCreateEvent;
import com.olziedev.playerwarps.api.events.warp.PlayerWarpRemoveEvent;
import com.olziedev.playerwarps.api.warp.Warp;
import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.quick_shop_search.integration.ChunkBucketedCache;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import javax.annotation.Nullable;
import java.util.logging.Logger;

public class PlayerWarpsIntegration extends ChunkBucketedCache<Warp> implements IPlayerWarpsIntegration {

  public PlayerWarpsIntegration(Logger logger, PlatformScheduler scheduler) {
    super();

    PlayerWarpsAPI.getInstance(instance -> {
      scheduler.runAsync(task -> {
        var warps = PlayerWarpsAPI.getInstance().getPlayerWarps(false);

        for (var playerWarp : warps)
          registerItem(playerWarp);

        logger.info("Registered " + warps.size() + " PlayerWarps!");
      });
    });
  }

  @Override
  public @Nullable PlayerWarpData locateNearestWithinRange(Player player, Location origin, int blockRadius) {
    var nearestWarp = findClosestItem(origin, blockRadius);

    if (nearestWarp == null)
      return null;

    var playerId = player.getUniqueId();
    var isBanned = false;

    for (var banned : nearestWarp.getBanned()) {
      if (banned.getUUID().equals(playerId)) {
        isBanned = true;
        break;
      }
    }

    return new PlayerWarpData(
      nearestWarp.getWarpPlayer().getName(),
      nearestWarp.getWarpName(),
      nearestWarp.getWarpLocation().getLocation(),
      isBanned
    );
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerWarpCreate(PlayerWarpCreateEvent event) {
    registerItem(event.getPlayerWarp());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerWarpRemove(PlayerWarpRemoveEvent event) {
    unregisterItem(event.getPlayerWarp());
  }

  @Override
  protected @Nullable Location extractItemLocation(Warp item) {
    return item.getWarpLocation().getLocation();
  }

  @Override
  protected boolean doItemsEqual(Warp a, Warp b) {
    return a.getUUID().equals(b.getUUID());
  }
}
