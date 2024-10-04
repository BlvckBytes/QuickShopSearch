package me.blvckbytes.quick_shop_search;

import com.ghostchu.quickshop.api.event.ShopCreateSuccessEvent;
import com.ghostchu.quickshop.api.event.ShopDeleteEvent;
import com.ghostchu.quickshop.api.event.ShopInventoryCalculateEvent;
import com.ghostchu.quickshop.api.event.ShopItemChangeEvent;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopManager;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class CachedShopRegistry implements Listener {

  private final Map<Location, CachedShop> existingShopByLocation;

  private MainSection mainSection;

  public CachedShopRegistry(Logger logger, ShopManager shopManager, ValuePusher<MainSection> configPusher) {
    this.existingShopByLocation = new HashMap<>();

    this.mainSection = configPusher
      .subscribeToUpdates(value -> {
        this.mainSection = value;

        synchronized (existingShopByLocation) {
          for (var cachedShop : existingShopByLocation.values())
            cachedShop.setConfig(value);
        }
      })
      .get();

    logger.info("Getting all globally existing shops... This may take a while!");

    for (var shop : shopManager.getAllShops())
      existingShopByLocation.put(shop.getLocation(), new CachedShop(shop, mainSection));

    logger.info("Found " + existingShopByLocation.size() + " shops in total");
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
    synchronized (existingShopByLocation) {
      existingShopByLocation.put(event.getShop().getLocation(), new CachedShop(event.getShop(), mainSection));
    }
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
  public void onShopInventoryCalculate(ShopInventoryCalculateEvent event) {
    tryAccessCache(event.getShop(), shop -> shop.onInventoryCalculate(event.getStock(), event.getSpace()));
  }

  private void tryAccessCache(Shop shop, Consumer<CachedShop> handler) {
    synchronized (existingShopByLocation) {
      var cachedShop = existingShopByLocation.get(shop.getLocation());

      if (cachedShop != null)
        handler.accept(cachedShop);
    }
  }
}
