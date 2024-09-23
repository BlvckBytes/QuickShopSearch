package me.blvckbytes.quick_shop_search.display;

import com.ghostchu.quickshop.api.shop.Shop;
import me.blvckbytes.quick_shop_search.CachedShop;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public enum ShopFilteringCriteria {

  IS_BUYING(shop -> shop.getShop().isBuying()),
  IS_SELLING(shop -> shop.getShop().isSelling()),
  IS_UNLIMITED(shop -> shop.getShop().isUnlimited()),
  HAS_STOCK_LEFT(shop -> shop.getCachedStock() > 0),
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
      return values.getFirst();

    return values.get(ordinal);
  }
}
