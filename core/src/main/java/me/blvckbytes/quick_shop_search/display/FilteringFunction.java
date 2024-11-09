package me.blvckbytes.quick_shop_search.display;

import me.blvckbytes.quick_shop_search.cache.CachedShop;

@FunctionalInterface
public interface FilteringFunction {

  boolean test(CachedShop shop, DynamicPropertyProvider distanceProvider, boolean negative);

}
