package me.blvckbytes.quick_shop_search.textures;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CachedTexturesUpdateEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  public final CachedTextures cachedTextures;

  public CachedTexturesUpdateEvent(CachedTextures cachedTextures) {
    this.cachedTextures = cachedTextures;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlers;
  }

  @NotNull
  public static HandlerList getHandlerList() {
    return handlers;
  }
}
