package me.blvckbytes.quick_shop_search.command;

import me.blvckbytes.syllables_matcher.EnumMatcher;

public enum AdvertiseMode {
  ON,
  OFF,
  UNSET
  ;

  public static final EnumMatcher<AdvertiseMode> matcher = new EnumMatcher<>(values());
}
