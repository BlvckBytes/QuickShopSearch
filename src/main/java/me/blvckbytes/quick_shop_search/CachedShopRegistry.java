package me.blvckbytes.quick_shop_search;

import com.ghostchu.quickshop.api.event.*;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopManager;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CachedShopRegistry implements Listener {

  private final Plugin plugin;
  private final Map<Location, CachedShop> existingShopByLocation;
  private final ConfigKeeper<MainSection> config;

  public CachedShopRegistry(Plugin plugin, ShopManager shopManager, ConfigKeeper<MainSection> config) {
    this.plugin = plugin;
    this.config = config;
    this.existingShopByLocation = new HashMap<>();

    config.registerReloadListener(() -> {
      synchronized (existingShopByLocation) {
        for (var cachedShop : existingShopByLocation.values())
          cachedShop.onConfigReload();
      }
    });

    plugin.getLogger().info("Getting all globally existing shops... This may take a while!");

    for (var shop : shopManager.getAllShops())
      existingShopByLocation.put(shop.getLocation(), new CachedShop(shop, config));

    plugin.getLogger().info("Found " + existingShopByLocation.size() + " shops in total");
  }

  public Collection<CachedShop> getExistingShops() {
    synchronized (existingShopByLocation) {
      return this.existingShopByLocation.values();
    }
  }

  public int getNumberOfExistingShops() {
    synchronized (existingShopByLocation) {
      return this.existingShopByLocation.size();
    }
  }

  @EventHandler
  public void onShopCreate(ShopCreateSuccessEvent event) {
    Bukkit.getScheduler().runTask(plugin, () -> {
      synchronized (existingShopByLocation) {
        existingShopByLocation.put(event.getShop().getLocation(), new CachedShop(event.getShop(), config));
      }
    });
  }

  @EventHandler
  public void onShopDelete(ShopDeleteEvent event) {
    synchronized (existingShopByLocation) {
      if (!event.isCancelled())
        existingShopByLocation.remove(event.getShop().getLocation());
    }
  }

  @EventHandler
  public void onShopItemChange(ShopItemChangeEvent event) {
    if (!event.isCancelled())
      tryAccessCache(event.getShop(), shop -> shop.onItemChange(event.getNewItem()));
  }

  @EventHandler
  public void onSignUpdate(ShopSignUpdateEvent event) {
    tryAccessCache(event.getShop(), shop -> {
      shop.setCachedStock(event.getShop().getRemainingStock());
      shop.setCachedSpace(event.getShop().getRemainingSpace());
    });
  }

  private void tryAccessCache(Shop shop, Consumer<CachedShop> handler) {
    synchronized (existingShopByLocation) {
      var cachedShop = existingShopByLocation.get(shop.getLocation());

      if (cachedShop != null)
        handler.accept(cachedShop);
    }
  }
}
