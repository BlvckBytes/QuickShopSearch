package me.blvckbytes.quick_shop_search.cache;

import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopType;
import com.tcoded.folialib.impl.PlatformScheduler;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.ShopUpdate;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.display.DisplayData;
import me.blvckbytes.quick_shop_search.display.ResultDisplayHandler;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class CachedShopRegistry implements QuickShopEventConsumer {

  private final PlatformScheduler scheduler;
  private final Map<Location, CachedShop> existingShopByLocation;
  private final LongOpenHashSet existingShopIds;
  private final ConfigKeeper<MainSection> config;
  private final ResultDisplayHandler displayHandler;

  public CachedShopRegistry(
    PlatformScheduler scheduler,
    ResultDisplayHandler displayHandler,
    ConfigKeeper<MainSection> config,
    Logger logger
  ) {
    this.scheduler = scheduler;
    this.config = config;
    this.displayHandler = displayHandler;
    this.existingShopByLocation = new HashMap<>();
    this.existingShopIds = new LongOpenHashSet();

    config.registerReloadListener(() -> {
      synchronized (existingShopByLocation) {
        for (var cachedShop : existingShopByLocation.values())
          cachedShop.onConfigReload();
      }
    });

    logger.info("Getting all globally existing shops... This may take a while!");

    for (var shop : QuickShopAPI.getInstance().getShopManager().getAllShops())
      existingShopByLocation.put(shop.getLocation(), new CachedShop(scheduler, shop, config));

    logger.info("Found " + existingShopByLocation.size() + " shops in total");
  }

  public DisplayData getExistingShops() {
    synchronized (existingShopByLocation) {
      return new DisplayData(
        this.existingShopByLocation.values(),
        existingShopIds,
        null,
        true
      );
    }
  }

  public int getNumberOfExistingShops() {
    synchronized (existingShopByLocation) {
      return this.existingShopByLocation.size();
    }
  }

  @Override
  public void onShopCreate(Shop shop) {
    scheduler.runAsync(scheduleTask -> {
      synchronized (existingShopByLocation) {
        var addedShop = new CachedShop(scheduler, shop, config);

        existingShopByLocation.put(shop.getLocation(), addedShop);
        existingShopIds.add(shop.getShopId());

        displayHandler.onShopUpdate(addedShop, ShopUpdate.CREATED);
      }
    });
  }

  @Override
  public void onShopDelete(Shop shop) {
    synchronized (existingShopByLocation) {
      var removedShop = existingShopByLocation.remove(shop.getLocation());

      if (removedShop == null)
        return;

      existingShopIds.remove(shop.getShopId());

      displayHandler.onShopUpdate(removedShop, ShopUpdate.REMOVED);
    }
  }

  @Override
  public void onShopItemChange(Shop shop, ItemStack newItem) {
    tryAccessCache(shop, cachedShop -> {
      cachedShop.onItemChange(newItem);
      displayHandler.onShopUpdate(cachedShop, ShopUpdate.ITEM_CHANGED);
    });
  }

  @Override
  public void onShopOwnerChange(Shop shop) {
    tryAccessCache(shop, cachedShop -> {
      displayHandler.onShopUpdate(cachedShop, ShopUpdate.PROPERTIES_CHANGED);
    });
  }

  @Override
  public void onShopSignUpdate(Shop shop) {
    tryAccessCache(shop, cachedShop -> {
      if (cachedShop.diff.update())
        displayHandler.onShopUpdate(cachedShop, ShopUpdate.PROPERTIES_CHANGED);
    });
  }

  @Override
  public void onShopInventoryCalculate(Shop shop, int stock, int space) {
    tryAccessCache(shop, cachedShop -> {
      if (cachedShop.handle.getShopType() == ShopType.BUYING && space >= 0)
        cachedShop.cachedSpace = space;

      if (cachedShop.handle.getShopType() == ShopType.SELLING && stock >= 0)
        cachedShop.cachedStock = stock;

      if (cachedShop.diff.update())
        displayHandler.onShopUpdate(cachedShop, ShopUpdate.PROPERTIES_CHANGED);
    });
  }

  @Override
  public void onShopNameChange(Shop shop) {
    tryAccessCache(shop, cachedShop -> {
      displayHandler.onShopUpdate(cachedShop, ShopUpdate.PROPERTIES_CHANGED);
    });
  }

  @Override
  public void onShopPriceChange(Shop shop) {
    tryAccessCache(shop, cachedShop -> {
      displayHandler.onShopUpdate(cachedShop, ShopUpdate.PROPERTIES_CHANGED);
    });
  }

  @Override
  public void onShopTypeChange(Shop shop) {
    tryAccessCache(shop, cachedShop -> {
      displayHandler.onShopUpdate(cachedShop, ShopUpdate.PROPERTIES_CHANGED);
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
