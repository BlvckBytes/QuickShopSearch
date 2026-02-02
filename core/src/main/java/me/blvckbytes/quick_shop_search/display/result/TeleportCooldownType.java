package me.blvckbytes.quick_shop_search.display.result;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import me.blvckbytes.quick_shop_search.PluginPermission;
import org.jetbrains.annotations.Nullable;

public record TeleportCooldownType(
  PluginPermission bypassPermission,
  String stampKey,
  long durationMillis,
  @Nullable ComponentMarkup cooldownMessage
) {}
