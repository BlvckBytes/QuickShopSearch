package me.blvckbytes.quick_shop_search.integration.essentials_warps;

import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.quick_shop_search.integration.ChunkBucketedCache;
import net.ess3.api.IEssentials;
import net.essentialsx.api.v2.events.WarpModifyEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

public class EssentialsWarpsIntegration extends ChunkBucketedCache<EssentialsWarpData> implements IEssentialsWarpsIntegration {

  public EssentialsWarpsIntegration(Logger logger, PlatformScheduler scheduler) {
    if (!(Bukkit.getPluginManager().getPlugin("Essentials") instanceof IEssentials essentials))
      throw new IllegalStateException("Expected the essentials-plugin to be an instance of IEssentials");

    try {
      Class.forName("net.essentialsx.api.v2.events.WarpModifyEvent");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Expected the WarpModifyEvent of Essentials to be existing; update your software!");
    }

    scheduler.runAsync(task -> {
      var warpsApi = essentials.getWarps();
      var warpNamesList = warpsApi.getList();

      for (var warpName : warpNamesList) {
        try {
          var warpLocation = warpsApi.getWarp(warpName);
          registerItem(new EssentialsWarpData(warpName, warpLocation));
        } catch (Exception e) {
          logger.log(Level.SEVERE, "Could not resolve warp by name \"" + warpName + "\"", e);
        }
      }

      logger.info("Registered " + warpNamesList.size() + " Essentials-Warps!");
    });
  }

  @Override
  protected @Nullable Location extractItemLocation(EssentialsWarpData item) {
    return item.location();
  }

  @Override
  protected boolean doItemsEqual(EssentialsWarpData a, EssentialsWarpData b) {
    return a.name().equals(b.name());
  }

  @EventHandler
  public void onWarpModify(WarpModifyEvent event) {
    var warpName = event.getWarpName();

    if (event.getCause() == WarpModifyEvent.WarpModifyCause.DELETE) {
      unregisterItem(new EssentialsWarpData(warpName, event.getOldLocation()));
      return;
    }

    if (event.getCause() == WarpModifyEvent.WarpModifyCause.CREATE) {
      registerItem(new EssentialsWarpData(warpName, event.getNewLocation()));
      return;
    }

    if (event.getCause() == WarpModifyEvent.WarpModifyCause.UPDATE) {
      unregisterItem(new EssentialsWarpData(warpName, event.getOldLocation()));
      registerItem(new EssentialsWarpData(warpName, event.getNewLocation()));
    }
  }

  @Override
  public @Nullable EssentialsWarpData locateNearestWithinRange(Player player, Location origin, int blockRadius) {
    return findClosestItem(origin, blockRadius);
  }
}
