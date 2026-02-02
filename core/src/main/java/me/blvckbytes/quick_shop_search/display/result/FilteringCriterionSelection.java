package me.blvckbytes.quick_shop_search.display.result;

import at.blvckbytes.component_markup.markup.interpreter.DirectFieldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class FilteringCriterionSelection implements DirectFieldAccess {

  public final ShopFilteringCriteria criterion;
  public final PredicateSelection selection;
  public boolean active;

  public FilteringCriterionSelection(ShopFilteringCriteria criterion, PredicateSelection selection) {
    this.criterion = criterion;
    this.selection = selection;
  }

  @Override
  public @Nullable Object accessField(String rawIdentifier) {
    return switch (rawIdentifier) {
      case "criterion" -> criterion.name();
      case "selection" -> selection.name();
      case "active" -> active;
      default -> DirectFieldAccess.UNKNOWN_FIELD_SENTINEL;
    };
  }

  @Override
  public @Nullable Set<String> getAvailableFields() {
    return Set.of("criterion", "selection", "active");
  }
}
