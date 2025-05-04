package me.blvckbytes.quick_shop_search.integration.player_warps;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import javax.annotation.Nullable;

public interface IPlayerWarpsIntegration extends Listener {

  @Nullable PlayerWarpData locateNearestWithinRange(Player player, Location origin, int blockRadius);

}
