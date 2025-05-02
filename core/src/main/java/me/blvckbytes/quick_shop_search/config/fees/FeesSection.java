package me.blvckbytes.quick_shop_search.config.fees;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

@CSAlways
public class FeesSection extends AConfigSection {

  public boolean enabled;

  public long feesPayBackTimeoutTicks;

  public FeesDistanceRangesSection worldsFallback;

  public FeesOtherWorldSection otherWorld;

  public Map<String, FeesDistanceRangesSection> worlds;

  public FeesSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.enabled = false;
    this.worlds = new LinkedHashMap<>();
    this.feesPayBackTimeoutTicks = 40;
  }

  public @Nullable FeesValuesSection decideFees(Player player, CachedShop shop) {
    var playerLocation = player.getLocation();
    var playerWorld = playerLocation.getWorld();

    if (playerWorld == null)
      return null;

    var shopLocation = shop.handle.getLocation();
    var shopWorld = shopLocation.getWorld();

    if (shopWorld == null)
      return null;

    var shopWorldName = shopWorld.getName();

    if (!playerWorld.equals(shopWorld)) {
      if (PluginPermission.FEATURE_FEES_BYPASS_OTHER_WORLD.has(player))
        return null;

      var worldSpecificSection = otherWorld.worlds.get(shopWorldName);

      if (worldSpecificSection == null)
        return otherWorld.worldsFallback;

      return worldSpecificSection;
    }

    if (PluginPermission.FEATURE_FEES_BYPASS.has(player))
      return null;

    FeesDistanceRangesSection feesSection;

    if ((feesSection = worlds.get(shopWorldName)) == null)
      feesSection = worldsFallback;

    var distance = (int) Math.ceil(shopLocation.distance(playerLocation));

    FeesDistanceRangeSection rangeSection = null;

    for (var distanceRange : feesSection.distanceRanges) {
      if (distance >= distanceRange.minDistance && distance <= distanceRange.maxDistance) {
        rangeSection = distanceRange;
        break;
      }
    }

    if (rangeSection == null)
      rangeSection = feesSection.distanceRangesFallback;

    FeesValuesSection highestPriorityFees = null;

    for (var permissionNameEntry : rangeSection.permissionNames.entrySet()) {
      var permissionName = permissionNameEntry.getKey();
      var permissionNode = PluginPermission.FEATURE_FEES_PERMISSION_NAME_BASE.nodeWithSuffix(permissionName);

      if (!player.hasPermission(permissionNode))
        continue;

      var currentFees = permissionNameEntry.getValue();

      if (highestPriorityFees == null || highestPriorityFees.priority < currentFees.priority)
        highestPriorityFees = currentFees;
    }

    if (highestPriorityFees != null)
      return highestPriorityFees;

    return rangeSection.permissionNamesFallback;
  }
}
