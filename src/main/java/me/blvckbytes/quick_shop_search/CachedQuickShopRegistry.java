package me.blvckbytes.quick_shop_search;

import com.ghostchu.quickshop.api.event.ShopCreateSuccessEvent;
import com.ghostchu.quickshop.api.event.ShopDeleteEvent;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class CachedQuickShopRegistry implements Listener {

  private final Map<Location, Shop> existingShopByLocation;

  public CachedQuickShopRegistry(Logger logger, ShopManager shopManager) {
    this.existingShopByLocation = new HashMap<>();

    logger.info("Getting all globally existing shops... This may take a while!");

    for (var shop : shopManager.getAllShops())
      existingShopByLocation.put(shop.getLocation(), shop);
  }

  public Iterable<Shop> getExistingShops() {
    return this.existingShopByLocation.values();
  }

  @EventHandler
  public void onShopCreate(ShopCreateSuccessEvent event) {
    existingShopByLocation.put(event.getShop().getLocation(), event.getShop());
  }

  @EventHandler
  public void onShopDelete(ShopDeleteEvent event) {
    if (!event.isCancelled())
      existingShopByLocation.remove(event.getShop().getLocation());
  }
}
