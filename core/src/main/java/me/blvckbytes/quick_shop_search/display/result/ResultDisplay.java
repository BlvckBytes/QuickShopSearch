package me.blvckbytes.quick_shop_search.display.result;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.shop.ShopType;
import com.tcoded.folialib.impl.PlatformScheduler;
import it.unimi.dsi.fastutil.longs.Long2LongAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.blvckbytes.item_predicate_parser.predicate.stringify.PlainStringifier;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.cache.ShopUpdate;
import me.blvckbytes.quick_shop_search.command.SearchFlag;
import me.blvckbytes.quick_shop_search.compatibility.RemoteInteractionApi;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.config.fees.FeesDistanceRangeSection;
import me.blvckbytes.quick_shop_search.config.fees.FeesDistanceRangesSection;
import me.blvckbytes.quick_shop_search.config.fees.FeesValuesSection;
import me.blvckbytes.quick_shop_search.display.Display;
import me.blvckbytes.quick_shop_search.integration.IntegrationRegistry;
import me.blvckbytes.quick_shop_search.integration.essentials_warps.EssentialsWarpData;
import me.blvckbytes.quick_shop_search.integration.essentials_warps.IEssentialsWarpsIntegration;
import me.blvckbytes.quick_shop_search.integration.player_warps.IPlayerWarpsIntegration;
import me.blvckbytes.quick_shop_search.integration.player_warps.PlayerWarpData;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ResultDisplay extends Display<ResultDisplayData> implements DynamicPropertyProvider {

  private final IntegrationRegistry integrationRegistry;
  private final RemoteInteractionApi remoteInteractionApi;
  private final TexturesResolver texturesResolver;
  private final AsyncTaskQueue asyncQueue;

  private final Long2LongMap shopDistanceByShopId;
  private final Long2ObjectMap<CalculatedFees> shopFeesByShopId;
  private final Long2ObjectMap<@Nullable PlayerWarpData> nearestPlayerWarpByShopId;
  private final Long2ObjectMap<@Nullable EssentialsWarpData> nearestEssentialsWarpByShopId;
  private List<CachedShop> filteredUnSortedShops;
  private List<CachedShop> filteredSortedShops;

  private final Player player;
  private final Location playerLocation;
  private final CachedShop[] slotMap;
  private int numberOfPages;
  public final SelectionState selectionState;

  private InterpretationEnvironment pageEnvironment;
  private InterpretationEnvironment sortingEnvironment;
  private InterpretationEnvironment filteringEnvironment;
  private InterpretationEnvironment activeSearchEnvironment;
  private boolean hasAnyActiveSearchProperties;

  private int currentPage = 1;

  public ResultDisplay(
    PlatformScheduler scheduler,
    IntegrationRegistry integrationRegistry,
    ConfigKeeper<MainSection> config,
    RemoteInteractionApi remoteInteractionApi,
    TexturesResolver texturesResolver,
    Player player,
    ResultDisplayData displayData,
    SelectionState selectionState
  ) {
    super(player, displayData, config, scheduler);

    this.integrationRegistry = integrationRegistry;
    this.asyncQueue = new AsyncTaskQueue(scheduler);
    this.player = player;
    this.remoteInteractionApi = remoteInteractionApi;
    this.texturesResolver = texturesResolver;
    this.playerLocation = player.getLocation();
    this.shopDistanceByShopId = new Long2LongAVLTreeMap();
    this.shopFeesByShopId = new Long2ObjectAVLTreeMap<>();
    this.nearestPlayerWarpByShopId = new Long2ObjectAVLTreeMap<>();
    this.nearestEssentialsWarpByShopId = new Long2ObjectAVLTreeMap<>();
    this.slotMap = new CachedShop[9 * 6];
    this.selectionState = selectionState;

    setupEnvironments();

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

  private void setupEnvironments() {
   pageEnvironment = config.rootSection.resultDisplay.inventoryEnvironment.copy()
      .withVariable("current_page", this.currentPage)
      .withVariable("number_of_pages", this.numberOfPages);

    sortingEnvironment = pageEnvironment.copy();
    selectionState.extendSortingEnvironment(sortingEnvironment, player);

    filteringEnvironment = pageEnvironment.copy();
    selectionState.extendFilteringEnvironment(filteringEnvironment, player);

    var activeSearchProperties = new HashMap<String, Object>();

    activeSearchProperties.put(
      "predicate",
      displayData.query() == null
        ? null
        : PlainStringifier.stringify(displayData.query(), true)
    );

    activeSearchProperties.put("owner", displayData.searchFlagsContainer().getWithMapper(SearchFlag.OWNER, OfflinePlayer::getName));
    activeSearchProperties.put("radius", displayData.searchFlagsContainer().get(SearchFlag.RADIUS));
    activeSearchProperties.put("price", displayData.searchFlagsContainer().get(SearchFlag.PRICE));
    activeSearchProperties.put("max_price", displayData.searchFlagsContainer().get(SearchFlag.MAX_PRICE));
    activeSearchProperties.put("min_price", displayData.searchFlagsContainer().get(SearchFlag.MIN_PRICE));

    activeSearchEnvironment = new InterpretationEnvironment();

    this.hasAnyActiveSearchProperties = false;

    for (var propertyEntry : activeSearchProperties.entrySet()) {
      var currentValue = propertyEntry.getValue();

      if (currentValue != null)
        this.hasAnyActiveSearchProperties = true;

      activeSearchEnvironment.withVariable(propertyEntry.getKey(), currentValue);
    }
  }

  @Override
  public void onConfigReload() {
    setupEnvironments();
    applyFiltering();
    applySorting();
    show();
  }

  public @Nullable CachedShop getShopCorrespondingToSlot(int slot) {
    return slotMap[slot];
  }

  @Override
  public void onInventoryClose() {
    clearSlotMap();
    super.onInventoryClose();
  }

  @Override
  public void onShutdown() {
    clearSlotMap();
    super.onShutdown();
  }

  public void clearSlotMap() {
    for (var i = 0; i < slotMap.length; ++i)
      this.slotMap[i] = null;
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

  @Override
  public void show() {
    clearSlotMap();
    super.show();
  }

  public CalculatedFees getShopFees(CachedShop cachedShop) {
    var shopId = cachedShop.handle.getShopId();

    CalculatedFees shopFees = shopFeesByShopId.get(shopId);

    if (shopFees == null) {
      var feesValues = decideFees(player, cachedShop);
      shopFees = CalculatedFees.calculateFor(feesValues, cachedShop);
      shopFeesByShopId.put(shopId, shopFees);
    }

    return shopFees;
  }

  private @Nullable FeesValuesSection decideFees(Player player, CachedShop shop) {
    var playerLocation = player.getLocation();
    var playerWorld = playerLocation.getWorld();

    if (playerWorld == null)
      return null;

    var shopLocation = shop.handle.getLocation();
    var shopWorld = shopLocation.getWorld();

    if (shopWorld == null)
      return null;

    var shopWorldName = shopWorld.getName();

    if (!playerWorld.equals(shopWorld)) {
      if (PluginPermission.FEATURE_FEES_BYPASS_OTHER_WORLD.has(player))
        return null;

      var worldSpecificSection = config.rootSection.fees.otherWorld.worlds.get(shopWorldName);

      if (worldSpecificSection == null)
        return config.rootSection.fees.otherWorld.worldsFallback;

      return worldSpecificSection;
    }

    if (PluginPermission.FEATURE_FEES_BYPASS.has(player))
      return null;

    FeesDistanceRangesSection feesSection;

    if ((feesSection = config.rootSection.fees.worlds.get(shopWorldName)) == null)
      feesSection = config.rootSection.fees.worldsFallback;

    var distance = (int) Math.ceil(shopLocation.distance(playerLocation));

    FeesDistanceRangeSection rangeSection = null;

    for (var distanceRange : feesSection.distanceRanges) {
      if (distance >= distanceRange.minDistance && distance <= distanceRange.maxDistance) {
        rangeSection = distanceRange;
        break;
      }
    }

    if (rangeSection == null)
      rangeSection = feesSection.distanceRangesFallback;

    FeesValuesSection highestPriorityFees = null;

    for (var permissionNameEntry : rangeSection.permissionNames.entrySet()) {
      var permissionName = permissionNameEntry.getKey();
      var permissionNode = PluginPermission.FEATURE_FEES_PERMISSION_NAME_BASE.nodeWithSuffix(permissionName);

      if (!player.hasPermission(permissionNode))
        continue;

      var currentFees = permissionNameEntry.getValue();

      if (highestPriorityFees == null || highestPriorityFees.priority < currentFees.priority)
        highestPriorityFees = currentFees;
    }

    if (highestPriorityFees != null)
      return highestPriorityFees;

    return rangeSection.permissionNamesFallback;
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
    return remoteInteractionApi.getPlayerBalance(player, cachedShop.handle);
  }

  @Override
  public double getShopOwnerBalanceForShopCurrency(CachedShop cachedShop) {
    return remoteInteractionApi.getOwnerBalance(cachedShop.handle);
  }

  private void renderSortingItem() {
    config.rootSection.resultDisplay.items.sorting.renderInto(inventory, sortingEnvironment);
  }

  private void renderFilteringItem() {
    config.rootSection.resultDisplay.items.filtering.renderInto(inventory, filteringEnvironment);
  }

  @Override
  protected void renderItems() {
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
      var representative = cachedShop.handle.getItem().clone();

      config.rootSection.resultDisplay.items.representativePatch.patch(representative, cachedShop.makeShopEnvironment());

      inventory.setItem(slot, representative);

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

  public @Nullable EssentialsWarpData getNearestEssentialsWarp(CachedShop cachedShop) {
    var essentialsWarpsIntegration = integrationRegistry.getEssentialsWarpsIntegration();

    if (essentialsWarpsIntegration == null)
      return null;

    var shopId = cachedShop.handle.getShopId();
    var result = nearestEssentialsWarpByShopId.get(shopId);

    if (result == null) {
      result = essentialsWarpsIntegration.locateNearestWithinRange(player, cachedShop.handle.getLocation(), config.rootSection.essentialsWarpsIntegration.nearestWarpBlockRadius);

      if (result == null)
        result = IEssentialsWarpsIntegration.ESSENTIALS_WARP_NULL_SENTINEL;

      nearestEssentialsWarpByShopId.put(shopId, result);
    }

    if (result == IEssentialsWarpsIntegration.ESSENTIALS_WARP_NULL_SENTINEL)
      return null;

    return result;
  }

  public @Nullable PlayerWarpData getNearestPlayerWarp(CachedShop cachedShop) {
    var playerWarpsIntegration = integrationRegistry.getPlayerWarpsIntegration();

    if (playerWarpsIntegration == null)
      return null;

    var shopId = cachedShop.handle.getShopId();
    var result = nearestPlayerWarpByShopId.get(shopId);

    if (result == null) {
      result = playerWarpsIntegration.locateNearestWithinRange(player, cachedShop.handle.getLocation(), config.rootSection.playerWarpsIntegration.nearestWarpBlockRadius);

      if (result == null)
        result = IPlayerWarpsIntegration.PLAYER_WARP_NULL_SENTINEL;

      nearestPlayerWarpByShopId.put(shopId, result);
    }

    if (result == IPlayerWarpsIntegration.PLAYER_WARP_NULL_SENTINEL)
      return null;

    return result;
  }

  private boolean canPlayerTeleport(
    CachedShop cachedShop,
    @Nullable PlayerWarpData nearestPlayerWarp,
    @Nullable EssentialsWarpData nearestEssentialsWarp
  ) {
    var isOtherWorld = cachedShop.handle.getLocation().getWorld() != player.getWorld();

    if (isOtherWorld && !PluginPermission.FEATURE_TELEPORT_OTHER_WORLD.has(player))
      return false;

    if (PluginPermission.FEATURE_TELEPORT_SHOP.has(player))
      return true;

    if (PluginPermission.FEATURE_TELEPORT_PLAYER_WARP.has(player) && nearestPlayerWarp != null)
      return true;

    return PluginPermission.FEATURE_TELEPORT_ESSENTIALS_WARP.has(player) && nearestEssentialsWarp != null;
  }

  public InterpretationEnvironment getExtendedShopEnvironment(CachedShop cachedShop) {
    var distance = getShopDistance(cachedShop);
    var isOtherWorld = distance < 0;
    var fees = getShopFees(cachedShop);
    var nearestPlayerWarp = getNearestPlayerWarp(cachedShop);
    var nearestEssentialsWarp = getNearestEssentialsWarp(cachedShop);

    var shopManager = QuickShopAPI.getInstance().getShopManager();

    var environment = cachedShop.makeShopEnvironment()
      .inheritFrom(pageEnvironment, false)
      .withVariable("distance", distance)
      .withVariable(
        "can_teleport",
        canPlayerTeleport(cachedShop, nearestPlayerWarp, nearestEssentialsWarp)
      )
      .withVariable(
        "can_interact",
        isOtherWorld
          ? PluginPermission.FEATURE_INTERACT_OTHER_WORLD.has(player)
          : PluginPermission.FEATURE_INTERACT.has(player)
      )
      .withVariable(
        "fees_absolute",
        fees.absoluteFees() == 0
          ? 0
          : shopManager.format(fees.absoluteFees(), cachedShop.handle)
      )
      .withVariable("fees_relative", fees.relativeFees())
      .withVariable(
        "fees_relative_value",
        fees.relativeFeesValue() == 0
          ? 0
          : shopManager.format(fees.relativeFeesValue(), cachedShop.handle)
      )
      .withVariable(
        "fees_total_value",
        fees.relativeFeesValue() + fees.absoluteFees() == 0
          ? 0
          : shopManager.format(fees.relativeFeesValue() + fees.absoluteFees(), cachedShop.handle)
      )
      .withVariable(
        "fees_final_price",
        shopManager.format(
          cachedShop.cachedType == ShopType.SELLING
            ? cachedShop.cachedPrice + fees.relativeFeesValue() + fees.absoluteFees()
            : cachedShop.cachedPrice - (fees.relativeFeesValue() + fees.absoluteFees()),
          cachedShop.handle
        )
      );

    if (nearestPlayerWarp != null) {
      var warpDistance = -1;

      if (player.getWorld().equals(nearestPlayerWarp.location().getWorld()))
        warpDistance = (int) Math.round(nearestPlayerWarp.location().distance(playerLocation));

      var ownerName = nearestPlayerWarp.ownerName();

      environment
        .withVariable("player_warp_display_details", config.rootSection.playerWarpsIntegration.displayNearestInIcon)
        .withVariable("player_warp_name", nearestPlayerWarp.warpName())
        .withVariable("player_warp_owner", ownerName)
        .withVariable("player_warp_world", Objects.requireNonNull(nearestPlayerWarp.location().getWorld()).getName())
        .withVariable("player_warp_x", nearestPlayerWarp.location().getBlockX())
        .withVariable("player_warp_y", nearestPlayerWarp.location().getBlockY())
        .withVariable("player_warp_z", nearestPlayerWarp.location().getBlockZ())
        .withVariable("player_warp_distance", warpDistance)
        .withVariable("player_warp_owner_textures", texturesResolver.tryResolveTextures(ownerName));
    }

    if (nearestEssentialsWarp != null) {
      var warpDistance = -1;

      if (player.getWorld().equals(nearestEssentialsWarp.location().getWorld()))
        warpDistance = (int) Math.round(nearestEssentialsWarp.location().distance(playerLocation));

      environment
        .withVariable("essentials_warp_display_details", config.rootSection.essentialsWarpsIntegration.displayNearestInIcon)
        .withVariable("essentials_warp_name", nearestEssentialsWarp.name())
        .withVariable("essentials_warp_world", Objects.requireNonNull(nearestEssentialsWarp.location().getWorld()).getName())
        .withVariable("essentials_warp_x", nearestEssentialsWarp.location().getBlockX())
        .withVariable("essentials_warp_y", nearestEssentialsWarp.location().getBlockY())
        .withVariable("essentials_warp_z", nearestEssentialsWarp.location().getBlockZ())
        .withVariable("essentials_warp_distance", warpDistance);
    }

    return environment;
  }

  @Override
  protected Inventory makeInventory() {
    return config.rootSection.resultDisplay.createInventory(pageEnvironment);
  }
}
