package me.blvckbytes.quick_shop_search.display;

import me.blvckbytes.quick_shop_search.CachedShop;

public interface ShopDistanceProvider {

  long getShopDistance(CachedShop cachedShop);

}
