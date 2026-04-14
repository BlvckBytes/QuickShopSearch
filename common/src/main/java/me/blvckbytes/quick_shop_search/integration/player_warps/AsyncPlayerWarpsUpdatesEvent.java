package me.blvckbytes.quick_shop_search.integration.player_warps;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;
import java.util.List;

public class AsyncPlayerWarpsUpdatesEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  public final List<Location> updatedLocations;

  public AsyncPlayerWarpsUpdatesEvent(List<Location> updatedLocations) {
    super(true);

    this.updatedLocations = updatedLocations;
  }

  @Override
  public @Nonnull HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
