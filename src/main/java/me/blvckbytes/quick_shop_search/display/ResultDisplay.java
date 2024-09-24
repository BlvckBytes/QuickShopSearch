package me.blvckbytes.quick_shop_search.display;

import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.quick_shop_search.CachedShop;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ResultDisplay {

  public static final int INVENTORY_N_ROWS = 6;
  public static final int BACKWARDS_SLOT_ID = 45;
  public static final int FORWARDS_SLOT_ID = 53;
  public static final int SORTING_SLOT_ID = 47;
  public static final int FILTERING_SLOT_ID = 51;

  private static final Set<Integer> DISPLAY_SLOTS;
  private static final Set<Integer> FILLER_SLOTS;

  static {
    DISPLAY_SLOTS = IntStream.range(0, (INVENTORY_N_ROWS - 1) * 9).boxed().collect(Collectors.toSet());
    FILLER_SLOTS = IntStream.range((INVENTORY_N_ROWS - 1) * 9, INVENTORY_N_ROWS * 9).boxed().collect(Collectors.toSet());

    FILLER_SLOTS.remove(BACKWARDS_SLOT_ID);
    FILLER_SLOTS.remove(FORWARDS_SLOT_ID);
    FILLER_SLOTS.remove(SORTING_SLOT_ID);
    FILLER_SLOTS.remove(FILTERING_SLOT_ID);
  }

  private final Plugin plugin;
  private final AsyncTaskQueue asyncQueue;
  private final MainSection mainSection;

  private final List<CachedShop> unselectedShops;
  private List<CachedShop> selectedShops;

  public final Player player;
  private final Map<Integer, CachedShop> slotMap;
  private int numberOfPages;

  public final SelectionState selectionState;

  private final IEvaluationEnvironment pageEnvironment;
  private final IEvaluationEnvironment sortingEnvironment;
  private final IEvaluationEnvironment filteringEnvironment;

  private Inventory inventory;
  private int currentPage = 1;

  public ResultDisplay(
    Plugin plugin,
    MainSection mainSection,
    Player player,
    List<CachedShop> shops,
    SelectionState selectionState
  ) {
    this.plugin = plugin;
    this.asyncQueue = new AsyncTaskQueue(plugin);
    this.mainSection = mainSection;
    this.player = player;
    this.unselectedShops = shops;
    this.slotMap = new HashMap<>();
    this.selectionState = selectionState;

    this.sortingEnvironment = this.selectionState.makeSortingEnvironment();
    this.filteringEnvironment = this.selectionState.makeFilteringEnvironment();
    this.pageEnvironment = mainSection.getBaseEnvironment()
      .withLiveVariable("current_page", () -> this.currentPage)
      .withLiveVariable("number_pages", () -> this.numberOfPages)
      .build();

    // Within async context already, see corresponding command
    applyFiltering();
    applySorting();
    show();
  }

  public @Nullable CachedShop getShopCorrespondingToSlot(int slot) {
    return slotMap.get(slot);
  }

  public boolean isInventory(Inventory inventory) {
    return inventory == this.inventory;
  }

  public void cleanup(boolean close) {
    if (this.inventory != null)
      this.inventory.clear();
    this.slotMap.clear();

    if (close && player.getOpenInventory().getTopInventory() == inventory)
      player.closeInventory();
  }

  public void nextPage() {
    asyncQueue.enqueue(() -> {
      if (currentPage == numberOfPages)
        return;

      ++currentPage;
      show();
    });
  }

  public void previousPage() {
    asyncQueue.enqueue(() -> {
      if (currentPage == 1)
        return;

      --currentPage;
      show();
    });
  }

  public void firstPage() {
    asyncQueue.enqueue(() -> {
      if (currentPage == 1)
        return;

      currentPage = 1;
      show();
    });
  }

  public void lastPage() {
    asyncQueue.enqueue(() -> {
      if (currentPage == numberOfPages)
        return;

      currentPage = numberOfPages;
      show();
    });
  }

  public void nextSortingCriterion() {
    asyncQueue.enqueue(() -> {
      this.selectionState.nextSortingCriterion();
      applySorting();
      renderItems();
    });
  }

  public void toggleSortingOrder() {
    asyncQueue.enqueue(() -> {
      this.selectionState.toggleSortingOrder();
      applySorting();
      renderItems();
    });
  }

  public void nextFilteringCriterion() {
    asyncQueue.enqueue(() -> {
      this.selectionState.nextFilteringCriterion();
      renderItems();
    });
  }

  public void nextFilteringState() {
    asyncQueue.enqueue(() -> {
      this.selectionState.nextFilteringState();

      int pageCountDelta = applyFiltering();
      applySorting();

      // Need to update the UI-title
      if (pageCountDelta != 0)
        show();
      else
        renderItems();
    });
  }

  private int applyFiltering() {
    this.selectedShops = this.selectionState.applyFilter(unselectedShops);

    var oldNumberOfPages = this.numberOfPages;
    this.numberOfPages = (int) Math.ceil(selectedShops.size() / (double) DISPLAY_SLOTS.size());

    var pageCountDelta = this.numberOfPages - oldNumberOfPages;

    // Try to stay on the current page, if possible
    if (pageCountDelta < 0)
      this.currentPage = 1;

    return pageCountDelta;
  }

  private void applySorting() {
    this.selectionState.applySort(selectedShops);
  }

  private void show() {
    // Avoid the case of the client not accepting opening the new inventory
    // and then being able to take items out of there. This way, we're safe.
    this.cleanup(false);

    inventory = makeInventory();

    renderItems();

    Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inventory));
  }

  private void renderItems() {
    var itemsIndex = (currentPage - 1) * DISPLAY_SLOTS.size();
    var numberOfItems = selectedShops.size();

    for (var slot : DISPLAY_SLOTS) {
      var currentSlot = itemsIndex++;

      if (currentSlot >= numberOfItems) {
        slotMap.remove(slot);
        inventory.setItem(slot, null);
        continue;
      }

      var cachedShop = selectedShops.get(currentSlot);

      inventory.setItem(
        slot,
        cachedShop.getRepresentativeBuildable().build(
          cachedShop.getShopEnvironment().build(pageEnvironment)
        )
      );

      slotMap.put(slot, cachedShop);
    }

    inventory.setItem(
      BACKWARDS_SLOT_ID,
      mainSection.resultDisplay.previousPage.build(pageEnvironment)
    );

    inventory.setItem(
      FORWARDS_SLOT_ID,
      mainSection.resultDisplay.nextPage.build(pageEnvironment)
    );

    inventory.setItem(
      SORTING_SLOT_ID,
      mainSection.resultDisplay.sorting.build(sortingEnvironment)
    );

    inventory.setItem(
      FILTERING_SLOT_ID,
      mainSection.resultDisplay.filtering.build(filteringEnvironment)
    );

    if (mainSection.resultDisplay.filler != null) {
      var fillerItem = mainSection.resultDisplay.filler.build();
      for (var fillerSlot : FILLER_SLOTS)
        inventory.setItem(fillerSlot, fillerItem);
    }
  }

  private Inventory makeInventory() {
    var title = mainSection.resultDisplay.title.stringify(pageEnvironment);
    return Bukkit.createInventory(null, 9 * INVENTORY_N_ROWS, title);
  }
}
