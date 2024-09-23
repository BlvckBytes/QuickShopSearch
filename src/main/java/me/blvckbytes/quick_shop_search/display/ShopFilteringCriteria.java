package me.blvckbytes.quick_shop_search.display;

import com.ghostchu.quickshop.api.shop.Shop;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public enum ShopFilteringCriteria {

  IS_BUYING(Shop::isBuying),
  IS_SELLING(Shop::isSelling),
  IS_UNLIMITED(Shop::isUnlimited),
  HAS_STOCK_LEFT(shop -> shop.getRemainingStock() > 0),
  ;

  private final Predicate<Shop> predicate;

  public static final List<ShopFilteringCriteria> values = Arrays.stream(values()).toList();

  ShopFilteringCriteria(Predicate<Shop> predicate) {
    this.predicate = predicate;
  }

  public boolean test(Shop shop) {
    return predicate.test(shop);
  }

  public ShopFilteringCriteria next() {
    return values.get((ordinal() + 1) % values.size());
  }

  public static ShopFilteringCriteria byOrdinalOrFirst(int ordinal) {
    if (ordinal < 0 || ordinal >= values.size())
      return values.getFirst();

    return values.get(ordinal);
  }
}
