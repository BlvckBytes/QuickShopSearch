package me.blvckbytes.quick_shop_search.display;

import java.util.Arrays;
import java.util.List;

public enum PredicateSelection {
  POSITIVE,
  NEGATIVE,
  INVARIANT
  ;

  public static final List<PredicateSelection> values = Arrays.stream(values()).toList();

  public PredicateSelection next() {
    return values.get((ordinal() + 1) % values.size());
  }

  public static PredicateSelection byOrdinalOrFirst(int ordinal) {
    if (ordinal < 0 || ordinal >= values.size())
      return values.get(0);

    return values.get(ordinal);
  }
}
