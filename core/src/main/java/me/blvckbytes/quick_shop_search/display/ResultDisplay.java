package me.blvckbytes.quick_shop_search.display;

import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.ShopUpdate;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ResultDisplay implements ShopDistanceProvider {

  private static final long SENTINEL_DISTANCE_UN_COMPUTABLE = -1;

  private final PlatformScheduler scheduler;
  private final @Nullable ClickTranslator clickTranslator;
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
    PlatformScheduler scheduler,
    ConfigKeeper<MainSection> config,
    Player player,
    DisplayData displayData,
    SelectionState selectionState,
    boolean useClickTranslator
  ) {
    this.scheduler = scheduler;
    this.config = config;
    this.asyncQueue = new AsyncTaskQueue(scheduler);
    this.player = player;
    this.playerLocation = player.getLocation();
    this.displayData = displayData;
    this.shopDistanceByShopId = new HashMap<>();
    this.slotMap = new CachedShop[9 * 6];
    this.selectionState = selectionState;
    this.clickTranslator = useClickTranslator ? new ClickTranslator() : null;

    onConfigReload(false);

    // Within async context already, see corresponding command
    applyFiltering();
    applySorting();
    show();
  }

  public void onShopUpdate(CachedShop shop, ShopUpdate update) {
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
    var anyPageSlot = anyOfSet(config.rootSection.resultDisplay.items.nextPage.getDisplaySlots());
    this.pageEnvironment = new EvaluationEnvironmentBuilder()
      .withLiveVariable("current_page", () -> this.currentPage)
      .withLiveVariable("number_pages", () -> this.numberOfPages)
      .withLiveVariable("click_translation", () -> anyPageSlot == null ? null : getClickTranslation(anyPageSlot))
      .build(config.rootSection.resultDisplay.inventoryEnvironment);

    var anySortingSlot = anyOfSet(config.rootSection.resultDisplay.items.sorting.getDisplaySlots());
    this.sortingEnvironment = this.selectionState
      .makeSortingEnvironment(player, config.rootSection)
      .withLiveVariable("click_translation", () -> anySortingSlot == null ? null : getClickTranslation(anySortingSlot))
      .build(pageEnvironment);

    var anyFilteringSlot = anyOfSet(config.rootSection.resultDisplay.items.filtering.getDisplaySlots());
    this.filteringEnvironment = this.selectionState
      .makeFilteringEnvironment(player, config.rootSection)
      .withLiveVariable("click_translation", () -> anyFilteringSlot == null ? null : getClickTranslation(anyFilteringSlot))
      .build(pageEnvironment);

    this.setupTranslator();

    if (redraw) {
      applyFiltering();
      applySorting();
      show();
    }
  }

  private @Nullable Integer anyOfSet(Set<Integer> set) {
    var iterator = set.iterator();

    if (!iterator.hasNext())
      return null;

    return iterator.next();
  }

  private void setupTranslator() {
    if (this.clickTranslator == null)
      return;

    var pageButtonSlots = new HashSet<Integer>();
    pageButtonSlots.addAll(config.rootSection.resultDisplay.items.previousPage.getDisplaySlots());
    pageButtonSlots.addAll(config.rootSection.resultDisplay.items.nextPage.getDisplaySlots());

    clickTranslator.setup(
      config.rootSection.resultDisplay.getRows(),
      List.of(
        config.rootSection.resultDisplay.getPaginationSlots(),
        // Since previous- and next-page also use the same environment, and to make things easier in general,
        // have them both share the same translation slot-group as well.
        pageButtonSlots
      ),
      slot -> {

        if (config.rootSection.resultDisplay.getPaginationSlots().contains(slot))
          return EnumSet.of(TranslatedClick.LEFT, TranslatedClick.SHIFT_LEFT, TranslatedClick.RIGHT);

        var targetSection = config.rootSection.resultDisplay.getItemSectionBySlot(slot);

        if (targetSection == config.rootSection.resultDisplay.items.sorting)
          return EnumSet.of(TranslatedClick.LEFT, TranslatedClick.RIGHT, TranslatedClick.DROP_ONE, TranslatedClick.DROP_ALL);

        if (targetSection == config.rootSection.resultDisplay.items.filtering)
          return EnumSet.of(TranslatedClick.LEFT, TranslatedClick.RIGHT, TranslatedClick.DROP_ALL);

        if (
          targetSection == config.rootSection.resultDisplay.items.nextPage
          || targetSection == config.rootSection.resultDisplay.items.previousPage
        )
          return EnumSet.of(TranslatedClick.LEFT, TranslatedClick.RIGHT);

        return null;
      }
    );
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
    this.filteredUnSortedShops = this.selectionState.applyFilter(displayData.shops(), this);

    var oldNumberOfPages = this.numberOfPages;
    var numberOfDisplaySlots = config.rootSection.resultDisplay.getPaginationSlots().size();

    this.numberOfPages = Math.max(1, (int) Math.ceil(filteredUnSortedShops.size() / (double) numberOfDisplaySlots));

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

    scheduler.runAtEntity(player, scheduleTask -> player.openInventory(inventory));
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

  public @Nullable TranslatedClick updateAndTranslate(InventoryClickEvent event) {
    if (this.clickTranslator == null)
      return TranslatedClick.fromClickEvent(event);

    var result = clickTranslator.updateOrTranslate(event);

    if (result == null)
      renderItems();

    return result;
  }

  private void renderItems() {
    var displaySlots = config.rootSection.resultDisplay.getPaginationSlots();
    var itemsIndex = (currentPage - 1) * displaySlots.size();
    var numberOfItems = filteredSortedShops.size();

    for (var slot : displaySlots) {
      var currentSlot = itemsIndex++;

      if (currentSlot >= numberOfItems) {
        slotMap[slot] = null;
        inventory.setItem(slot, null);
        continue;
      }

      var cachedShop = filteredSortedShops.get(currentSlot);

      inventory.setItem(slot, cachedShop.getRepresentativeBuildable().build(
        getExtendedShopEnvironment(slot, cachedShop)
      ));

      slotMap[slot] = cachedShop;
    }

    config.rootSection.resultDisplay.items.previousPage.renderInto(inventory, pageEnvironment);
    config.rootSection.resultDisplay.items.nextPage.renderInto(inventory, pageEnvironment);
    config.rootSection.resultDisplay.items.sorting.renderInto(inventory, sortingEnvironment);
    config.rootSection.resultDisplay.items.filtering.renderInto(inventory, filteringEnvironment);
    config.rootSection.resultDisplay.items.filler.renderInto(inventory, pageEnvironment);
  }

  public IEvaluationEnvironment getExtendedShopEnvironment(int slot, CachedShop cachedShop) {
    var distance = getShopDistance(cachedShop);
    var isOtherWorld = distance < 0;

    return cachedShop
      .getShopEnvironment()
      .duplicate()
      .withStaticVariable("distance", distance)
      .withLiveVariable("click_translation", () -> getClickTranslation(slot))
      .withStaticVariable(
        "can_teleport",
        isOtherWorld
          ? PluginPermission.FEATURE_TELEPORT_OTHER_WORLD.has(player)
          : PluginPermission.FEATURE_TELEPORT.has(player)
      )
      .withStaticVariable(
        "can_interact",
        isOtherWorld
          ? PluginPermission.FEATURE_INTERACT_OTHER_WORLD.has(player)
          : PluginPermission.FEATURE_INTERACT.has(player)
      )
      .build(pageEnvironment);
  }

  private Inventory makeInventory() {
    return config.rootSection.resultDisplay.createInventory(pageEnvironment);
  }

  private @Nullable String getClickTranslation(int slot) {
    String clickTranslation = null;

    if (clickTranslator != null) {
      var slotTranslation = clickTranslator.getCurrent(slot);

      if (slotTranslation != null)
        clickTranslation = slotTranslation.name();
    }

    return clickTranslation;
  }
}
