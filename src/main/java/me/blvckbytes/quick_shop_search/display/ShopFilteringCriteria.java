package me.blvckbytes.quick_shop_search.display;

import me.blvckbytes.quick_shop_search.cache.CachedShop;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public enum ShopFilteringCriteria {

  IS_BUYING(shop -> shop.handle.isBuying()),
  IS_SELLING(shop -> shop.handle.isSelling()),
  IS_UNLIMITED(shop -> shop.handle.isUnlimited()),
  HAS_STOCK_LEFT(shop -> shop.cachedStock > 0),
  ;

  private final Predicate<CachedShop> predicate;

  public static final List<ShopFilteringCriteria> values = Arrays.stream(values()).toList();

  ShopFilteringCriteria(Predicate<CachedShop> predicate) {
    this.predicate = predicate;
  }

  public boolean test(CachedShop shop) {
    return predicate.test(shop);
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
