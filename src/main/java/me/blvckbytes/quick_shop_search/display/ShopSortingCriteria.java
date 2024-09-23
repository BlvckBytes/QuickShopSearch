package me.blvckbytes.quick_shop_search.display;

import me.blvckbytes.quick_shop_search.CachedShop;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

public enum ShopSortingCriteria {

  PRICE((a, b) -> Double.compare(a.getShop().getPrice(), b.getShop().getPrice())),
  OWNER_NAME((a, b) -> a.getShop().getOwner().getDisplay().compareTo(b.getShop().getOwner().getDisplay())),
  STOCK_LEFT((a, b) -> Integer.compare(a.getCachedStock(), b.getCachedStock()))
  ;

  private final BiFunction<CachedShop, CachedShop, Integer> compare;

  public static final List<ShopSortingCriteria> values = Arrays.stream(values()).toList();

  ShopSortingCriteria(BiFunction<CachedShop, CachedShop, Integer> compare) {
    this.compare = compare;
  }

  public Integer compare(CachedShop a, CachedShop b) {
    return compare.apply(a, b);
  }

  public ShopSortingCriteria next() {
    return values.get((ordinal() + 1) % values.size());
  }

  public static ShopSortingCriteria byOrdinalOrFirst(int ordinal) {
    if (ordinal < 0 || ordinal >= values.size())
      return values.getFirst();

    return values.get(ordinal);
  }
}
