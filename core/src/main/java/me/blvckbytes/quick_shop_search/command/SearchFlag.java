package me.blvckbytes.quick_shop_search.command;

import me.blvckbytes.quick_shop_search.OfflinePlayerRegistry;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.EnumPredicate;
import me.blvckbytes.syllables_matcher.MatchableEnum;
import org.apache.commons.lang.StringUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class SearchFlag<T> implements MatchableEnum {

  private static final Map<String, SearchFlag<?>> searchFlagByName = new HashMap<>();

  public static final SearchFlag<OfflinePlayer> OWNER = new SearchFlag<>("OWNER", PluginPermission.FEATURE_SEARCH_FLAG_OWNER) {

    @Override
    public @Nullable OfflinePlayer parse(String value, OfflinePlayerRegistry offlinePlayerRegistry) {
      return offlinePlayerRegistry.getByName(value);
    }

    @Override
    public boolean _test(CachedShop shop, Player executor, @NotNull OfflinePlayer value) {
      var shopOwnerId = shop.handle.getOwner().getUniqueId();
      return shopOwnerId != null && shopOwnerId.equals(value.getUniqueId());
    }
  };

  public static final SearchFlag<Integer> RADIUS = new SearchFlag<>("RADIUS", PluginPermission.FEATURE_SEARCH_FLAG_RADIUS) {

    @Override
    public @Nullable Integer parse(String value, OfflinePlayerRegistry offlinePlayerRegistry) {
      return SearchFlag.tryParsePositiveInteger(value);
    }

    @Override
    public boolean _test(CachedShop shop, Player executor, @NotNull Integer value) {
      var shopLocation = shop.handle.getLocation();
      var playerLocation = executor.getLocation();
      var shopWorld = shopLocation.getWorld();

      if (shopWorld == null || !shopWorld.equals(executor.getWorld()))
        return false;

      return shopLocation.distanceSquared(playerLocation) < value * value;
    }
  };

  public static final SearchFlag<Double> PRICE = new SearchFlag<>("PRICE", PluginPermission.FEATURE_SEARCH_FLAG_PRICE) {

    @Override
    public @Nullable Double parse(String value, OfflinePlayerRegistry offlinePlayerRegistry) {
      return SearchFlag.tryParsePositiveDouble(value);
    }

    @Override
    public boolean _test(CachedShop shop, Player executor, @NotNull Double value) {
      return Math.abs(shop.handle.getPrice() - value) < .001;
    }
  };

  public static final SearchFlag<Double> MAX_PRICE = new SearchFlag<>("MAX_PRICE", PluginPermission.FEATURE_SEARCH_FLAG_MAX_PRICE) {

    @Override
    public @Nullable Double parse(String value, OfflinePlayerRegistry offlinePlayerRegistry) {
      return SearchFlag.tryParsePositiveDouble(value);
    }

    @Override
    public boolean _test(CachedShop shop, Player executor, @NotNull Double value) {
      return shop.handle.getPrice() <= value;
    }
  };

  public static final SearchFlag<Double> MIN_PRICE = new SearchFlag<>("MIN_PRICE", PluginPermission.FEATURE_SEARCH_FLAG_MIN_PRICE) {

    @Override
    public @Nullable Double parse(String value, OfflinePlayerRegistry offlinePlayerRegistry) {
      return SearchFlag.tryParsePositiveDouble(value);
    }

    @Override
    public boolean _test(CachedShop shop, Player executor, @NotNull Double value) {
      return shop.handle.getPrice() >= value;
    }
  };

  public static final EnumMatcher<SearchFlag<?>> matcher = new EnumMatcher<>(searchFlagByName.values());

  private final String name;
  private final PluginPermission permission;
  private @Nullable List<String> suggestions;

  private SearchFlag(String name, PluginPermission permission) {
    this.name = name;
    this.permission = permission;

    searchFlagByName.put(name.toLowerCase(), this);
  }

  public abstract @Nullable T parse(String value, OfflinePlayerRegistry offlinePlayerRegistry);

  protected abstract boolean _test(CachedShop shop, Player executor, @NotNull T value);

  @SuppressWarnings("unchecked")
  public boolean test(CachedShop shop, Player executor, @NotNull Object value) {
    return _test(shop, executor, (T) value);
  }

  public void setSuggestions(@Nullable List<String> suggestions) {
    this.suggestions = suggestions;
  }

  public List<String> getSuggestions(String input) {
    if (this.suggestions != null) {
      return this.suggestions.stream()
        .filter(it -> StringUtils.startsWithIgnoreCase(it, input))
        .toList();
    }

    return List.of("[value]");
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SearchFlag<?> that)) return false;
    return Objects.equals(name, that.name);
  }

  public static @Nullable SearchFlag<?> getByName(String name) {
    return searchFlagByName.get(name.toLowerCase());
  }

  public static EnumPredicate<SearchFlag<?>> permissionPredicate(Player player) {
    return flag -> flag.constant.permission.has(player);
  }

  private static @Nullable Integer tryParsePositiveInteger(String value) {
    try {
      var result = Integer.parseInt(value);

      if (result < 0)
        return null;

      return result;
    } catch (Exception e) {
      return null;
    }
  }

  private static @Nullable Double tryParsePositiveDouble(String value) {
    try {
      var result = Double.parseDouble(value);

      if (result < 0)
        return null;

      return result;
    } catch (Exception e) {
      return null;
    }
  }
}
