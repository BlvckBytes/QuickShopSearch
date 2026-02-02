package me.blvckbytes.quick_shop_search.display.teleport;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.quick_shop_search.integration.essentials_warps.EssentialsWarpData;
import me.blvckbytes.quick_shop_search.integration.player_warps.PlayerWarpData;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public record TeleportDisplayData(
  boolean canUseShopLocation,
  Location finalShopLocation,

  boolean canUsePlayerWarp,
  @Nullable PlayerWarpData nearestPlayerWarp,

  boolean canUseEssentialsWarp,
  @Nullable EssentialsWarpData nearestEssentialsWarp,

  InterpretationEnvironment extendedShopEnvironment,
  Runnable reopenResultDisplay,
  @Nullable Runnable afterTeleporting
) {
  public boolean playerWarpAvailable() {
    return canUsePlayerWarp && nearestPlayerWarp != null;
  }

  public boolean essentialsWarpAvailable() {
    return canUseEssentialsWarp && nearestEssentialsWarp != null;
  }
}
