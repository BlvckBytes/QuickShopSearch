package me.blvckbytes.quick_shop_search.integration.player_warps;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

public record PlayerWarpData(
  PlayerWarpSource source,
  String ownerName,
  String warpName,
  Location location,
  Predicate<Player> checkIfBanned
) {}