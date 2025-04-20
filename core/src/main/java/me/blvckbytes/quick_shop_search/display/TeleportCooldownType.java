package me.blvckbytes.quick_shop_search.display;

import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.quick_shop_search.PluginPermission;
import org.jetbrains.annotations.Nullable;

public record TeleportCooldownType(
  PluginPermission bypassPermission,
  String stampKey,
  long durationMillis,
  @Nullable BukkitEvaluable cooldownMessage
) {}
