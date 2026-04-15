package me.blvckbytes.quick_shop_search.display.result;

import me.blvckbytes.quick_shop_search.cache.CachedShop;
import org.jetbrains.annotations.Nullable;

public enum ShopSortingCriteria implements SortingFunction {

  PRICE((d, a, b) -> Double.compare(a.handle.getPrice(), b.handle.getPrice())),
  OWNER_NAME((d, a, b) -> a.handle.getOwner().getDisplay().compareTo(b.handle.getOwner().getDisplay())),
  STOCK_LEFT((d, a, b) -> Integer.compare(a.cachedStock, b.cachedStock)),
  SPACE_LEFT((d, a, b) -> Integer.compare(a.cachedSpace, b.cachedSpace)),
  ITEM_TYPE((d, a, b) -> a.handle.getItem().getType().compareTo(b.handle.getItem().getType())),
  SHOP_TYPE((d, a, b) -> Integer.compare(a.handle.shopType().id(), b.handle.shopType().id())),
  SHOP_NAME((d, a, b) -> compareNullableStrings(a.handle.getShopName(), b.handle.getShopName())),
  DISTANCE((d, a, b) -> Long.compare(d.getShopDistance(a), d.getShopDistance(b))),
  WORLD_NAME((d, a, b) -> a.shopWorldName.compareTo(b.shopWorldName)),
  ;

  private final SortingFunction function;

  public static final EnumValues<ShopSortingCriteria> values = new EnumValues<>(values());

  ShopSortingCriteria(SortingFunction function) {
    this.function = function;
  }

  @Override
  public int compare(DynamicPropertyProvider distanceProvider, CachedShop a, CachedShop b) {
    return function.compare(distanceProvider, a, b);
  }

  private static int compareNullableStrings(@Nullable String a, @Nullable String b) {
    if (a == null && b == null)
      return 0;

    if (a == null)
      return 1;

    if (b == null)
      return -1;

    return a.compareTo(b);
  }
}
