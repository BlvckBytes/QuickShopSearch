package me.blvckbytes.quick_shop_search.display;

import me.blvckbytes.quick_shop_search.CachedShop;

@FunctionalInterface
public interface SortingFunction {

  int compare(ShopDistanceProvider distanceProvider, CachedShop a, CachedShop b);

}
