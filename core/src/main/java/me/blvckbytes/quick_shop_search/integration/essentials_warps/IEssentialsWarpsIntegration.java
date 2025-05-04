package me.blvckbytes.quick_shop_search.integration.essentials_warps;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import javax.annotation.Nullable;

public interface IEssentialsWarpsIntegration extends Listener {

  @Nullable EssentialsWarpData locateNearestWithinRange(Player player, Location origin, int blockRadius);

}
