package me.blvckbytes.quick_shop_search.display;

import com.ghostchu.quickshop.api.shop.ShopType;
import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.quick_shop_search.*;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.cache.RemoteInteractionApi;
import me.blvckbytes.quick_shop_search.config.CooldownType;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.integration.player_warps.IPlayerWarpsIntegration;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ResultDisplayHandler implements Listener {

  private static final long MOVE_GHOST_ITEM_THRESHOLD_MS = 500;
  private static final double PLAYER_EYE_HEIGHT = 1.5;
  private static final String TELEPORT_COOLDOWN_KEY = "teleport-to-shop";

  private static final BlockFace[] SHOP_SIGN_FACES = new BlockFace[] {
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  private final PlatformScheduler scheduler;
  private final ConfigKeeper<MainSection> config;
  private final RemoteInteractionApi remoteInteractionApi;

  private final SelectionStateStore stateStore;
  private final UidScopedNamedStampStore stampStore;
  private final ChatPromptManager chatPromptManager;
  private final SlowTeleportManager slowTeleportManager;
  private final @Nullable IPlayerWarpsIntegration playerWarpsIntegration;
  private final Map<UUID, ResultDisplay> displayByPlayer;

  public ResultDisplayHandler(
    PlatformScheduler scheduler,
    RemoteInteractionApi remoteInteractionApi,
    ConfigKeeper<MainSection> config,
    SelectionStateStore stateStore,
    UidScopedNamedStampStore stampStore,
    ChatPromptManager chatPromptManager,
    SlowTeleportManager slowTeleportManager,
    @Nullable IPlayerWarpsIntegration playerWarpsIntegration
  ) {
    this.scheduler = scheduler;
    this.remoteInteractionApi = remoteInteractionApi;
    this.stateStore = stateStore;
    this.stampStore = stampStore;
    this.chatPromptManager = chatPromptManager;
    this.slowTeleportManager = slowTeleportManager;
    this.playerWarpsIntegration = playerWarpsIntegration;
    this.config = config;
    this.displayByPlayer = new HashMap<>();

    config.registerReloadListener(() -> {
      for (var display : displayByPlayer.values())
        display.onConfigReload(true);
    });
  }

  public void onShopUpdate(CachedShop shop, ShopUpdate update) {
    for (var display : displayByPlayer.values())
      display.onShopUpdate(shop, update);
  }

  public void show(Player player, DisplayData displayData) {
    displayByPlayer.put(player.getUniqueId(), new ResultDisplay(scheduler, config, player, displayData, stateStore.loadState(player)));
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player player))
      return;

    var playerId = player.getUniqueId();
    var display = displayByPlayer.get(playerId);

    // Only remove on inventory match, as to prevent removal on title update
    if (display != null && display.isInventory(event.getInventory())) {
      display.cleanup(false);
      displayByPlayer.remove(playerId);

      if (
        display.lastMoveToOwnInventoryStamp != 0 &&
        System.currentTimeMillis() - display.lastMoveToOwnInventoryStamp < MOVE_GHOST_ITEM_THRESHOLD_MS
      ) {
        scheduler.runNextTick(task -> player.updateInventory());
      }
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var display = displayByPlayer.remove(event.getPlayer().getUniqueId());

    if (display != null)
      display.cleanup(false);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player))
      return;

    var display = displayByPlayer.get(player.getUniqueId());

    if (display == null)
      return;

    event.setCancelled(true);

    if (
      event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY &&
      event.getClickedInventory() != player.getInventory()
    ) {
      display.lastMoveToOwnInventoryStamp = System.currentTimeMillis();
    }

    if (!display.isInventory(player.getOpenInventory().getTopInventory()))
      return;

    var slot = event.getRawSlot();

    if (slot < 0 || slot >= config.rootSection.resultDisplay.getRows() * 9)
      return;

    var targetShop = display.getShopCorrespondingToSlot(slot);
    var clickType = event.getClick();

    if (clickType == ClickType.LEFT) {
      if (config.rootSection.resultDisplay.items.previousPage.getDisplaySlots().contains(slot)) {
        display.previousPage();
        return;
      }

      if (config.rootSection.resultDisplay.items.nextPage.getDisplaySlots().contains(slot)) {
        display.nextPage();
        return;
      }

      if (config.rootSection.resultDisplay.items.sorting.getDisplaySlots().contains(slot)) {
        ensurePermission(player, PluginPermission.FEATURE_SORT, config.rootSection.playerMessages.missingPermissionFeatureSort, display::nextSortingSelection);
        return;
      }

      if (config.rootSection.resultDisplay.items.filtering.getDisplaySlots().contains(slot)) {
        ensurePermission(player, PluginPermission.FEATURE_FILTER, config.rootSection.playerMessages.missingPermissionFeatureFilter, display::nextFilteringCriterion);
        return;
      }

      if (targetShop != null) {
        var shopLocation = targetShop.handle.getLocation();

        if (shopLocation.getWorld() != player.getWorld()) {
          ensurePermission(
            player, PluginPermission.FEATURE_TELEPORT_OTHER_WORLD,
            config.rootSection.playerMessages.missingPermissionFeatureTeleportOtherWorld,
            () -> teleportPlayerToShop(player, display, targetShop)
          );
          return;
        }

        ensurePermission(
          player, PluginPermission.FEATURE_TELEPORT,
          config.rootSection.playerMessages.missingPermissionFeatureTeleport,
          () -> teleportPlayerToShop(player, display, targetShop)
        );
        return;
      }

      return;
    }

    if (clickType == ClickType.RIGHT) {
      if (config.rootSection.resultDisplay.items.previousPage.getDisplaySlots().contains(slot)) {
        display.firstPage();
        return;
      }

      if (config.rootSection.resultDisplay.items.nextPage.getDisplaySlots().contains(slot)) {
        display.lastPage();
        return;
      }

      if (config.rootSection.resultDisplay.items.sorting.getDisplaySlots().contains(slot)) {
        ensurePermission(player, PluginPermission.FEATURE_SORT, config.rootSection.playerMessages.missingPermissionFeatureSort, display::nextSortingOrder);
        return;
      }

      if (config.rootSection.resultDisplay.items.filtering.getDisplaySlots().contains(slot)) {
        ensurePermission(player, PluginPermission.FEATURE_FILTER, config.rootSection.playerMessages.missingPermissionFeatureFilter, display::nextFilteringState);
        return;
      }

      if (targetShop != null)
        targetShop.handle.openPreview(player);

      return;
    }

    if (clickType == ClickType.DROP) {
      if (config.rootSection.resultDisplay.items.sorting.getDisplaySlots().contains(slot))
        ensurePermission(player, PluginPermission.FEATURE_SORT, config.rootSection.playerMessages.missingPermissionFeatureSort, display::moveSortingSelectionDown);

      return;
    }

    if (clickType == ClickType.CONTROL_DROP) {
      if (config.rootSection.resultDisplay.items.sorting.getDisplaySlots().contains(slot)) {
        ensurePermission(player, PluginPermission.FEATURE_SORT, config.rootSection.playerMessages.missingPermissionFeatureSort, display::resetSortingState);
        return;
      }

      if (config.rootSection.resultDisplay.items.filtering.getDisplaySlots().contains(slot)) {
        ensurePermission(player, PluginPermission.FEATURE_FILTER, config.rootSection.playerMessages.missingPermissionFeatureFilter, display::resetFilteringState);
      }
    }

    if (clickType == ClickType.SHIFT_LEFT && targetShop != null) {
      var shopLocation = targetShop.handle.getLocation();

      if (shopLocation.getWorld() != player.getWorld()) {
        ensurePermission(
          player, PluginPermission.FEATURE_INTERACT_OTHER_WORLD,
          config.rootSection.playerMessages.missingPermissionFeatureInteractOtherWorld,
          () -> initiateShopInteraction(player, display, targetShop)
        );
        return;
      }

      ensurePermission(
        player, PluginPermission.FEATURE_INTERACT,
        config.rootSection.playerMessages.missingPermissionFeatureInteract,
        () -> initiateShopInteraction(player, display, targetShop)
      );
    }
  }

  private void initiateShopInteraction(Player player, ResultDisplay display, CachedShop cachedShop) {
    var maxUnitsResult = calculateMaxUnits(player, cachedShop);

    var distanceExtendedEnvironment = config.rootSection.getBaseEnvironment()
      .withStaticVariable("all_sentinel", config.rootSection.resultDisplay.chatPromptAllSentinel)
      .withStaticVariable("cancel_sentinel", config.rootSection.resultDisplay.chatPromptCancelSentinel)
      .build(display.getExtendedShopEnvironment(cachedShop));

    if (maxUnitsResult.units() == 0) {
      var message = switch (maxUnitsResult.limitingFactor()) {
        case PLAYER_SPACE -> config.rootSection.playerMessages.shopInteractPlayerHasNoSpace;
        case PLAYER_STOCK -> config.rootSection.playerMessages.shopInteractPlayerHasNoStock;
        case SHOP_SPACE -> config.rootSection.playerMessages.shopInteractShopHasNoSpace;
        case SHOP_STOCK -> config.rootSection.playerMessages.shopInteractShopHasNoStock;
        case SELLER_FUNDS -> config.rootSection.playerMessages.shopInteractSellerInsufficientFunds;
        case BUYER_FUNDS -> config.rootSection.playerMessages.shopInteractBuyerInsufficientFunds;
      };

      if (message != null)
        message.sendMessage(player, distanceExtendedEnvironment);

      return;
    }

    var limitingFactorPlaceholderMessage = switch (maxUnitsResult.limitingFactor()) {
      case PLAYER_SPACE -> config.rootSection.playerMessages.shopInteractPromptLimitingFactorPlayerSpace;
      case PLAYER_STOCK -> config.rootSection.playerMessages.shopInteractPromptLimitingFactorPlayerStock;
      case SHOP_SPACE -> config.rootSection.playerMessages.shopInteractPromptLimitingFactorShopSpace;
      case SHOP_STOCK -> config.rootSection.playerMessages.shopInteractPromptLimitingFactorShopStock;
      case SELLER_FUNDS -> config.rootSection.playerMessages.shopInteractPromptLimitingFactorSellerFunds;
      case BUYER_FUNDS -> config.rootSection.playerMessages.shopInteractPromptLimitingFactorBuyerFunds;
    };

    var limitingFactorExtendedEnvironment = config.rootSection.getBaseEnvironment()
      .withStaticVariable("limiting_factor", (
        limitingFactorPlaceholderMessage == null
          ? "?"
          : limitingFactorPlaceholderMessage.asScalar(ScalarType.STRING, distanceExtendedEnvironment)
      ))
      .withStaticVariable("max_units", maxUnitsResult.units())
      .build(distanceExtendedEnvironment);

    scheduler.runAtEntity(player, scheduleTask -> player.closeInventory());

    var shopFees = display.getShopFees(cachedShop);

    var didOverwritePrevious = chatPromptManager.register(
      player,
      input -> {
        BukkitEvaluable message;

        if (input.equalsIgnoreCase(config.rootSection.resultDisplay.chatPromptCancelSentinel)) {
          if ((message = config.rootSection.playerMessages.shopInteractPromptCancelCurrent) != null)
            message.sendMessage(player, limitingFactorExtendedEnvironment);

          return;
        }

        int amount;

        if (input.equalsIgnoreCase(config.rootSection.resultDisplay.chatPromptAllSentinel))
          amount = maxUnitsResult.units();

        else {
          amount = tryParseStrictlyPositiveInteger(input);

          if (amount <= 0) {
            if ((message = config.rootSection.playerMessages.shopInteractPromptInvalidInput) != null) {
              message.sendMessage(
                player,
                config.rootSection.getBaseEnvironment()
                  .withStaticVariable("input", input)
                  .build()
              );
            }
            return;
          }
        }

        if ((message = config.rootSection.playerMessages.shopInteractPromptDispatch) != null) {
          message.sendMessage(
            player,
            new EvaluationEnvironmentBuilder()
              .withStaticVariable("amount", amount)
              .build(limitingFactorExtendedEnvironment)
          );
        }

        scheduler.runAtLocation(cachedShop.handle.getLocation(), scheduleTask -> {
          if (shopFees.isNotZero()) {
            player.sendMessage("§cTODO: Implement fees " + shopFees);
          }
          remoteInteractionApi.interact(player, cachedShop.handle, amount);
        });
      },
      () -> {
        BukkitEvaluable message;

        if ((message = config.rootSection.playerMessages.shopInteractPromptTimeout) != null)
          message.sendMessage(player, limitingFactorExtendedEnvironment);
      }
    );

    BukkitEvaluable message;

    if (didOverwritePrevious) {
      if ((message = config.rootSection.playerMessages.shopInteractPromptCancelPrevious) != null)
        message.sendMessage(player, config.rootSection.builtBaseEnvironment);
    }

    if (cachedShop.handle.getShopType() == ShopType.BUYING) {
      if ((message = config.rootSection.playerMessages.shopInteractPromptBuying) != null)
        message.sendMessage(player, limitingFactorExtendedEnvironment);
    }
    else if ((message = config.rootSection.playerMessages.shopInteractPromptSelling) != null)
      message.sendMessage(player, limitingFactorExtendedEnvironment);

    if (shopFees.isNotZero()) {
      if ((message = config.rootSection.playerMessages.shopInteractPromptFeesWarning) != null)
        message.sendMessage(player, limitingFactorExtendedEnvironment);
    }
  }

  private int tryParseStrictlyPositiveInteger(String input) {
    try {
      var result = Integer.parseInt(input);

      if (result > 0)
        return result;
    } catch (NumberFormatException ignored) {}

    return -1;
  }

  private String formatDuration(long milliseconds) {
    long seconds = (milliseconds + 999) / 1000;
    long durationSeconds = seconds % 60;
    long durationMinutes = seconds % 3600 / 60;
    long durationHours = seconds % 86400 / 3600;
    long durationDays = seconds / 86400;

    return config.rootSection.cooldowns.cooldownFormat.asScalar(
      ScalarType.STRING,
      config.rootSection.getBaseEnvironment()
        .withStaticVariable("days", durationDays)
        .withStaticVariable("hours", durationHours)
        .withStaticVariable("minutes", durationMinutes)
        .withStaticVariable("seconds", durationSeconds)
        .build()
    );
  }

  private boolean checkAndNotifyOfCooldown(
    Player player,
    TeleportCooldownType cooldownType
  ) {
    if (cooldownType.bypassPermission().has(player))
      return false;

    var currentStamp = System.currentTimeMillis();
    var lastTeleportStamp = stampStore.read(player.getUniqueId(), cooldownType.stampKey());

    if (lastTeleportStamp > 0) {
      long elapsedDuration = currentStamp - lastTeleportStamp;
      if (elapsedDuration < cooldownType.durationMillis()) {

        if (cooldownType.cooldownMessage() != null) {
          cooldownType.cooldownMessage().sendMessage(
            player,
            config.rootSection.getBaseEnvironment()
              .withStaticVariable("duration", formatDuration(cooldownType.durationMillis() - elapsedDuration))
              .build()
          );
        }

        return true;
      }
    }

    return false;
  }

  private void teleportPlayerToShop(Player player, ResultDisplay display, CachedShop cachedShop) {
    BukkitEvaluable message;

    var shop = cachedShop.handle;
    var shopId = cachedShop.handle.getShopId();
    var shopLocation = shop.getLocation().clone(); // Not cloning can mess shops up (direct reference)!

    var applicableCooldowns = new ArrayList<TeleportCooldownType>();
    TeleportCooldownType cooldownType;

    if (shopLocation.getWorld() != player.getWorld()) {
      cooldownType = new TeleportCooldownType(
        PluginPermission.FEATURE_TELEPORT_OTHER_WORLD_BYPASS_COOLDOWN_SAME_SHOP,
        TELEPORT_COOLDOWN_KEY + "-other-world-shopId-" + shopId,
        config.rootSection.cooldowns.teleportToShop.getCooldownMillis(player, CooldownType.OTHER_WORLD_SAME_SHOP),
        config.rootSection.playerMessages.pendingCooldownFeatureTeleportOtherWorldSameShop
      );

      if (checkAndNotifyOfCooldown(player, cooldownType))
        return;

      applicableCooldowns.add(cooldownType);

      cooldownType = new TeleportCooldownType(
        PluginPermission.FEATURE_TELEPORT_OTHER_WORLD_BYPASS_COOLDOWN_ANY_SHOP,
        TELEPORT_COOLDOWN_KEY + "-other-world",
        config.rootSection.cooldowns.teleportToShop.getCooldownMillis(player, CooldownType.OTHER_WORLD_ANY_SHOP),
        config.rootSection.playerMessages.pendingCooldownFeatureTeleportOtherWorldAnyShop
      );

      if (checkAndNotifyOfCooldown(player, cooldownType))
        return;

      applicableCooldowns.add(cooldownType);
    }

    cooldownType = new TeleportCooldownType(
      PluginPermission.FEATURE_TELEPORT_BYPASS_COOLDOWN_SAME_SHOP,
      TELEPORT_COOLDOWN_KEY + "-shopId-" + shopId,
      config.rootSection.cooldowns.teleportToShop.getCooldownMillis(player, CooldownType.SAME_SHOP),
      config.rootSection.playerMessages.pendingCooldownFeatureTeleportSameShop
    );

    if (checkAndNotifyOfCooldown(player, cooldownType))
      return;

    applicableCooldowns.add(cooldownType);

    cooldownType = new TeleportCooldownType(
      PluginPermission.FEATURE_TELEPORT_BYPASS_COOLDOWN_ANY_SHOP,
      TELEPORT_COOLDOWN_KEY,
      config.rootSection.cooldowns.teleportToShop.getCooldownMillis(player, CooldownType.ANY_SHOP),
      config.rootSection.playerMessages.pendingCooldownFeatureTeleportAnyShop
    );

    if (checkAndNotifyOfCooldown(player, cooldownType))
      return;

    applicableCooldowns.add(cooldownType);

    var shopBlock = shopLocation.getBlock();

    Location targetLocation = null;

    // Try to teleport at the block-face where the sign's mounted, looking directly at the center of the container

    for (var signFace : SHOP_SIGN_FACES) {
      var currentBlock = shopBlock.getRelative(signFace);
      if (!(currentBlock.getState() instanceof Sign sign))
        continue;

      if (!shop.isShopSign(sign))
        continue;

      var currentLocationXZCenter = currentBlock.getLocation().add(.5, 0, .5);
      var newEyeLocation = currentLocationXZCenter.toVector().add(new Vector(0, PLAYER_EYE_HEIGHT, 0));
      var shopXYZCenter = shopLocation.add(.5, .5, .5);

      targetLocation = currentLocationXZCenter.setDirection(shopXYZCenter.toVector().subtract(newEyeLocation));
      break;
    }

    // This fallback should never be reached, but better safe than sorry.
    if (targetLocation == null)
      targetLocation = shopLocation.add(.5, 0, .5);

    IEvaluationEnvironment playerWarpExtendedEnvironment = null;

    if (playerWarpsIntegration != null && PluginPermission.FEATURE_TELEPORT_CLOSEST_PLAYER_WARP.has(player)) {
      var result = playerWarpsIntegration.locateNearestWithinRange(player, targetLocation, config.rootSection.playerWarpsIntegration.nearestWarpBlockRadius);

      if (result != null) {
        playerWarpExtendedEnvironment =
          config.rootSection.getBaseEnvironment()
            .withStaticVariable("player_warp_owner", result.ownerName())
            .withStaticVariable("player_warp_name", result.warpName())
            .withStaticVariable("player_warp_world", Objects.requireNonNull(result.location().getWorld()).getName())
            .withStaticVariable("player_warp_x", result.location().getBlockX())
            .withStaticVariable("player_warp_y", result.location().getBlockY())
            .withStaticVariable("player_warp_z", result.location().getBlockZ())
            .build(display.getExtendedShopEnvironment(cachedShop));

        if (result.isBanned() && !PluginPermission.FEATURE_TELEPORT_CLOSEST_PLAYER_WARP_BAN_BYPASS.has(player)) {
          if ((message = config.rootSection.playerMessages.nearestPlayerWarpBanned) != null)
            message.sendMessage(player, playerWarpExtendedEnvironment);

          playerWarpExtendedEnvironment = null;
        }

        else
          targetLocation = result.location();
      }
    }

    if (playerWarpExtendedEnvironment != null) {
      if ((message = config.rootSection.playerMessages.beforeTeleportingNearestPlayerWarp) != null)
        message.sendMessage(player, playerWarpExtendedEnvironment);
    }

    else if ((message = config.rootSection.playerMessages.beforeTeleporting) != null) {
      message.sendMessage(
        player,
        config.rootSection.getBaseEnvironment().build(display.getExtendedShopEnvironment(cachedShop))
      );
    }

    scheduler.runAtEntity(player, scheduleTask -> player.closeInventory());

    slowTeleportManager.initializeTeleportation(player, targetLocation, () -> {
      for (var applicableCooldown : applicableCooldowns)
        stampStore.write(player.getUniqueId(), applicableCooldown.stampKey(), System.currentTimeMillis());
    });
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player player))
      return;

    if (displayByPlayer.containsKey(player.getUniqueId()))
      event.setCancelled(true);
  }

  public void onShutdown() {
    for (var displayIterator = displayByPlayer.entrySet().iterator(); displayIterator.hasNext();) {
      displayIterator.next().getValue().cleanup(true);
      displayIterator.remove();
    }
  }

  private void ensurePermission(Player player, PluginPermission permission, @Nullable BukkitEvaluable missingMessage, Runnable handler) {
    if (permission.has(player)) {
      handler.run();
      return;
    }

    if (missingMessage != null)
      missingMessage.sendMessage(player, config.rootSection.builtBaseEnvironment);
  }

  private MaxUnitsResult calculateMaxUnits(Player player, CachedShop cachedShop) {
    var isShopBuying = cachedShop.handle.isBuying();
    var maxUnitsByPlayerInventory = countInventorySpaceOrStockInUnits(player, cachedShop, !isShopBuying);

    if (maxUnitsByPlayerInventory == 0)
      return new MaxUnitsResult(0, isShopBuying ? LimitingFactor.PLAYER_STOCK : LimitingFactor.PLAYER_SPACE);

    int maxUnitsByShopInventory;

    // Stock/Space-values, as received by QuickShop-Hikari, are already in units,
    // not items - thus, there's no need to divide by the item's amount.
    if (cachedShop.handle.isSelling())
      maxUnitsByShopInventory = cachedShop.cachedStock;
    else
      maxUnitsByShopInventory = cachedShop.cachedSpace;

    if (maxUnitsByShopInventory == 0)
      return new MaxUnitsResult(0, isShopBuying ? LimitingFactor.SHOP_SPACE : LimitingFactor.SHOP_STOCK);

    var shopPrice = cachedShop.handle.getPrice();

    int maxUnitsByBalance;

    if (cachedShop.handle.isSelling()) {
      var playerBalance = remoteInteractionApi.getPlayerBalance(player, cachedShop.handle);
      maxUnitsByBalance = (int) (playerBalance / shopPrice);
    } else {
      var sellerBalance = remoteInteractionApi.getOwnerBalance(cachedShop.handle);
      maxUnitsByBalance = (int) (sellerBalance / shopPrice);
    }

    if (maxUnitsByBalance == 0)
      return new MaxUnitsResult(0, isShopBuying ? LimitingFactor.SELLER_FUNDS : LimitingFactor.BUYER_FUNDS);

    int maxUnitsTotal = Math.min(maxUnitsByPlayerInventory, Math.min(maxUnitsByShopInventory, maxUnitsByBalance));

    LimitingFactor limitingFactor;

    if (maxUnitsTotal == maxUnitsByPlayerInventory)
      limitingFactor = isShopBuying ? LimitingFactor.PLAYER_STOCK : LimitingFactor.PLAYER_SPACE;
    else if (maxUnitsTotal == maxUnitsByShopInventory)
      limitingFactor = isShopBuying ? LimitingFactor.SHOP_SPACE : LimitingFactor.SHOP_STOCK;
    else
      limitingFactor = isShopBuying ? LimitingFactor.SELLER_FUNDS : LimitingFactor.BUYER_FUNDS;

    return new MaxUnitsResult(maxUnitsTotal, limitingFactor);
  }

  private int countInventorySpaceOrStockInUnits(Player player, CachedShop shop, boolean countSpace) {
    var inventory = player.getInventory();
    var shopItem = shop.handle.getItem();

    var shopItemStackSize = shopItem.getAmount();
    var shopItemMaxStackSize = shopItem.getMaxStackSize();

    var result = 0;

    for (var currentItem : inventory.getStorageContents()) {
      if (currentItem == null || currentItem.getType().isAir()) {

        if (countSpace)
          result += shopItemMaxStackSize;

        continue;
      }

      if (!shop.handle.matches(currentItem))
        continue;

      if (countSpace) {
        result += shopItemMaxStackSize - currentItem.getAmount();
        continue;
      }

      result += currentItem.getAmount();
    }

    return result / shopItemStackSize;
  }
}
