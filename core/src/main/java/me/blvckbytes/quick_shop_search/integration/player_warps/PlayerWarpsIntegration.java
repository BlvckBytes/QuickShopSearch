package me.blvckbytes.quick_shop_search.integration.player_warps;

import com.olziedev.playerwarps.api.PlayerWarpsAPI;
import com.olziedev.playerwarps.api.events.warp.PlayerWarpCreateEvent;
import com.olziedev.playerwarps.api.events.warp.PlayerWarpRemoveEvent;
import com.olziedev.playerwarps.api.warp.Warp;
import com.tcoded.folialib.impl.PlatformScheduler;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Logger;

public class PlayerWarpsIntegration implements IPlayerWarpsIntegration {

  private final Map<UUID, Long2ObjectMap<List<Warp>>> warpsByChunkHashByWorldId;

  public PlayerWarpsIntegration(Logger logger, PlatformScheduler scheduler) {
    this.warpsByChunkHashByWorldId = new HashMap<>();

    PlayerWarpsAPI.getInstance(instance -> {
      scheduler.runAsync(task -> {
        var warps = PlayerWarpsAPI.getInstance().getPlayerWarps(false);

        for (var playerWarp : warps)
          registerWarp(playerWarp);

        logger.info("Registered " + warps.size() + " PlayerWarps!");
      });
    });
  }

  @Override
  public @Nullable LocateResult locateNearestWithinRange(Player player, Location origin, int axisRangeInBlocks) {
    var world = origin.getWorld();

    if (world == null)
      return null;

    var warpByChunkHash = warpsByChunkHashByWorldId.get(world.getUID());

    if (warpByChunkHash == null)
      return null;

    var axisRangeInBlocksSquared = axisRangeInBlocks * axisRangeInBlocks;
    var axisRangeInChunks = (axisRangeInBlocks + 15) / 16;
    var originChunkX = origin.getBlockX() >> 4;
    var originChunkY = origin.getBlockY() >> 4;
    var originChunkZ = origin.getBlockZ() >> 4;

    Warp closestWarp = null;
    double closestDistanceSquared = 0;

    for (int iY = 0; iY < axisRangeInChunks * 2 + 1; ++iY) {
      int chunkDeltaY = (iY & 1) != 0 ? iY - iY / 2 : iY / -2;
      for (int iX = 0; iX < axisRangeInChunks * 2 + 1; ++iX) {
        int chunkDeltaX = (iX & 1) != 0 ? iX - iX / 2 : iX / -2;
        for (int iZ = 0; iZ < axisRangeInChunks * 2 + 1; ++iZ) {
          int chunkDeltaZ = (iZ & 1) != 0 ? iZ - iZ / 2 : iZ / -2;

          long currentChunkHash = fastChunkHash(
            originChunkX + chunkDeltaX,
            originChunkY + chunkDeltaY,
            originChunkZ + chunkDeltaZ
          );

          var warps = warpByChunkHash.get(currentChunkHash);

          if (warps == null)
            continue;

          for (var warp : warps) {
            var distanceSquared = warp.getWarpLocation().getLocation().distanceSquared(origin);

            if (distanceSquared > axisRangeInBlocksSquared)
              continue;

            if (closestWarp == null || distanceSquared < closestDistanceSquared) {
              closestWarp = warp;
              closestDistanceSquared = distanceSquared;
            }
          }
        }
      }
    }

    if (closestWarp == null)
      return null;

    var playerId = player.getUniqueId();
    boolean isBanned = false;

    for (var banEntry : closestWarp.getBanned()) {
      if (!banEntry.getUUID().equals(playerId))
        continue;

      isBanned = true;
      break;
    }

    return new LocateResult(
      closestWarp.getWarpPlayer().getName(),
      closestWarp.getWarpName(),
      closestWarp.getWarpLocation().getLocation(),
      isBanned
    );
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerWarpCreate(PlayerWarpCreateEvent event) {
    registerWarp(event.getPlayerWarp());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerWarpRemove(PlayerWarpRemoveEvent event) {
    unregisterWarp(event.getPlayerWarp());
  }

  private void registerWarp(Warp warp) {
    var warpLocation = warp.getWarpLocation();
    var bukkitLocation = warpLocation.getLocation();

    if (bukkitLocation == null)
      return;

    var warpWorld = bukkitLocation.getWorld();

    if (warpWorld == null)
      return;

    warpsByChunkHashByWorldId
      .computeIfAbsent(warpWorld.getUID(), k -> new Long2ObjectAVLTreeMap<>())
      .computeIfAbsent(fastChunkHash(bukkitLocation), k -> new ArrayList<>())
      .add(warp);
  }

  private void unregisterWarp(Warp warp) {
    var warpLocation = warp.getWarpLocation();
    var bukkitLocation = warpLocation.getLocation();

    if (bukkitLocation == null)
      return;

    var warpWorld = bukkitLocation.getWorld();

    if (warpWorld == null)
      return;

    var warpByChunkHash = warpsByChunkHashByWorldId.get(warpWorld.getUID());

    if (warpByChunkHash == null)
      return;

    warpByChunkHash.remove(fastChunkHash(bukkitLocation));
  }

  private long fastChunkHash(Location location) {
    return fastChunkHash(
      location.getBlockX() >> 4,
      location.getBlockY() >> 4,
      location.getBlockZ() >> 4
    );
  }

  private long fastChunkHash(int chunkX, int chunkY, int chunkZ) {
    // Assuming an axis is limited to roughly +- 30,000,000, that would be +- 1,875,000 chunks/axis
    // 2^(22-1) = 2,097,152 => 22 bits per axis should suffice
    // The y-axis does by far not exhaust this range, so it will be compromised to 20 bits

    //  ---------- 22b ----------   ---------- 22b ----------   ---------- 20b ----------
    // 64.......................43 42.......................21 20.........................1
    // <1b sign chunkX><21b chunkX><1b sign chunkZ><21b chunkZ><1b sign chunkY><19b chunkY>
    return (
      (((((long) chunkX) & 0x3FFFFF) << (20 + 22)) | ((chunkX < 0 ? 1L : 0L) << (20 + 22 + 21))) |
      (((((long) chunkZ) & 0x3FFFFF) << 20) | ((chunkZ < 0 ? 1L : 0L) << (20 + 21))) |
      ((((long) chunkY) & 0x7FFFF) | ((chunkY < 0 ? 1L : 0L) << 19))
    );
  }
}
