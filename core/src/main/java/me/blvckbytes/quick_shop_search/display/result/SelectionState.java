package me.blvckbytes.quick_shop_search.display.result;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import com.google.gson.JsonObject;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.PluginPermission;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SelectionState {

  private List<SortingCriterionSelection> sortingSelections;
  private int selectedSortingSelectionIndex;

  private Map<ShopFilteringCriteria, PredicateSelection> filteringSelections;
  private @Nullable Map<ShopFilteringCriteria, PredicateSelection> volatileFilteringBackups;
  private ShopFilteringCriteria selectedFilteringCriteria;

  public SelectionState() {
    this(true);
  }

  private SelectionState(boolean initialize) {
    if (initialize) {
      resetSorting();
      resetFiltering();
    }
  }

  public void possiblyRestoreVolatileBackups() {
    if (volatileFilteringBackups == null)
      return;

    for (ShopFilteringCriteria filter : volatileFilteringBackups.keySet())
      filteringSelections.put(filter, volatileFilteringBackups.get(filter));

    volatileFilteringBackups.clear();
  }

  public void setFilteringCriterionState(ShopFilteringCriteria criterion, PredicateSelection state) {
    PredicateSelection priorSelection = this.filteringSelections.put(criterion, state);

    if (volatileFilteringBackups == null)
      volatileFilteringBackups = new HashMap<>();

    volatileFilteringBackups.put(criterion, priorSelection);
  }

  public void updateEnumValues(Logger logger) {
    try {
      loadFromJson(toJson());
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "An error occurred while trying to reload a selection-state from json as to update enum-values", e);
    }
  }

  public void resetFiltering() {
    this.filteringSelections = makeDefaultFilteringSelections();
    this.selectedFilteringCriteria = ShopFilteringCriteria.values.firstEnabled();

    if (this.volatileFilteringBackups != null)
      this.volatileFilteringBackups.clear();
  }

  public void resetSorting() {
    this.sortingSelections = makeDefaultSortingSelections();
    this.selectedSortingSelectionIndex = 0;
  }

  public void nextSortingSelection() {
    if (++this.selectedSortingSelectionIndex == this.sortingSelections.size())
      this.selectedSortingSelectionIndex = 0;
  }

  public void nextSortingOrder() {
    var sortingSelection = sortingSelections.get(this.selectedSortingSelectionIndex);
    sortingSelection.selection = sortingSelection.selection.next();
  }

  public void moveSortingSelectionDown() {
    var removalIndex = selectedSortingSelectionIndex;
    nextSortingSelection();
    var targetSelection = this.sortingSelections.remove(removalIndex);
    this.sortingSelections.add(selectedSortingSelectionIndex, targetSelection);
  }

  public void nextFilteringCriterion() {
    this.selectedFilteringCriteria = ShopFilteringCriteria.values.nextEnabled(this.selectedFilteringCriteria);
  }

  public void nextFilteringState() {
    PredicateSelection currentState = this.filteringSelections.get(this.selectedFilteringCriteria);

    if (currentState == null)
      return;

    this.filteringSelections.put(this.selectedFilteringCriteria, currentState.next());

    if (this.volatileFilteringBackups != null)
      this.volatileFilteringBackups.remove(this.selectedFilteringCriteria);
  }

  public void applySort(DynamicPropertyProvider distanceProvider, List<CachedShop> items) {
    items.sort((a, b) -> {
      for (var sortingSelection : sortingSelections) {
        if (sortingSelection.selection == SortingSelection.INACTIVE)
          continue;

        int sortingResult = sortingSelection.criterion.compare(distanceProvider, a, b);

        if (sortingSelection.selection == SortingSelection.DESCENDING)
          sortingResult *= -1;

        if (sortingResult != 0)
          return sortingResult;
      }

      return 0;
    });
  }

  public List<CachedShop> applyFilter(Collection<CachedShop> items, DynamicPropertyProvider distanceProvider) {
    List<CachedShop> result = new ArrayList<>();

    for (var item : items) {
      var doesMatch = true;

      for (var entry : this.filteringSelections.entrySet()) {
        var predicate = entry.getValue();

        if (predicate == PredicateSelection.INVARIANT)
          continue;

        if (entry.getKey().test(item, distanceProvider, predicate == PredicateSelection.NEGATIVE))
          continue;

        doesMatch = false;
        break;
      }

      if (doesMatch)
        result.add(item);
    }

    return result;
  }

  public void extendSortingEnvironment(InterpretationEnvironment environment, Player player) {
    for (var index = 0; index < sortingSelections.size(); ++index)
      sortingSelections.get(index).active = index == selectedSortingSelectionIndex;

    environment
      .withVariable("sorting_selections", this.sortingSelections)
      .withVariable("has_permission", PluginPermission.FEATURE_SORT.has(player));
  }

  public void extendFilteringEnvironment(InterpretationEnvironment environment, Player player) {
    var mappedSelections = new ArrayList<FilteringCriterionSelection>();

    for (var entry : this.filteringSelections.entrySet()) {
      var selection = new FilteringCriterionSelection(entry.getKey(), entry.getValue());
      selection.active = selection.criterion == selectedFilteringCriteria;
      mappedSelections.add(selection);
    }

    environment
      .withVariable("filtering_selections", mappedSelections)
      .withVariable("has_permission", PluginPermission.FEATURE_FILTER.has(player));
  }

  public JsonObject toJson() {
    var result = new JsonObject();

    var sortingSelectionsObject = new JsonObject();

    for (var sortingSelection : sortingSelections)
      sortingSelectionsObject.addProperty(String.valueOf(sortingSelection.criterion.ordinal()), String.valueOf(sortingSelection.selection.ordinal()));

    result.add("sortingSelections", sortingSelectionsObject);
    result.addProperty("selectedSortingSelectionIndex", selectedSortingSelectionIndex);

    var filteringSelectionsObject = new JsonObject();

    for (var filteringSelection : filteringSelections.entrySet())
      filteringSelectionsObject.addProperty(String.valueOf(filteringSelection.getKey().ordinal()), filteringSelection.getValue().ordinal());

    result.add("filteringSelections", filteringSelectionsObject);
    result.addProperty("selectedFilteringCriteria", selectedFilteringCriteria.ordinal());

    return result;
  }

  private void loadFromJson(JsonObject json) {
    sortingSelections = makeDefaultSortingSelections();

    var sortingSelectionsObject = json.getAsJsonObject("sortingSelections");

    for (var sortingSelectionEntry : sortingSelectionsObject.entrySet()) {
      var criterionOrdinal = Integer.parseInt(sortingSelectionEntry.getKey());
      var criterion = ShopSortingCriteria.values.byOrdinalIfEnabledOrNull(criterionOrdinal );

      if (criterion == null)
        continue;

      var selectionOrdinal = sortingSelectionEntry.getValue().getAsInt();
      var selection = SortingSelection.byOrdinalOrFirst(selectionOrdinal);

      for (var index = 0; index < sortingSelections.size(); ++index) {
        if (sortingSelections.get(index).criterion != criterion)
          continue;

        sortingSelections.set(index, new SortingCriterionSelection(criterion, selection));
        break;
      }
    }

    selectedSortingSelectionIndex = json.get("selectedSortingSelectionIndex").getAsInt();

    if (selectedSortingSelectionIndex < 0 || selectedSortingSelectionIndex >= sortingSelections.size())
      selectedSortingSelectionIndex = 0;

    filteringSelections = makeDefaultFilteringSelections();

    var filteringSelectionsObject = json.getAsJsonObject("filteringSelections");

    for (var criterion : ShopFilteringCriteria.values.getEnabledValues()) {
      var criterionOrdinal = String.valueOf(criterion.ordinal());
      var selectionOrdinalPrimitive = filteringSelectionsObject.getAsJsonPrimitive(criterionOrdinal);

      if (selectionOrdinalPrimitive == null)
        continue;

      var selectionOrdinal = selectionOrdinalPrimitive.getAsInt();

      filteringSelections.put(criterion, PredicateSelection.byOrdinalOrFirst(selectionOrdinal));
    }

    var criterionOrdinal = json.getAsJsonPrimitive("selectedFilteringCriteria").getAsInt();
    selectedFilteringCriteria = ShopFilteringCriteria.values.byOrdinalIfEnabledOrNull(criterionOrdinal);

    if (selectedFilteringCriteria == null)
      selectedFilteringCriteria = ShopFilteringCriteria.values.firstEnabled();
  }

  public static SelectionState fromJson(JsonObject json) {
    var result = new SelectionState(false);
    result.loadFromJson(json);
    return result;
  }

  private static ArrayList<SortingCriterionSelection> makeDefaultSortingSelections() {
    var result = new ArrayList<SortingCriterionSelection>();

    for (var criterion : ShopSortingCriteria.values.getEnabledValues())
      result.add(new SortingCriterionSelection(criterion, SortingSelection.INACTIVE));

    return result;
  }

  private static TreeMap<ShopFilteringCriteria, PredicateSelection> makeDefaultFilteringSelections() {
    var result = new TreeMap<ShopFilteringCriteria, PredicateSelection>();

    for (var criteria : ShopFilteringCriteria.values.getEnabledValues())
      result.put(criteria, PredicateSelection.INVARIANT);

    return result;
  }
}
