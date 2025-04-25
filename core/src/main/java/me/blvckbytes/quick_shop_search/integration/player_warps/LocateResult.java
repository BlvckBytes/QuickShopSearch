package me.blvckbytes.quick_shop_search.integration.player_warps;

import org.bukkit.Location;

public record LocateResult(
  String ownerName,
  String warpName,
  Location location,
  boolean isBanned
) {}