package me.blvckbytes.quick_shop_search.integration.player_warps;

import com.artillexstudios.axplayerwarps.database.impl.Base;
import com.artillexstudios.axplayerwarps.enums.Access;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.axplayerwarps.warps.WarpManager;
import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.integration.ChunkBucketedCache;
import me.blvckbytes.quick_shop_search.integration.worldguard.IWorldGuardIntegration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

public class AxPlayerWarpsIntegration extends ChunkBucketedCache<Warp> implements IPlayerWarpsIntegration {

  private final @Nullable IWorldGuardIntegration worldGuardIntegration;
  private final ConfigKeeper<MainSection> config;
  private final PlatformScheduler platformScheduler;

  public AxPlayerWarpsIntegration(
    Logger logger,
    PlatformScheduler platformScheduler,
    ConfigKeeper<MainSection> config,
    @Nullable IWorldGuardIntegration worldGuardIntegration
  ) {
    this.config = config;
    this.platformScheduler = platformScheduler;
    this.worldGuardIntegration = worldGuardIntegration;

    try {
      //noinspection ResultOfMethodCallIgnored
      WarpManager.getWarps();

    } catch (Throwable e) {
      throw new IllegalStateException("Expected the warp-manager to be accessible at this point in time", e);
    }

    platformScheduler.runAsync(task -> {
      var itemCount = updateAndInitiateNextUpdate();
      logger.info("Registered " + itemCount + " PlayerWarps!");
    });
  }

  private int updateAndInitiateNextUpdate() {
    clear();

    var warps = WarpManager.getWarps();

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

    return new PlayerWarpData(
      PlayerWarpSource.AX,
      nearestWarp.getOwnerName(),
      nearestWarp.getName(),
      nearestWarp.getLocation(),
      checkIfAccessDenied(nearestWarp, player)
    );
  }

  @Override
  protected @Nullable Location extractItemLocation(Warp item) {
    return item.getLocation();
  }

  @Override
  protected boolean doItemsEqual(Warp a, Warp b) {
    return a.getId() == b.getId();
  }

  private boolean checkIfAccessDenied(Warp warp, Player player) {
    System.out.println(warp.getAccess());

    if (warp.getAccess() == Access.PRIVATE)
      return !warp.getOwner().equals(player.getUniqueId());

    if (warp.getAccess() == Access.WHITELISTED) {
      if (!checkIfContains(warp.getWhitelisted(), player))
        return true;
    }

    return checkIfContains(warp.getBlacklisted(), player);
  }

  private boolean checkIfContains(List<Base.AccessPlayer> list, Player player) {
    var playerName = player.getName();
    var playerId = player.getUniqueId();

    for (var playerAccess : list) {
      var otherPlayer = playerAccess.player();

      if (playerName.equals(playerAccess.name()))
        return true;

      if (otherPlayer == null)
        continue;

      if (otherPlayer.getUniqueId().equals(playerId))
        return true;

      if (playerName.equals(otherPlayer.getName()))
        return true;
    }

    return false;
  }
}
