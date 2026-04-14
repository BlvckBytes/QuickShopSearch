package me.blvckbytes.quick_shop_search.integration.player_warps;

import org.bukkit.Location;
import org.bukkit.event.Listener;

import javax.annotation.Nullable;

public interface IPlayerWarpsIntegration extends Listener {

  PlayerWarpData PLAYER_WARP_NULL_SENTINEL = new PlayerWarpData(null, null, null, null, player -> false);

  @Nullable PlayerWarpData locateNearestWithinRange(Location origin, int blockRadius);

}
