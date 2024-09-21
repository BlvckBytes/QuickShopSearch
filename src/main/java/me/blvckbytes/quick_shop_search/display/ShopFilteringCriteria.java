package me.blvckbytes.quick_shop_search.display;

import com.ghostchu.quickshop.api.shop.Shop;

import java.util.function.Predicate;

public enum ShopFilteringCriteria {

  IS_BUYING(Shop::isBuying),
  IS_SELLING(Shop::isSelling),
  IS_UNLIMITED(Shop::isUnlimited),
  HAS_STOCK_LEFT(shop -> shop.getRemainingStock() > 0),
  ;

  private final Predicate<Shop> predicate;

  private static final ShopFilteringCriteria[] values = values();
  public static final ArrayIterable<ShopFilteringCriteria> iterable = new ArrayIterable<>(values);

  ShopFilteringCriteria(Predicate<Shop> predicate) {
    this.predicate = predicate;
  }

  public boolean test(Shop shop) {
    return predicate.test(shop);
  }

  public ShopFilteringCriteria next() {
    return values[(ordinal() + 1) % values.length];
  }

  public static ShopFilteringCriteria byOrdinalOrFirst(int ordinal) {
    if (ordinal < 0 || ordinal >= values.length)
      return first();

    return values[ordinal];
  }

  public static ShopFilteringCriteria first() {
    return values[0];
  }
}
