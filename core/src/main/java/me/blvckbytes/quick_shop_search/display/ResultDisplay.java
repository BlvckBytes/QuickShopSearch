package me.blvckbytes.quick_shop_search.display;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.obj.QUser;
import com.ghostchu.quickshop.obj.QUserImpl;
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
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ResultDisplay implements DynamicPropertyProvider {

  private static final long SENTINEL_DISTANCE_UN_COMPUTABLE = -1;

  private final PlatformScheduler scheduler;
  private final AsyncTaskQueue asyncQueue;
  private final ConfigKeeper<MainSection> config;

  private final DisplayData displayData;
  private final Map<Long, Long> shopDistanceByShopId;
  private final Map<String, Double> balanceByCurrencyCache;
  private List<CachedShop> filteredUnSortedShops;
  private List<CachedShop> filteredSortedShops;

  public final Player player;
  private final QUser playerUser;
  private final Location playerLocation;
  private final CachedShop[] slotMap;
  private int numberOfPages;
  public final SelectionState selectionState;

  // If players move to their own inventory and close the UI quickly enough, the server will send back a packet
  // undoing that slot which assumed the top-inventory to still be open, and thus the undo won't work. For survival,
  // it's merely cosmetic, but for creative, the client will actually call this item into existence. While not
  // necessarily critical, it just makes the plugin look bad. On closing the inventory, if the last move happened
  // within a certain threshold of time, let's just update the player's inventory, as to make that ghost-item vanish.
  public long lastMoveToOwnInventoryStamp;

  private IEvaluationEnvironment pageEnvironment;
  private IEvaluationEnvironment sortingEnvironment;
  private IEvaluationEnvironment filteringEnvironment;

  private Inventory inventory;
  private int currentPage = 1;

  // TODO: Do not render whole UI only because selection-state is changed

  public ResultDisplay(
    PlatformScheduler scheduler,
    ConfigKeeper<MainSection> config,
    Player player,
    DisplayData displayData,
    SelectionState selectionState
  ) {
    this.scheduler = scheduler;
    this.config = config;
    this.asyncQueue = new AsyncTaskQueue(scheduler);
    this.player = player;
    this.playerUser = QUserImpl.createFullFilled(player);
    this.playerLocation = player.getLocation();
    this.displayData = displayData;
    this.shopDistanceByShopId = new HashMap<>();
    this.balanceByCurrencyCache = new HashMap<>();
    this.slotMap = new CachedShop[9 * 6];
    this.selectionState = selectionState;

    onConfigReload(false);

    // Within async context already, see corresponding command
    applyFiltering();
    applySorting();
    show();
  }

  public void onShopUpdate(CachedShop shop, ShopUpdate update) {
    if (!PluginPermission.FEATURE_LIVE_UPDATES.has(player))
      return;

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
    this.pageEnvironment = new EvaluationEnvironmentBuilder()
      .withLiveVariable("current_page", () -> this.currentPage)
      .withLiveVariable("number_pages", () -> this.numberOfPages)
      .build(config.rootSection.resultDisplay.inventoryEnvironment);

    this.sortingEnvironment = this.selectionState
      .makeSortingEnvironment(player, config.rootSection)
      .build(pageEnvironment);

    this.filteringEnvironment = this.selectionState
      .makeFilteringEnvironment(player, config.rootSection)
      .build(pageEnvironment);

    if (redraw) {
      applyFiltering();
      applySorting();
      show();
    }
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

  @Override
  public double getPlayerBalanceForShopCurrency(CachedShop cachedShop) {
    return this.balanceByCurrencyCache.computeIfAbsent(cachedShop.handle.getCurrency(), currency -> (
      QuickShop.getInstance().getEconomy().getBalance(playerUser, player.getWorld(), currency)
    ));
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
        getDistanceExtendedShopEnvironment(cachedShop)
      ));

      slotMap[slot] = cachedShop;
    }

    config.rootSection.resultDisplay.items.previousPage.renderInto(inventory, pageEnvironment);
    config.rootSection.resultDisplay.items.nextPage.renderInto(inventory, pageEnvironment);
    config.rootSection.resultDisplay.items.sorting.renderInto(inventory, sortingEnvironment);
    config.rootSection.resultDisplay.items.filtering.renderInto(inventory, filteringEnvironment);
    config.rootSection.resultDisplay.items.filler.renderInto(inventory, pageEnvironment);
  }

  public IEvaluationEnvironment getDistanceExtendedShopEnvironment(CachedShop cachedShop) {
    var distance = getShopDistance(cachedShop);
    var isOtherWorld = distance < 0;

    return cachedShop
      .getShopEnvironment()
      .duplicate()
      .withStaticVariable("distance", distance)
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
}
