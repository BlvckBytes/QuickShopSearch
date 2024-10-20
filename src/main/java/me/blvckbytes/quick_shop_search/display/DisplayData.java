package me.blvckbytes.quick_shop_search.display;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public record DisplayData(
  Collection<CachedShop> shops,
  LongOpenHashSet shopIds,
  @Nullable ItemPredicate query,
  boolean isBackedByRegistry
) {
  public boolean contains(CachedShop shop) {
    return shopIds.contains(shop.handle.getShopId());
  }

  public void add(CachedShop shop) {
    if (isBackedByRegistry)
      return;

    this.shopIds.add(shop.handle.getShopId());
    this.shops.add(shop);
  }

  public void remove(CachedShop shop) {
    if (isBackedByRegistry)
      return;

    this.shopIds.remove(shop.handle.getShopId());
    this.shops.remove(shop);
  }

  public boolean doesMatchQuery(CachedShop shop) {
    if (query == null)
      return true;

    return query.test(shop.handle.getItem());
  }
}
