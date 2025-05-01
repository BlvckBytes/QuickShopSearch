package me.blvckbytes.quick_shop_search.command;

import me.blvckbytes.quick_shop_search.OfflinePlayerRegistry;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SearchFlagsContainer {

  private final Map<SearchFlag<?>, Object> valueByFlag;

  public SearchFlagsContainer() {
    this.valueByFlag = new HashMap<>();
  }

  public int size() {
    return valueByFlag.size();
  }

  @SuppressWarnings("unchecked")
  public <T> @Nullable T get(SearchFlag<T> flag) {
    var value = valueByFlag.get(flag);

    if (value == null)
      return null;

    return (T) value;
  }

  @SuppressWarnings("unchecked")
  public <T, R> @Nullable R getWithMapper(SearchFlag<T> flag, Function<T, R> mapper) {
    var value = valueByFlag.get(flag);

    if (value == null)
      return null;

    return mapper.apply((T) value);
  }

  public boolean parseAndSet(SearchFlag<?> flag, String value, OfflinePlayerRegistry offlinePlayerRegistry) {
    var parsedValue = flag.parse(value, offlinePlayerRegistry);

    if (parsedValue == null)
      return false;

    this.valueByFlag.put(flag, parsedValue);
    return true;
  }

  public boolean test(CachedShop shop, Player executor) {
    for (var valueEntry : valueByFlag.entrySet()) {
      if (!valueEntry.getKey().test(shop, executor, valueEntry.getValue()))
        return false;
    }

    return true;
  }
}
