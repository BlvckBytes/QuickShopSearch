package me.blvckbytes.quick_shop_search.cache;

import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopType;
import com.tcoded.folialib.impl.PlatformScheduler;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.ShopUpdate;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.display.ResultDisplayHandler;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class CachedShopRegistry implements QuickShopEventConsumer {

  private final Plugin plugin;
  private final PlatformScheduler scheduler;
  private final Map<Location, CachedShop> existingShopByLocation;
  private final ConfigKeeper<MainSection> config;
  private final ResultDisplayHandler displayHandler;
  private final Long2ObjectMap<Set<CachedShop>> shopsBucketByCoordinateHash;

  public CachedShopRegistry(
    Plugin plugin,
    PlatformScheduler scheduler,
    ResultDisplayHandler displayHandler,
    ConfigKeeper<MainSection> config,
    Logger logger
  ) {
    this.plugin = plugin;
    this.scheduler = scheduler;
    this.config = config;
    this.displayHandler = displayHandler;
    this.existingShopByLocation = new HashMap<>();
    this.shopsBucketByCoordinateHash = new Long2ObjectAVLTreeMap<>();

    config.registerReloadListener(() -> {
      synchronized (existingShopByLocation) {
        for (var cachedShop : existingShopByLocation.values())
          cachedShop.onConfigReload();
      }
    });

    logger.info("Getting all globally existing shops... This may take a while!");

    for (var shop : QuickShopAPI.getInstance().getShopManager().getAllShops()) {
      var cachedShop = new CachedShop(plugin, scheduler, shop, config);

      existingShopByLocation.put(shop.getLocation(), cachedShop);
      addToCoordinateLookup(cachedShop);
    }

    logger.info("Found " + existingShopByLocation.size() + " shops in total");
  }

  public @Nullable CachedShop findByLocation(Location location) {
    var bucket = shopsBucketByCoordinateHash.get(fastCoordinateHash(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

    if (bucket == null)
      return null;

    for (var bucketEntry : bucket) {
      var bucketEntryWorld = Objects.requireNonNull(bucketEntry.handle.getLocation().getWorld());

      if (bucketEntryWorld.equals(location.getWorld()))
        return bucketEntry;
    }

    return null;
  }

  public Collection<CachedShop> getExistingShops() {
    synchronized (existingShopByLocation) {
      return this.existingShopByLocation.values();
    }
  }

  @Override
  public void onShopCreate(Shop shop) {
    scheduler.runAsync(scheduleTask -> {
      synchronized (existingShopByLocation) {
        var cachedShop = new CachedShop(plugin, scheduler, shop, config);

        existingShopByLocation.put(shop.getLocation(), cachedShop);
        addToCoordinateLookup(cachedShop);

        displayHandler.onShopUpdate(cachedShop, ShopUpdate.CREATED);
      }
    });
  }

  @Override
  public void onShopDelete(Shop shop) {
    synchronized (existingShopByLocation) {
      var cachedShop = existingShopByLocation.remove(shop.getLocation());

      if (cachedShop == null)
        return;

      removeFromCoordinateLookup(cachedShop);
      displayHandler.onShopUpdate(cachedShop, ShopUpdate.REMOVED);
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

  private void addToCoordinateLookup(CachedShop shop) {
    var location = shop.handle.getLocation();
    var coordinateHash = fastCoordinateHash(location.getBlockX(), location.getBlockY(), location.getBlockZ());

    shopsBucketByCoordinateHash
      .computeIfAbsent(coordinateHash, key -> new HashSet<>())
      .add(shop);
  }

  private void removeFromCoordinateLookup(CachedShop shop) {
    var location = shop.handle.getLocation();
    var coordinateHash = fastCoordinateHash(location.getBlockX(), location.getBlockY(), location.getBlockZ());

    var bucket = shopsBucketByCoordinateHash.get(coordinateHash);

    if (bucket != null)
      bucket.remove(shop);
  }

  private static long fastCoordinateHash(int x, int y, int z) {
    // y in [-64;320] - adding 64 will result in [0;384], thus 9 bits will suffice
    // long has 64 bits, (64-9)/2 = 27.5, thus, let's reserve 10 bits for y, and add 128, for future-proofing
    // 27 bits per x/z axis, with one sign-bit, => +- 67,108,864
    // As far as I know, the world is limited to around +- 30,000,000 - so we're fine

    return (
      // 2^10 - 1 = 0x3FF
      // 2^26 - 1 = 0x3FFFFFF
      // 2^26     = 0x4000000
      ((y + 128) & 0x3FF) |
      (((x & 0x3FFFFFF) | (x < 0 ? 0x4000000L : 0)) << 10) |
      (((z & 0x3FFFFFF) | (z < 0 ? 0x4000000L : 0)) << (10 + 27))
    );
  }
}
