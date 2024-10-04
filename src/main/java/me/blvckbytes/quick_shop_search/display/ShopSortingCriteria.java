package me.blvckbytes.quick_shop_search.display;

import me.blvckbytes.quick_shop_search.CachedShop;

import java.util.Arrays;
import java.util.List;

public enum ShopSortingCriteria {

  PRICE((d, a, b) -> Double.compare(a.getShop().getPrice(), b.getShop().getPrice())),
  OWNER_NAME((d, a, b) -> a.getShop().getOwner().getDisplay().compareTo(b.getShop().getOwner().getDisplay())),
  STOCK_LEFT((d, a, b) -> Integer.compare(a.getCachedStock(), b.getCachedStock())),
  SPACE_LEFT((d, a, b) -> Integer.compare(a.getCachedSpace(), b.getCachedSpace())),
  ITEM_TYPE((d, a, b) -> a.getShop().getItem().getType().compareTo(b.getShop().getItem().getType())),
  SHOP_TYPE((d, a, b) -> a.getShop().getShopType().compareTo(b.getShop().getShopType())),
  DISTANCE((d, a, b) -> Long.compare(d.getShopDistance(a), d.getShopDistance(b)))
  ;

  private final SortingFunction function;

  public static final List<ShopSortingCriteria> values = Arrays.stream(values()).toList();

  ShopSortingCriteria(SortingFunction function) {
    this.function = function;
  }

  public Integer compare(ShopDistanceProvider distanceProvider, CachedShop a, CachedShop b) {
    return function.compare(distanceProvider, a, b);
  }

  public static ShopSortingCriteria byOrdinalOrFirst(int ordinal) {
    if (ordinal < 0 || ordinal >= values.size())
      return values.get(0);

    return values.get(ordinal);
  }
}
