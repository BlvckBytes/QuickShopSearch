package me.blvckbytes.quick_shop_search;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ShopScalarDiff {

  private final CachedShop shop;

  private boolean lastIsUnlimited;
  private @Nullable String lastShopName;

  public ShopScalarDiff(CachedShop shop) {
    this.shop = shop;
    this.update();
  }

  public boolean update() {
    var unlimited = shop.handle.isUnlimited();
    var shopName = shop.handle.getShopName();

    boolean hadDelta;

    hadDelta = lastIsUnlimited != unlimited;
    lastIsUnlimited = unlimited;

    hadDelta |= !Objects.equals(lastShopName, shopName);
    lastShopName = shopName;

    return hadDelta;
  }
}
