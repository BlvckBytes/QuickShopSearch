package me.blvckbytes.quick_shop_search.command;

import me.blvckbytes.syllables_matcher.EnumMatcher;

import java.util.Arrays;
import java.util.List;

public enum SubCommandLabel {
  ABOUT,
  ADVERTISE,
  ADVERTISE_MANY,
  GLOBAL,
  NEAR,
  PLAYER,
  RELOAD,
  ;

  public static final List<SubCommandLabel> values = Arrays.stream(values()).toList();
  public static final EnumMatcher<SubCommandLabel> matcher = new EnumMatcher<>(values());
}
