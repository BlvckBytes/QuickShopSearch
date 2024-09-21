package me.blvckbytes.quick_shop_search.display;

import com.ghostchu.quickshop.api.shop.Shop;
import java.util.function.BiFunction;

public enum ShopSortingCriteria {

  PRICE((a, b) -> Double.compare(a.getPrice(), b.getPrice())),
  OWNER_NAME((a, b) -> a.getOwner().getDisplay().compareTo(b.getOwner().getDisplay())),
  STOCK_LEFT((a, b) -> Integer.compare(a.getRemainingStock(), b.getRemainingStock()))
  ;

  private final BiFunction<Shop, Shop, Integer> compare;

  private static final ShopSortingCriteria[] values = values();
  public static final ArrayIterable<ShopSortingCriteria> iterable = new ArrayIterable<>(values);

  ShopSortingCriteria(BiFunction<Shop, Shop, Integer> compare) {
    this.compare = compare;
  }

  public Integer compare(Shop a, Shop b) {
    return compare.apply(a, b);
  }

  public ShopSortingCriteria next() {
    return values[(ordinal() + 1) % values.length];
  }

  public static ShopSortingCriteria byOrdinalOrFirst(int ordinal) {
    if (ordinal < 0 || ordinal >= values.length)
      return first();

    return values[ordinal];
  }

  public static ShopSortingCriteria first() {
    return values[0];
  }
}
