package me.blvckbytes.quick_shop_search.display.teleport;

import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.quick_shop_search.integration.player_warps.PlayerWarpData;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public record TeleportDisplayData(
  Location finalShopLocation,
  IEvaluationEnvironment extendedShopEnvironment,
  @Nullable PlayerWarpData nearestPlayerWarp,
  Runnable reopenResultDisplay,
  @Nullable Runnable afterTeleporting
) {}
