package me.blvckbytes.quick_shop_search.integration.player_warps;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import com.olziedev.playerwarps.api.PlayerWarpsAPI;
import com.olziedev.playerwarps.api.events.warp.PlayerWarpCreateEvent;
import com.olziedev.playerwarps.api.events.warp.PlayerWarpRemoveEvent;
import com.olziedev.playerwarps.api.warp.Warp;
import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.integration.ChunkBucketedCache;
import me.blvckbytes.quick_shop_search.integration.worldguard.IWorldGuardIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.logging.Logger;

public class OlzieDevPlayerWarpsIntegration extends ChunkBucketedCache<Warp> implements IPlayerWarpsIntegration {

  private final ConfigKeeper<MainSection> config;
  private final @Nullable IWorldGuardIntegration worldGuardIntegration;
  private final PlatformScheduler scheduler;

  public OlzieDevPlayerWarpsIntegration(
    Logger logger,
    PlatformScheduler scheduler,
    ConfigKeeper<MainSection> config,
    @Nullable IWorldGuardIntegration worldGuardIntegration
  ) {
    super(false);

    this.config = config;
    this.worldGuardIntegration = worldGuardIntegration;
    this.scheduler = scheduler;

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
  public @Nullable PlayerWarpData locateNearestWithinRange(Location origin, int blockRadius) {
    var matchPredicate = IWorldGuardIntegration.makePredicate(origin, worldGuardIntegration, config.rootSection.playerWarpsIntegration.withinSameRegion);
    var nearestWarp = findClosestItem(origin, blockRadius, matchPredicate);

    if (nearestWarp == null)
      return null;

    return new PlayerWarpData(
      PlayerWarpSource.OLZIE_DEV,
      nearestWarp.getWarpPlayer().getName(),
      nearestWarp.getWarpName(),
      nearestWarp.getWarpLocation().getLocation(),
      player -> checkIfIsBanned(nearestWarp, player)
    );
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerWarpCreate(PlayerWarpCreateEvent event) {
    var playerWarp = event.getPlayerWarp();

    registerItem(playerWarp);
    callUpdatesEvent(playerWarp);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerWarpRemove(PlayerWarpRemoveEvent event) {
    var playerWarp = event.getPlayerWarp();

    unregisterItem(playerWarp);
    callUpdatesEvent(playerWarp);
  }

  private boolean checkIfIsBanned(Warp warp, Player player) {
    var playerId = player.getUniqueId();

    for (var banned : warp.getBanned()) {
      if (banned.getUUID().equals(playerId))
        return true;
    }

    return false;
  }

  private void callUpdatesEvent(Warp playerWarp) {
    var updates = Collections.singletonList(playerWarp.getWarpLocation().getLocation());
    scheduler.runAsync(task -> Bukkit.getPluginManager().callEvent(new AsyncPlayerWarpsUpdatesEvent(updates)));
  }

  @Override
  protected @Nullable Location extractItemLocation(Warp item) {
    return item.getWarpLocation().getLocation();
  }

  @Override
  protected String extractItemId(Warp item) {
    // NOTE: #getUUID() returns the ID of the player who owns this warp - do not use!
    return String.valueOf(item.getID());
  }
}
