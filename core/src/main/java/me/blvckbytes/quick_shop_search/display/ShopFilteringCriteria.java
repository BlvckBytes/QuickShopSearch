package me.blvckbytes.quick_shop_search.display;

import me.blvckbytes.quick_shop_search.cache.CachedShop;

import java.util.Arrays;
import java.util.List;

public enum ShopFilteringCriteria implements FilteringFunction {

  IS_BUYING((shop, negative) -> shop.handle.isBuying() ^ negative),
  IS_SELLING((shop, negative) -> shop.handle.isSelling() ^ negative),
  IS_UNLIMITED((shop, negative) -> shop.handle.isUnlimited() ^ negative),
  HAS_STOCK_LEFT((shop, negative) -> {
    if (!shop.handle.isSelling())
      return false;

    return (shop.handle.isUnlimited() || shop.cachedStock > 0) ^ negative;
  }),
  HAS_SPACE_LEFT((shop, negative) -> {
    if (!shop.handle.isBuying())
      return false;

    return (shop.handle.isUnlimited() || shop.cachedSpace > 0) ^ negative;
  }),
  ;

  private final FilteringFunction predicate;

  public static final List<ShopFilteringCriteria> values = Arrays.stream(values()).toList();

  ShopFilteringCriteria(FilteringFunction predicate) {
    this.predicate = predicate;
  }

  @Override
  public boolean test(CachedShop shop, boolean negative) {
    return predicate.test(shop, negative);
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