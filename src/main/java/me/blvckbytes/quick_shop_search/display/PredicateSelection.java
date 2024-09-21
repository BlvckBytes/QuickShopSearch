package me.blvckbytes.quick_shop_search.display;

public enum PredicateSelection {
  POSITIVE,
  NEGATIVE,
  INVARIANT
  ;

  private static final PredicateSelection[] values = values();

  public PredicateSelection next() {
    return values[(ordinal() + 1) % values.length];
  }

  public static PredicateSelection byOrdinalOrFirst(int ordinal) {
    if (ordinal < 0 || ordinal >= values.length)
      return first();

    return values[ordinal];
  }

  public static PredicateSelection first() {
    return values[0];
  }
}
