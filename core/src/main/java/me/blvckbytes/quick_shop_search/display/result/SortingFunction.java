package me.blvckbytes.quick_shop_search.display.result;

import me.blvckbytes.quick_shop_search.cache.CachedShop;

@FunctionalInterface
public interface SortingFunction {

  int compare(DynamicPropertyProvider distanceProvider, CachedShop a, CachedShop b);

}
