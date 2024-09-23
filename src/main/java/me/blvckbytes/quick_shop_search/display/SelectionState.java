package me.blvckbytes.quick_shop_search.display;

import com.google.gson.JsonObject;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.quick_shop_search.CachedShop;

import java.util.*;

public class SelectionState {

  private ShopSortingCriteria currentSortingCriterion;
  private boolean currentSortingOrder;

  private final Map<ShopFilteringCriteria, PredicateSelection> filteringSelections;
  private ShopFilteringCriteria selectedFilteringCriteria;

  public SelectionState() {
    this.currentSortingCriterion = ShopSortingCriteria.values.getFirst();
    this.currentSortingOrder = false;

    this.filteringSelections = new TreeMap<>(); // Ensure constant order
    for (ShopFilteringCriteria criteria : ShopFilteringCriteria.values)
      this.filteringSelections.put(criteria, PredicateSelection.INVARIANT);
    this.selectedFilteringCriteria = ShopFilteringCriteria.values.getFirst();
  }

  private SelectionState(
    ShopSortingCriteria currentSortingCriterion,
    boolean currentSortingOrder,
    Map<ShopFilteringCriteria, PredicateSelection> filteringSelections,
    ShopFilteringCriteria selectedFilteringCriteria
  ) {
    this.currentSortingCriterion = currentSortingCriterion;
    this.currentSortingOrder = currentSortingOrder;
    this.filteringSelections = filteringSelections;
    this.selectedFilteringCriteria = selectedFilteringCriteria;
  }

  public void nextSortingCriterion() {
    this.currentSortingCriterion = this.currentSortingCriterion.next();
  }

  public void toggleSortingOrder() {
    this.currentSortingOrder ^= true;
  }

  public void nextFilteringCriterion() {
    this.selectedFilteringCriteria = this.selectedFilteringCriteria.next();
  }

  public void nextFilteringState() {
    PredicateSelection currentState = this.filteringSelections.get(this.selectedFilteringCriteria);

    if (currentState == null)
      return;

    this.filteringSelections.put(this.selectedFilteringCriteria, currentState.next());
  }

  public void applySort(List<CachedShop> items) {
    items.sort((a, b) -> this.currentSortingCriterion.compare(a, b) * (this.currentSortingOrder ? 1 : -1));
  }

  public List<CachedShop> applyFilter(List<CachedShop> items) {
    List<CachedShop> result = new ArrayList<>();

    for (var item : items) {
      var doesMatch = true;

      for (var entry : this.filteringSelections.entrySet()) {
        var predicate = entry.getValue();

        if (predicate == PredicateSelection.INVARIANT)
          continue;

        var criteriaResult = entry.getKey().test(item);

        if (predicate == PredicateSelection.NEGATIVE)
          criteriaResult ^= true;

        if (criteriaResult)
          continue;

        doesMatch = false;
        break;
      }

      if (doesMatch)
        result.add(item);
    }

    return result;
  }

  public IEvaluationEnvironment makeSortingEnvironment() {
    return new EvaluationEnvironmentBuilder()
      .withStaticVariable("available_criteria", ShopSortingCriteria.values)
      .withLiveVariable("current_order", () -> currentSortingOrder)
      .withLiveVariable("current_criterion", () -> currentSortingCriterion)
      .build();
  }

  public IEvaluationEnvironment makeFilteringEnvironment() {
    return new EvaluationEnvironmentBuilder()
      .withLiveVariable("filtering_selections", () -> this.filteringSelections)
      .withLiveVariable("selected_criteria", () -> this.selectedFilteringCriteria)
      .build();
  }

  public JsonObject toJson() {
    var result = new JsonObject();

    result.addProperty("currentSortingCriterion", currentSortingCriterion.ordinal());
    result.addProperty("currentSortingOrder", currentSortingOrder);

    var selections = new JsonObject();

    for (var filteringSelection : filteringSelections.entrySet())
      selections.addProperty(String.valueOf(filteringSelection.getKey().ordinal()), filteringSelection.getValue().ordinal());

    result.add("filteringSelections", selections);
    result.addProperty("selectedFilteringCriteria", selectedFilteringCriteria.ordinal());

    return result;
  }

  public static SelectionState fromJson(JsonObject json) throws Exception {
    var currentSortingCriterion = ShopSortingCriteria.byOrdinalOrFirst(json.getAsJsonPrimitive("currentSortingCriterion").getAsInt());
    var currentSortingOrder = json.getAsJsonPrimitive("currentSortingOrder").getAsBoolean();

    var filteringSelections = new TreeMap<ShopFilteringCriteria, PredicateSelection>(); // Ensure constant order
    var selections = json.getAsJsonObject("filteringSelections");

    for (ShopFilteringCriteria criteria : ShopFilteringCriteria.values) {
      filteringSelections.put(
        criteria,
        PredicateSelection.byOrdinalOrFirst(selections.getAsJsonPrimitive(String.valueOf(criteria.ordinal())).getAsInt())
      );
    }

    var selectedFilteringCriteria = ShopFilteringCriteria.byOrdinalOrFirst(json.getAsJsonPrimitive("selectedFilteringCriteria").getAsInt());
    return new SelectionState(currentSortingCriterion, currentSortingOrder, filteringSelections, selectedFilteringCriteria);
  }
}
