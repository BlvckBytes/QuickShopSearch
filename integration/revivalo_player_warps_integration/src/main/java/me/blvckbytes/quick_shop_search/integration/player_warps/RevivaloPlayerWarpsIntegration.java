package me.blvckbytes.quick_shop_search.integration.player_warps;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import com.tcoded.folialib.impl.PlatformScheduler;
import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.WarpManager;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.integration.ChunkBucketedCache;
import me.blvckbytes.quick_shop_search.integration.worldguard.IWorldGuardIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public class RevivaloPlayerWarpsIntegration extends ChunkBucketedCache<Warp> implements IPlayerWarpsIntegration {

  private final @Nullable IWorldGuardIntegration worldGuardIntegration;
  private final ConfigKeeper<MainSection> config;
  private final PlatformScheduler platformScheduler;

  private final WarpManager warpManager;

  public RevivaloPlayerWarpsIntegration(
    Logger logger,
    PlatformScheduler platformScheduler,
    ConfigKeeper<MainSection> config,
    @Nullable IWorldGuardIntegration worldGuardIntegration
  ) {
    this.config = config;
    this.platformScheduler = platformScheduler;
    this.worldGuardIntegration = worldGuardIntegration;

    this.warpManager = PlayerWarpsPlugin.getWarpHandler();

    if (this.warpManager == null)
      throw new IllegalStateException("Expected the warp-manager to be accessible at this point in time");

    platformScheduler.runAsync(task -> {
      var itemCount = updateAndInitiateNextUpdate();
      logger.info("Registered " + itemCount + " PlayerWarps!");
    });
  }

  private int updateAndInitiateNextUpdate() {
    clear();

    var warps = this.warpManager.getWarps();

    for (var playerWarp : warps)
      registerItem(playerWarp);

    int updatePeriod;

    if ((updatePeriod = config.rootSection.playerWarpsIntegration.updatePeriodSeconds) > 0)
      this.platformScheduler.runLaterAsync(this::updateAndInitiateNextUpdate, 20L * updatePeriod);

    return warps.size();
  }

  @Override
  public @Nullable PlayerWarpData locateNearestWithinRange(Player player, Location origin, int blockRadius) {
    var matchPredicate = IWorldGuardIntegration.makePredicate(origin, worldGuardIntegration, config.rootSection.playerWarpsIntegration.withinSameRegion);
    var nearestWarp = findClosestItem(origin, blockRadius, matchPredicate);

    if (nearestWarp == null)
      return null;

    var playerId = player.getUniqueId();
    var isBanned = false;

    for (var bannedId : nearestWarp.getBlockedPlayers()) {
      if (bannedId.equals(playerId)) {
        isBanned = true;
        break;
      }
    }

    return new PlayerWarpData(
      PlayerWarpSource.REVIVALO,
      Bukkit.getOfflinePlayer(nearestWarp.getOwner()).getName(),
      nearestWarp.getName(),
      nearestWarp.getLocation(),
      isBanned
    );
  }

  @Override
  protected @Nullable Location extractItemLocation(Warp item) {
    return item.getLocation();
  }

  @Override
  protected boolean doItemsEqual(Warp a, Warp b) {
    return a.getWarpID().equals(b.getWarpID());
  }
}
