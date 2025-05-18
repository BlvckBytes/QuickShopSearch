package me.blvckbytes.quick_shop_search.integration.worldguard;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public interface IWorldGuardIntegration {

  List<String> getRegionsContainingLocation(Location location);

  static @Nullable Predicate<Location> makePredicate(
    Location origin,
    @Nullable IWorldGuardIntegration worldGuardIntegration,
    boolean withinSameRegion
  ) {
    if (worldGuardIntegration == null || !withinSameRegion)
      return null;

    var originRegions = worldGuardIntegration.getRegionsContainingLocation(origin);

    return candidateLocation -> {
      var candidateRegions = worldGuardIntegration.getRegionsContainingLocation(candidateLocation);

      // There'll likely not be many regions at any given point, thus I believe that using
      // sets with their intersection-implementation will only slow things down unnecessarily.
      for (var candidateRegion : candidateRegions) {
        for (var originRegion : originRegions) {
          if (candidateRegion.equals(originRegion))
            return true;
        }
      }

      return false;
    };
  }
}
