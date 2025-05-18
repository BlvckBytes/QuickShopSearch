package me.blvckbytes.quick_shop_search.integration.worldguard;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class WorldGuardIntegration implements IWorldGuardIntegration {

  private final ConfigKeeper<MainSection> config;
  private final Logger logger;
  private final RegionContainer regionContainer;

  public WorldGuardIntegration(Logger logger, ConfigKeeper<MainSection> config) {
    this.logger = logger;
    this.config = config;
    this.regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
  }

  @Override
  public List<String> getRegionsContainingLocation(Location location) {
    var locationWorld = location.getWorld();

    if (locationWorld == null)
      return List.of();

    var regionManager = regionContainer.get(new BukkitWorld(locationWorld));

    if (regionManager == null) {
      logger.warning("Could not access a RegionContainer for world " + locationWorld.getName());
      return List.of();
    }

    var position = new BlockVector3(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    var regions = regionManager.getApplicableRegions(position);
    var resultingIds = new ArrayList<String>();

    for (var region : regions) {
      if (config.rootSection.worldGuardIntegration.ignoredIds.contains(region.getId()))
        continue;

      if (config.rootSection.worldGuardIntegration.ignoredPriorities.contains(region.getPriority()))
        continue;

      resultingIds.add(region.getId());
    }

    return resultingIds;
  }
}
