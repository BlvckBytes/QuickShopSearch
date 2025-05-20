package me.blvckbytes.quick_shop_search.display.result;

import me.blvckbytes.quick_shop_search.cache.CachedShop;

import java.util.Arrays;
import java.util.List;

public enum ShopFilteringCriteria implements FilteringFunction {

  IS_BUYING((shop, d, negative) -> shop.handle.isBuying() ^ negative),
  IS_SELLING((shop, d, negative) -> shop.handle.isSelling() ^ negative),
  IS_UNLIMITED((shop, d, negative) -> shop.handle.isUnlimited() ^ negative),
  HAS_STOCK_LEFT((shop, d, negative) -> {
    if (!shop.handle.isSelling())
      return true;

    return (shop.handle.isUnlimited() || shop.cachedStock > 0) ^ negative;
  }),
  HAS_SPACE_LEFT((shop, d, negative) -> {
    if (!shop.handle.isBuying())
      return true;

    return (shop.handle.isUnlimited() || shop.cachedSpace > 0) ^ negative;
  }),
  SAME_WORLD((shop, d, negative) -> (d.getShopDistance(shop) >= 0) ^ negative),
  CAN_BUY((shop, d, negative) -> {
    if (shop.handle.isSelling())
      return (d.getPlayerBalanceForShopCurrency(shop) >= shop.handle.getPrice()) ^ negative;

    return (d.getShopOwnerBalanceForShopCurrency(shop) >= shop.handle.getPrice()) ^ negative;
  })
  ;

  private final FilteringFunction predicate;

  public static final List<ShopFilteringCriteria> values = Arrays.stream(values()).toList();

  ShopFilteringCriteria(FilteringFunction predicate) {
    this.predicate = predicate;
  }

  @Override
  public boolean test(CachedShop shop, DynamicPropertyProvider distanceProvider, boolean negative) {
    return predicate.test(shop, distanceProvider, negative);
  }

  public ShopFilteringCriteria next() {
    return values.get((ordinal() + 1) % values.size());
  }

  public static ShopFilteringCriteria byOrdinalOrFirst(int ordinal) {
    if (ordinal < 0 || ordinal >= values.size())
      return values.get(0);

    return values.get(ordinal);
  }
}
