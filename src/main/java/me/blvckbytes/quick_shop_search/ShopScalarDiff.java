package me.blvckbytes.quick_shop_search;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ShopScalarDiff {

  private final CachedShop shop;

  private int lastCachedSpace;
  private int lastCachedStock;
  private boolean lastIsUnlimited;
  private @Nullable String lastShopName;

  public ShopScalarDiff(CachedShop shop) {
    this.shop = shop;
    this.update();
  }

  public boolean update() {
    var unlimited = shop.handle.isUnlimited();
    var shopName = shop.handle.getShopName();
    var cachedSpace = shop.cachedSpace;
    var cachedStock = shop.cachedStock;

    boolean hadDelta;

    hadDelta = lastIsUnlimited != unlimited;
    lastIsUnlimited = unlimited;

    hadDelta |= !Objects.equals(lastShopName, shopName);
    lastShopName = shopName;

    hadDelta |= lastCachedSpace != cachedSpace;
    lastCachedSpace = cachedSpace;

    hadDelta |= lastCachedStock != cachedStock;
    lastCachedStock = cachedStock;

    return hadDelta;
  }
}
