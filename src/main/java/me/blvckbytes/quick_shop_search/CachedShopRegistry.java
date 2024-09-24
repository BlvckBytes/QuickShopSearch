package me.blvckbytes.quick_shop_search;

import com.ghostchu.quickshop.api.event.ShopCreateSuccessEvent;
import com.ghostchu.quickshop.api.event.ShopDeleteEvent;
import com.ghostchu.quickshop.api.event.ShopInventoryCalculateEvent;
import com.ghostchu.quickshop.api.event.ShopItemChangeEvent;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopManager;
import me.blvckbytes.bukkitevaluable.section.ItemStackSection;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class CachedShopRegistry implements Listener {

  private final ItemStackSection representativePatch;
  private final Map<Location, CachedShop> existingShopByLocation;

  public CachedShopRegistry(Logger logger, ShopManager shopManager, MainSection mainSection) {
    this.representativePatch = mainSection.resultDisplay.representativePatch;
    this.existingShopByLocation = new HashMap<>();

    logger.info("Getting all globally existing shops... This may take a while!");

    for (var shop : shopManager.getAllShops())
      existingShopByLocation.put(shop.getLocation(), new CachedShop(shop, representativePatch));

    logger.info("Found " + existingShopByLocation.size() + " shops in total");
  }

  public Iterable<CachedShop> getExistingShops() {
    synchronized (existingShopByLocation) {
      return this.existingShopByLocation.values();
    }
  }

  @EventHandler
  public void onShopCreate(ShopCreateSuccessEvent event) {
    synchronized (existingShopByLocation) {
      existingShopByLocation.put(event.getShop().getLocation(), new CachedShop(event.getShop(), representativePatch));
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
    tryAccessCache(event.getShop(), shop -> shop.onStockChange(event.getStock()));
  }

  private void tryAccessCache(Shop shop, Consumer<CachedShop> handler) {
    synchronized (existingShopByLocation) {
      var cachedShop = existingShopByLocation.get(shop.getLocation());

      if (cachedShop != null)
        handler.accept(cachedShop);
    }
  }
}
