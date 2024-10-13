package me.blvckbytes.quick_shop_search.cache;

import com.ghostchu.quickshop.api.event.*;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopManager;
import com.ghostchu.quickshop.api.shop.ShopType;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.ShopUpdate;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.display.DisplayData;
import me.blvckbytes.quick_shop_search.display.ResultDisplayHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CachedShopRegistry implements Listener {

  private final Plugin plugin;
  private final Map<Location, CachedShop> existingShopByLocation;
  private final LongOpenHashSet existingShopIds;
  private final ConfigKeeper<MainSection> config;
  private final ResultDisplayHandler displayHandler;

  public CachedShopRegistry(
    Plugin plugin,
    ShopManager shopManager,
    ResultDisplayHandler displayHandler,
    ConfigKeeper<MainSection> config
  ) {
    this.plugin = plugin;
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

    plugin.getLogger().info("Getting all globally existing shops... This may take a while!");

    for (var shop : shopManager.getAllShops())
      existingShopByLocation.put(shop.getLocation(), new CachedShop(shop, config));

    plugin.getLogger().info("Found " + existingShopByLocation.size() + " shops in total");
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

  @EventHandler
  public void onShopCreate(ShopCreateSuccessEvent event) {
    Bukkit.getScheduler().runTask(plugin, () -> {
      synchronized (existingShopByLocation) {
        var addedShop = new CachedShop(event.getShop(), config);

        existingShopByLocation.put(event.getShop().getLocation(), addedShop);
        existingShopIds.add(event.getShop().getShopId());

        displayHandler.onShopUpdate(addedShop, ShopUpdate.CREATED);
      }
    });
  }

  @EventHandler
  public void onShopDelete(ShopDeleteEvent event) {
    if (event.isCancelled())
      return;

    synchronized (existingShopByLocation) {
      var removedShop = existingShopByLocation.remove(event.getShop().getLocation());

      if (removedShop == null)
        return;

      existingShopIds.remove(event.getShop().getShopId());

      displayHandler.onShopUpdate(removedShop, ShopUpdate.REMOVED);
    }
  }

  @EventHandler
  public void onShopItemChange(ShopItemChangeEvent event) {
    if (!event.isCancelled())
      tryAccessCache(event.getShop(), shop -> {
        shop.onItemChange(event.getNewItem());
        displayHandler.onShopUpdate(shop, ShopUpdate.ITEM_CHANGED);
      });
  }

  @EventHandler
  public void onOwnerChange(ShopOwnershipTransferEvent event) {
    tryAccessCache(event.getShop(), shop -> {
      displayHandler.onShopUpdate(shop, ShopUpdate.PROPERTIES_CHANGED);
    });
  }

  @EventHandler
  public void onShopSignUpdate(ShopSignUpdateEvent event) {
    // NOTE: Sadly, this event is not being called when unsetting shop-names.
    //       Neither is the ShopNamingEvent.

    tryAccessCache(event.getShop(), shop -> {
      if (shop.diff.update())
        displayHandler.onShopUpdate(shop, ShopUpdate.PROPERTIES_CHANGED);
    });
  }

  @EventHandler
  public void onShopCalculate(ShopInventoryCalculateEvent event) {
    tryAccessCache(event.getShop(), shop -> {
      if (shop.handle.getShopType() == ShopType.BUYING && event.getSpace() >= 0)
        shop.cachedSpace = event.getSpace();

      if (shop.handle.getShopType() == ShopType.SELLING && event.getStock() >= 0)
        shop.cachedStock = event.getStock();

      if (shop.diff.update())
        displayHandler.onShopUpdate(shop, ShopUpdate.PROPERTIES_CHANGED);
    });
  }

  @EventHandler
  public void onShopNaming(ShopNamingEvent event) {
    tryAccessCache(event.getShop(), shop -> {
      displayHandler.onShopUpdate(shop, ShopUpdate.PROPERTIES_CHANGED);
    });
  }

  @EventHandler
  public void onShopPriceChange(ShopPriceChangeEvent event) {
    tryAccessCache(event.getShop(), shop -> {
      displayHandler.onShopUpdate(shop, ShopUpdate.PROPERTIES_CHANGED);
    });
  }

  @EventHandler
  public void onShopTypeChange(ShopTypeChangeEvent event) {
    tryAccessCache(event.getShop(), shop -> {
      displayHandler.onShopUpdate(shop, ShopUpdate.PROPERTIES_CHANGED);
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
