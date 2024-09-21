package me.blvckbytes.quick_shop_search.display;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;

public record ArrayIterable<T>(
  T[] array
) implements Iterable<T> {

  @Override
  public @NotNull Iterator<T> iterator() {
    return Arrays.stream(array).iterator();
  }
}
