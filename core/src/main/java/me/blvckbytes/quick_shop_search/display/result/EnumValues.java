package me.blvckbytes.quick_shop_search.display.result;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EnumValues<T extends Enum<T>> {

  private final List<T> allValues;
  private final List<T> enabledValues;

  public EnumValues(T[] values) {
    this.allValues = Arrays.asList(values);
    this.enabledValues = new ArrayList<>(allValues);
  }

  public Collection<T> getEnabledValues() {
    return Collections.unmodifiableCollection(enabledValues);
  }

  public T nextEnabled(T value) {
    return enabledValues.get((enabledValues.indexOf(value) + 1) % enabledValues.size());
  }

  public T firstEnabled() {
    return enabledValues.getFirst();
  }

  public @Nullable T byOrdinalIfEnabledOrNull(int ordinal) {
    if (ordinal < 0 || ordinal >= allValues.size())
      return null;

    var selectedValue = allValues.get(ordinal);

    if (!enabledValues.contains(selectedValue))
      return null;

    return selectedValue;
  }

  public void updateEnabledValues(Map<String, Boolean> enabledStateByName) {
    enabledValues.clear();

    for (var value : allValues) {
      if (enabledStateByName.getOrDefault(value.name(), false))
        enabledValues.add(value);
    }

    // Avoid the special case in which the count of enabled values is zero, thereby
    // causing calls to first and next to break. Disabling all options isn't making
    // any sense anyway, as one could instead simply disable the feature as a whole.

    if (enabledValues.isEmpty())
      enabledValues.addAll(allValues);
  }
}
