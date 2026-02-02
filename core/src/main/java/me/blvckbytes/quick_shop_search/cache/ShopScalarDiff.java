package me.blvckbytes.quick_shop_search.cache;

import com.ghostchu.quickshop.api.shop.IShopType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ShopScalarDiff {

  private final CachedShop shop;

  private int lastCachedSpace;
  private int lastCachedStock;
  private boolean lastIsUnlimited;
  private @Nullable String lastShopName;
  private double lastPrice;
  private @Nullable IShopType lastShopType;

  public ShopScalarDiff(CachedShop shop) {
    this.shop = shop;
  }

  public boolean update() {
    var unlimited = shop.handle.isUnlimited();

    boolean hadDelta;

    hadDelta = lastIsUnlimited != unlimited;
    lastIsUnlimited = unlimited;

    hadDelta |= !Objects.equals(lastShopName, shop.cachedName);
    lastShopName = shop.cachedName;

    hadDelta |= lastPrice != shop.cachedPrice;
    lastPrice = shop.cachedPrice;

    hadDelta |= !Objects.equals(lastShopType, shop.cachedType);
    lastShopType = shop.cachedType;

    hadDelta |= lastCachedSpace != shop.cachedSpace;
    lastCachedSpace = shop.cachedSpace;

    hadDelta |= lastCachedStock != shop.cachedStock;
    lastCachedStock = shop.cachedStock;

    return hadDelta;
  }
}
