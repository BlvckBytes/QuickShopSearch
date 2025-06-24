package me.blvckbytes.quick_shop_search.integration.player_warps;

import com.olziedev.playerwarps.api.PlayerWarpsAPI;
import com.olziedev.playerwarps.api.events.warp.PlayerWarpCreateEvent;
import com.olziedev.playerwarps.api.events.warp.PlayerWarpRemoveEvent;
import com.olziedev.playerwarps.api.warp.Warp;
import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.integration.ChunkBucketedCache;
import me.blvckbytes.quick_shop_search.integration.worldguard.IWorldGuardIntegration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import javax.annotation.Nullable;
import java.util.logging.Logger;

public class OlzieDevPlayerWarpsIntegration extends ChunkBucketedCache<Warp> implements IPlayerWarpsIntegration {

  private final ConfigKeeper<MainSection> config;
  private final @Nullable IWorldGuardIntegration worldGuardIntegration;

  public OlzieDevPlayerWarpsIntegration(
    Logger logger,
    PlatformScheduler scheduler,
    ConfigKeeper<MainSection> config,
    @Nullable IWorldGuardIntegration worldGuardIntegration
  ) {
    this.config = config;
    this.worldGuardIntegration = worldGuardIntegration;

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
    var matchPredicate = IWorldGuardIntegration.makePredicate(origin, worldGuardIntegration, config.rootSection.playerWarpsIntegration.withinSameRegion);
    var nearestWarp = findClosestItem(origin, blockRadius, matchPredicate);

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
    // NOTE: #getUUID() returns the ID of the player who owns this warp - do not use!
    return a.getID() == b.getID();
  }
}
