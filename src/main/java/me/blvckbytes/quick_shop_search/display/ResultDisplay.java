package me.blvckbytes.quick_shop_search.display;

import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.ShopUpdate;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ResultDisplay implements ShopDistanceProvider {

  private static final long SENTINEL_DISTANCE_UN_COMPUTABLE = -1;

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
  private final ConfigKeeper<MainSection> config;

  private final DisplayData displayData;
  private final Map<Long, Long> shopDistanceByShopId;
  private List<CachedShop> filteredUnSortedShops;
  private List<CachedShop> filteredSortedShops;

  public final Player player;
  private final Location playerLocation;
  private final CachedShop[] slotMap;
  private int numberOfPages;
  public final SelectionState selectionState;

  private IEvaluationEnvironment pageEnvironment;
  private IEvaluationEnvironment sortingEnvironment;
  private IEvaluationEnvironment filteringEnvironment;

  private Inventory inventory;
  private int currentPage = 1;

  public ResultDisplay(
    Plugin plugin,
    ConfigKeeper<MainSection> config,
    Player player,
    DisplayData displayData,
    SelectionState selectionState
  ) {
    this.plugin = plugin;
    this.config = config;
    this.asyncQueue = new AsyncTaskQueue(plugin);
    this.player = player;
    this.playerLocation = player.getLocation();
    this.displayData = displayData;
    this.shopDistanceByShopId = new HashMap<>();
    this.slotMap = new CachedShop[INVENTORY_N_ROWS * 9];
    this.selectionState = selectionState;

    onConfigReload(false);

    // Within async context already, see corresponding command
    applyFiltering();
    applySorting();
    show();
  }

  public void onShopUpdate(CachedShop shop, ShopUpdate update) {
    if (displayData.isBackedByRegistry()) {
      updateAll();
      return;
    }

    if (update == ShopUpdate.ITEM_CHANGED) {
      if (!displayData.contains(shop))
        return;

      if (!displayData.doesMatchQuery(shop)) {
        displayData.remove(shop);
        updateAll();
      }

      return;
    }

    if (update == ShopUpdate.CREATED) {
      if (!displayData.doesMatchQuery(shop))
        return;

      displayData.add(shop);
      updateAll();
      return;
    }

    if (update == ShopUpdate.REMOVED) {
      if (!displayData.contains(shop))
        return;

      displayData.remove(shop);
      updateAll();
      return;
    }

    updateAll();
  }

  private void updateAll() {
    asyncQueue.enqueue(() -> {
      applyFiltering();
      applySorting();
      show();
    });
  }

  public void onConfigReload(boolean redraw) {
    this.sortingEnvironment = this.selectionState.makeSortingEnvironment(player, config.rootSection);
    this.filteringEnvironment = this.selectionState.makeFilteringEnvironment(player, config.rootSection);
    this.pageEnvironment = config.rootSection.getBaseEnvironment()
      .withLiveVariable("current_page", () -> this.currentPage)
      .withLiveVariable("number_pages", () -> this.numberOfPages)
      .build();

    if (redraw)
      show();
  }

  public @Nullable CachedShop getShopCorrespondingToSlot(int slot) {
    return slotMap[slot];
  }

  public boolean isInventory(Inventory inventory) {
    return inventory == this.inventory;
  }

  public void cleanup(boolean close) {
    if (this.inventory != null)
      this.inventory.clear();

    for (var i = 0; i < slotMap.length; ++i)
      this.slotMap[i] = null;

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

  public void nextSortingSelection() {
    asyncQueue.enqueue(() -> {
      this.selectionState.nextSortingSelection();
      applySorting();
      renderItems();
    });
  }

  public void nextSortingOrder() {
    asyncQueue.enqueue(() -> {
      this.selectionState.nextSortingOrder();
      applySorting();
      renderItems();
    });
  }

  public void moveSortingSelectionDown() {
    asyncQueue.enqueue(() -> {
      this.selectionState.moveSortingSelectionDown();
      applySorting();
      renderItems();
    });
  }

  public void resetSortingState() {
    asyncQueue.enqueue(() -> {
      this.selectionState.resetSorting();
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
      afterFilterChange();
    });
  }

  public void resetFilteringState() {
    asyncQueue.enqueue(() -> {
      this.selectionState.resetFiltering();
      afterFilterChange();
    });
  }

  private void afterFilterChange() {
    int pageCountDelta = applyFiltering();
    applySorting();

    // Need to update the UI-title
    if (pageCountDelta != 0)
      show();
    else
      renderItems();
  }

  private int applyFiltering() {
    this.filteredUnSortedShops = this.selectionState.applyFilter(displayData.shops());

    var oldNumberOfPages = this.numberOfPages;
    this.numberOfPages = Math.max(1, (int) Math.ceil(filteredUnSortedShops.size() / (double) DISPLAY_SLOTS.size()));

    var pageCountDelta = this.numberOfPages - oldNumberOfPages;

    // Try to stay on the current page, if possible
    if (pageCountDelta < 0)
      this.currentPage = 1;

    return pageCountDelta;
  }

  private void applySorting() {
    this.filteredSortedShops = new ArrayList<>(this.filteredUnSortedShops);
    this.selectionState.applySort(this, this.filteredSortedShops);
  }

  private void show() {
    // Avoid the case of the client not accepting opening the new inventory
    // and then being able to take items out of there. This way, we're safe.
    this.cleanup(false);

    inventory = makeInventory();

    renderItems();

    Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inventory));
  }

  @Override
  public long getShopDistance(CachedShop cachedShop) {
    var shopId = cachedShop.handle.getShopId();
    var distance = shopDistanceByShopId.get(shopId);

    if (distance == null) {
      var shopLocation = cachedShop.handle.getLocation();

      if (shopLocation.getWorld() != playerLocation.getWorld())
        distance = SENTINEL_DISTANCE_UN_COMPUTABLE;
      else
        distance = Math.round(playerLocation.distance(shopLocation));
    }

    else if (distance == SENTINEL_DISTANCE_UN_COMPUTABLE)
      return -1;

    shopDistanceByShopId.put(shopId, distance);

    return distance;
  }

  private void renderItems() {
    var itemsIndex = (currentPage - 1) * DISPLAY_SLOTS.size();
    var numberOfItems = filteredSortedShops.size();

    for (var slot : DISPLAY_SLOTS) {
      var currentSlot = itemsIndex++;

      if (currentSlot >= numberOfItems) {
        slotMap[slot] = null;
        inventory.setItem(slot, null);
        continue;
      }

      var cachedShop = filteredSortedShops.get(currentSlot);

      inventory.setItem(slot, cachedShop.getRepresentativeBuildable().build(
        getDistanceExtendedShopEnvironment(cachedShop)
      ));

      slotMap[slot] = cachedShop;
    }

    inventory.setItem(
      BACKWARDS_SLOT_ID,
      config.rootSection.resultDisplay.previousPage.build(pageEnvironment)
    );

    inventory.setItem(
      FORWARDS_SLOT_ID,
      config.rootSection.resultDisplay.nextPage.build(pageEnvironment)
    );

    inventory.setItem(
      SORTING_SLOT_ID,
      config.rootSection.resultDisplay.sorting.build(sortingEnvironment)
    );

    inventory.setItem(
      FILTERING_SLOT_ID,
      config.rootSection.resultDisplay.filtering.build(filteringEnvironment)
    );

    if (config.rootSection.resultDisplay.filler != null) {
      var fillerItem = config.rootSection.resultDisplay.filler.build();
      for (var fillerSlot : FILLER_SLOTS)
        inventory.setItem(fillerSlot, fillerItem);
    }
  }

  public IEvaluationEnvironment getDistanceExtendedShopEnvironment(CachedShop cachedShop) {
    return cachedShop
      .getShopEnvironment()
      .duplicate()
      .withStaticVariable("distance", getShopDistance(cachedShop))
      .build(pageEnvironment);
  }

  private Inventory makeInventory() {
    var title = config.rootSection.resultDisplay.title.stringify(pageEnvironment);
    return Bukkit.createInventory(null, 9 * INVENTORY_N_ROWS, title);
  }
}
