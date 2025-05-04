package me.blvckbytes.quick_shop_search.display.result;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.obj.QUser;
import com.ghostchu.quickshop.api.shop.ShopType;
import com.ghostchu.quickshop.obj.QUserImpl;
import com.tcoded.folialib.impl.PlatformScheduler;
import it.unimi.dsi.fastutil.longs.Long2LongAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.item_predicate_parser.predicate.StringifyState;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.cache.ShopUpdate;
import me.blvckbytes.quick_shop_search.command.SearchFlag;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.integration.player_warps.IPlayerWarpsIntegration;
import me.blvckbytes.quick_shop_search.integration.player_warps.PlayerWarpData;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ResultDisplay implements DynamicPropertyProvider {

  private static final PlayerWarpData PLAYER_WARP_NULL_SENTINEL = new PlayerWarpData(null, null, null, false);

  private final PlatformScheduler scheduler;
  private final @Nullable IPlayerWarpsIntegration playerWarpsIntegration;
  private final AsyncTaskQueue asyncQueue;
  private final ConfigKeeper<MainSection> config;

  private final DisplayData displayData;
  private final Long2LongMap shopDistanceByShopId;
  private final Long2ObjectMap<CalculatedFees> shopFeesByShopId;
  private final Long2ObjectMap<@Nullable PlayerWarpData> nearestPlayerWarpByShopId;
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
  private IEvaluationEnvironment activeSearchEnvironment;
  private boolean hasAnyActiveSearchProperties;

  private Inventory inventory;
  private int currentPage = 1;

  public ResultDisplay(
    PlatformScheduler scheduler,
    @Nullable IPlayerWarpsIntegration playerWarpsIntegration,
    ConfigKeeper<MainSection> config,
    Player player,
    DisplayData displayData,
    SelectionState selectionState
  ) {
    this.scheduler = scheduler;
    this.playerWarpsIntegration = playerWarpsIntegration;
    this.config = config;
    this.asyncQueue = new AsyncTaskQueue(scheduler);
    this.player = player;
    this.playerUser = QUserImpl.createFullFilled(player);
    this.playerLocation = player.getLocation();
    this.displayData = displayData;
    this.shopDistanceByShopId = new Long2LongAVLTreeMap();
    this.shopFeesByShopId = new Long2ObjectAVLTreeMap<>();
    this.nearestPlayerWarpByShopId = new Long2ObjectAVLTreeMap<>();
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

    var activeSearchProperties = new HashMap<String, Object>();

    activeSearchProperties.put(
      "predicate",
      displayData.query() == null
        ? null
        : new StringifyState(true).appendPredicate(displayData.query()).toString()
    );

    activeSearchProperties.put("owner", displayData.searchFlagsContainer().getWithMapper(SearchFlag.OWNER, OfflinePlayer::getName));
    activeSearchProperties.put("radius", displayData.searchFlagsContainer().get(SearchFlag.RADIUS));
    activeSearchProperties.put("price", displayData.searchFlagsContainer().get(SearchFlag.PRICE));
    activeSearchProperties.put("max_price", displayData.searchFlagsContainer().get(SearchFlag.MAX_PRICE));
    activeSearchProperties.put("min_price", displayData.searchFlagsContainer().get(SearchFlag.MIN_PRICE));

    var activeSearchEnvironmentBuilder = new EvaluationEnvironmentBuilder();

    this.hasAnyActiveSearchProperties = false;

    for (var propertyEntry : activeSearchProperties.entrySet()) {
      var currentValue = propertyEntry.getValue();

      if (currentValue != null)
        this.hasAnyActiveSearchProperties = true;

      activeSearchEnvironmentBuilder.withStaticVariable(propertyEntry.getKey(), currentValue);
    }

    this.activeSearchEnvironment = activeSearchEnvironmentBuilder.build(config.rootSection.builtBaseEnvironment);

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
      renderSortingItem();
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
      renderFilteringItem();
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
    for (var i = 0; i < slotMap.length; ++i)
      this.slotMap[i] = null;

    var priorInventory = inventory;
    inventory = makeInventory();

    renderItems();

    scheduler.runAtEntity(player, scheduleTask -> {
      // Make sure to open the newly rendered inventory first as to avoid flicker
      player.openInventory(inventory);

      // Avoid the case of the client not accepting opening the new inventory
      // and then being able to take items out of there. This way, we're safe.
      if (priorInventory != null)
        priorInventory.clear();
    });
  }

  public CalculatedFees getShopFees(CachedShop cachedShop) {
    var shopId = cachedShop.handle.getShopId();

    CalculatedFees shopFees = shopFeesByShopId.get(shopId);

    if (shopFees == null) {
      var feesValues = config.rootSection.fees.decideFees(player, cachedShop);
      shopFees = CalculatedFees.calculateFor(feesValues, cachedShop);
      shopFeesByShopId.put(shopId, shopFees);
    }

    return shopFees;
  }

  @Override
  public long getShopDistance(CachedShop cachedShop) {
    var shopId = cachedShop.handle.getShopId();
    var distance = shopDistanceByShopId.getOrDefault(shopId, -2);

    if (distance == -2) {
      var shopLocation = cachedShop.handle.getLocation();

      if (shopLocation.getWorld() != playerLocation.getWorld())
        distance = -1L;
      else
        distance = Math.round(playerLocation.distance(shopLocation));
    }

    else if (distance == -1)
      return -1;

    shopDistanceByShopId.put(shopId, distance);

    return distance;
  }

  @Override
  public double getPlayerBalanceForShopCurrency(CachedShop cachedShop) {
    return QuickShop.getInstance().getEconomy().getBalance(
      playerUser,
      Objects.requireNonNull(cachedShop.handle.getLocation().getWorld()),
      cachedShop.handle.getCurrency()
    );
  }

  private void renderSortingItem() {
    config.rootSection.resultDisplay.items.sorting.renderInto(inventory, sortingEnvironment);
  }

  private void renderFilteringItem() {
    config.rootSection.resultDisplay.items.filtering.renderInto(inventory, filteringEnvironment);
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
        getExtendedShopEnvironment(cachedShop)
      ));

      slotMap[slot] = cachedShop;
    }

    // Render filler first, such that it may be overridden by conditionally displayed items
    config.rootSection.resultDisplay.items.filler.renderInto(inventory, pageEnvironment);

    config.rootSection.resultDisplay.items.previousPage.renderInto(inventory, pageEnvironment);
    config.rootSection.resultDisplay.items.nextPage.renderInto(inventory, pageEnvironment);
    renderSortingItem();
    renderFilteringItem();

    if (hasAnyActiveSearchProperties)
      config.rootSection.resultDisplay.items.activeSearch.renderInto(inventory, activeSearchEnvironment);
  }

  public @Nullable PlayerWarpData getNearestPlayerWarp(CachedShop cachedShop) {
    if (playerWarpsIntegration == null)
      return null;

    var shopId = cachedShop.handle.getShopId();
    var result = nearestPlayerWarpByShopId.get(shopId);

    if (result == null) {
      result = playerWarpsIntegration.locateNearestWithinRange(player, cachedShop.handle.getLocation(), config.rootSection.playerWarpsIntegration.nearestWarpBlockRadius);

      if (result == null)
        result = PLAYER_WARP_NULL_SENTINEL;

      nearestPlayerWarpByShopId.put(shopId, result);
    }

    if (result == PLAYER_WARP_NULL_SENTINEL)
      return null;

    return result;
  }

  public IEvaluationEnvironment getExtendedShopEnvironment(CachedShop cachedShop) {
    var distance = getShopDistance(cachedShop);
    var isOtherWorld = distance < 0;
    var fees = getShopFees(cachedShop);
    var nearestPlayerWarp = getNearestPlayerWarp(cachedShop);

    var shopManager = QuickShopAPI.getInstance().getShopManager();

    return cachedShop
      .getShopEnvironment()
      .duplicate()
      .withStaticVariable("distance", distance)
      .withStaticVariable("player_warp_display_details", config.rootSection.playerWarpsIntegration.displayNearestInIcon)
      .withStaticVariable("player_warp_name", nearestPlayerWarp == null ? null : nearestPlayerWarp.warpName())
      .withStaticVariable("player_warp_owner", nearestPlayerWarp == null ? null : nearestPlayerWarp.ownerName())
      .withStaticVariable("player_warp_world", nearestPlayerWarp == null ? null : Objects.requireNonNull(nearestPlayerWarp.location().getWorld()).getName())
      .withStaticVariable("player_warp_x", nearestPlayerWarp == null ? null : nearestPlayerWarp.location().getBlockX())
      .withStaticVariable("player_warp_y", nearestPlayerWarp == null ? null : nearestPlayerWarp.location().getBlockY())
      .withStaticVariable("player_warp_z", nearestPlayerWarp == null ? null : nearestPlayerWarp.location().getBlockZ())
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
      .withStaticVariable(
        "fees_absolute",
        fees.absoluteFees() == 0
          ? 0
          : shopManager.format(fees.absoluteFees(), cachedShop.handle)
      )
      .withStaticVariable("fees_relative", fees.relativeFees())
      .withStaticVariable(
        "fees_relative_value",
        fees.relativeFeesValue() == 0
          ? 0
          : shopManager.format(fees.relativeFeesValue(), cachedShop.handle)
      )
      .withStaticVariable(
        "fees_total_value",
        fees.relativeFeesValue() + fees.absoluteFees() == 0
          ? 0
          : shopManager.format(fees.relativeFeesValue() + fees.absoluteFees(), cachedShop.handle)
      )
      .withStaticVariable(
        "fees_final_price",
        shopManager.format(
          cachedShop.cachedType == ShopType.SELLING
            ? cachedShop.cachedPrice + fees.relativeFeesValue() + fees.absoluteFees()
            : cachedShop.cachedPrice - (fees.relativeFeesValue() + fees.absoluteFees()),
          cachedShop.handle
        )
      )
      .build(pageEnvironment);
  }

  private Inventory makeInventory() {
    return config.rootSection.resultDisplay.createInventory(pageEnvironment);
  }
}
