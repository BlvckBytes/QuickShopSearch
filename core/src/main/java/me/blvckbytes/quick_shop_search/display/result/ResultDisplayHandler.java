package me.blvckbytes.quick_shop_search.display.result;

import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.shop.ShopType;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.*;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.compatibility.RemoteInteractionApi;
import me.blvckbytes.quick_shop_search.cache.ShopUpdate;
import me.blvckbytes.quick_shop_search.config.cooldowns.CooldownType;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.display.DisplayHandler;
import me.blvckbytes.quick_shop_search.display.teleport.TeleportDisplayData;
import me.blvckbytes.quick_shop_search.display.teleport.TeleportDisplayHandler;
import me.blvckbytes.quick_shop_search.integration.IntegrationRegistry;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

public class ResultDisplayHandler extends DisplayHandler<ResultDisplay, ResultDisplayData> {

  private static final double PLAYER_EYE_HEIGHT = 1.5;
  private static final String TELEPORT_COOLDOWN_KEY = "teleport-to-shop";

  private static final BlockFace[] SHOP_SIGN_FACES = new BlockFace[] {
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  private record FeesPayBackTask(
    WrappedTask task,
    int amount,
    long shopId
  ) {}

  private final Logger logger;
  private final RemoteInteractionApi remoteInteractionApi;

  private final SelectionStateStore stateStore;
  private final UidScopedNamedStampStore stampStore;
  private final ChatPromptManager chatPromptManager;
  private final TeleportDisplayHandler teleportDisplayHandler;
  private final IntegrationRegistry integrationRegistry;
  private final Map<UUID, FeesPayBackTask> feesPayBackTaskByPlayerId;

  public ResultDisplayHandler(
    Logger logger,
    PlatformScheduler scheduler,
    RemoteInteractionApi remoteInteractionApi,
    ConfigKeeper<MainSection> config,
    SelectionStateStore stateStore,
    UidScopedNamedStampStore stampStore,
    ChatPromptManager chatPromptManager,
    TeleportDisplayHandler teleportDisplayHandler,
    IntegrationRegistry integrationRegistry
  ) {
    super(config, scheduler);

    this.logger = logger;
    this.remoteInteractionApi = remoteInteractionApi;
    this.stateStore = stateStore;
    this.stampStore = stampStore;
    this.chatPromptManager = chatPromptManager;
    this.teleportDisplayHandler = teleportDisplayHandler;
    this.integrationRegistry = integrationRegistry;
    this.feesPayBackTaskByPlayerId = new HashMap<>();
  }

  public void onPurchaseSuccess(CachedShop shop, int amount, UUID purchaserId) {
    var payBackTask = feesPayBackTaskByPlayerId.get(purchaserId);

    if (payBackTask == null)
      return;

    var shopId = shop.handle.getShopId();

    if (payBackTask.amount != amount) {
      logger.warning("Fees payback-task amount-mismatch: " + payBackTask.amount + " =/= " + amount + " for shopId " + shop.handle.getShopId() + " and purchaserId " + purchaserId);
      return;
    }

    if (payBackTask.shopId != shopId) {
      logger.warning("Fees payback-task shopId-mismatch: " + payBackTask.shopId + " =/= " + shopId + " for amount " + amount + " and purchaserId " + purchaserId);
      return;
    }

    payBackTask.task.cancel();
    feesPayBackTaskByPlayerId.remove(purchaserId);
  }

  public void onShopUpdate(CachedShop shop, ShopUpdate update) {
    forEachDisplay(display -> display.onShopUpdate(shop, update));
  }

  @Override
  public ResultDisplay instantiateDisplay(Player player, ResultDisplayData displayData) {
    return new ResultDisplay(scheduler, integrationRegistry, config, remoteInteractionApi, player, displayData, stateStore.loadState(player));
  }

  @Override
  protected void handleClick(Player player, ResultDisplay display, ClickType clickType, int slot) {
    var targetShop = display.getShopCorrespondingToSlot(slot);

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
        var result = callTeleportDisplayIfAnyAvailable(player, display, targetShop);

        if (result != null && !result) {
          var shopLocation = targetShop.handle.getLocation();
          var message = config.rootSection.playerMessages.missingPermissionFeatureTeleport;

          if (shopLocation.getWorld() != player.getWorld())
            message = config.rootSection.playerMessages.missingPermissionFeatureTeleportOtherWorld;

          if (message != null)
            message.sendMessage(player, config.rootSection.builtBaseEnvironment);

          return;
        }
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

  private void feesPaybackHandler(Player player, CachedShop cachedShop, double feesAmount, String feesAmountFormatted) {
    var playerId = player.getUniqueId();

    feesPayBackTaskByPlayerId.remove(playerId);

    BukkitEvaluable message;

    if (!remoteInteractionApi.depositAmount(player, cachedShop.handle, feesAmount)) {
      logger.warning("Could not deposit fees payback amount of " + feesAmountFormatted + " to playerId " + playerId + " for shopId" + cachedShop.handle.getShopId());

      if ((message = config.rootSection.playerMessages.shopInteractCouldNotPayBackFees) != null) {
        message.sendMessage(
          player,
          config.rootSection.getBaseEnvironment()
            .withStaticVariable("fees_amount", feesAmountFormatted)
            .build()
        );
      }

      return;
    }

    if ((message = config.rootSection.playerMessages.shopInteractPayedBackFees) != null) {
      message.sendMessage(
        player,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("fees_amount", feesAmountFormatted)
          .build()
      );
    }
  }

  private void dispatchShopInteraction(Player player, CachedShop cachedShop, CalculatedFees shopFees, int amount) {
    scheduler.runAtLocation(cachedShop.handle.getLocation(), scheduleTask -> {
      if (!shopFees.isNotZero()) {
        remoteInteractionApi.interact(player, cachedShop.handle, amount);
        return;
      }

      BukkitEvaluable message;

      synchronized (feesPayBackTaskByPlayerId) {
        var playerId = player.getUniqueId();

        if (feesPayBackTaskByPlayerId.containsKey(playerId)) {
          if ((message = config.rootSection.playerMessages.shopInteractPendingFeesTask) != null)
            message.sendMessage(player, config.rootSection.builtBaseEnvironment);

          return;
        }

        var feesAmount = (shopFees.relativeFeesValue() + shopFees.absoluteFees()) * amount;
        var feesAmountFormatted = QuickShopAPI.getInstance().getShopManager().format(feesAmount, cachedShop.handle);

        if (!remoteInteractionApi.withdrawAmount(player, cachedShop.handle, feesAmount)) {
          if ((message = config.rootSection.playerMessages.shopInteractCouldNotWithdrawFees) != null) {
            message.sendMessage(
              player,
              config.rootSection.getBaseEnvironment()
                .withStaticVariable("fees_amount", feesAmountFormatted)
                .build()
            );
          }

          return;
        }

        if ((message = config.rootSection.playerMessages.shopInteractWithdrawnFees) != null) {
          message.sendMessage(
            player,
            config.rootSection.getBaseEnvironment()
              .withStaticVariable("fees_amount", feesAmountFormatted)
              .build()
          );
        }

        var payBackTask = new FeesPayBackTask(
          scheduler.runLater(
            () -> feesPaybackHandler(player, cachedShop, feesAmount, feesAmountFormatted),
            config.rootSection.fees.feesPayBackTimeoutTicks
          ),
          amount,
          cachedShop.handle.getShopId()
        );

        feesPayBackTaskByPlayerId.put(playerId, payBackTask);
        remoteInteractionApi.interact(player, cachedShop.handle, amount);
      }
    });
  }

  private void initiateShopInteraction(Player player, ResultDisplay display, CachedShop cachedShop) {
    var shopFees = display.getShopFees(cachedShop);
    var maxUnitsResult = calculateMaxUnits(player, cachedShop, shopFees);

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

        dispatchShopInteraction(player, cachedShop, shopFees, amount);
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

  private @Nullable Boolean callTeleportDisplayIfAnyAvailable(Player player, ResultDisplay display, CachedShop cachedShop) {
    var shop = cachedShop.handle;
    var shopId = cachedShop.handle.getShopId();
    var shopLocation = shop.getLocation().clone(); // Not cloning can mess shops up (direct reference)!
    var isOtherWorld = shopLocation.getWorld() != player.getWorld();

    if (isOtherWorld && !PluginPermission.FEATURE_TELEPORT_OTHER_WORLD.has(player))
      return false;

    var applicableCooldowns = new ArrayList<TeleportCooldownType>();
    TeleportCooldownType cooldownType;

    if (isOtherWorld) {
      cooldownType = new TeleportCooldownType(
        PluginPermission.FEATURE_TELEPORT_OTHER_WORLD_BYPASS_COOLDOWN_SAME_SHOP,
        TELEPORT_COOLDOWN_KEY + "-other-world-shopId-" + shopId,
        getCooldownMillis(player, CooldownType.OTHER_WORLD_SAME_SHOP),
        config.rootSection.playerMessages.pendingCooldownFeatureTeleportOtherWorldSameShop
      );

      if (checkAndNotifyOfCooldown(player, cooldownType))
        return null;

      applicableCooldowns.add(cooldownType);

      cooldownType = new TeleportCooldownType(
        PluginPermission.FEATURE_TELEPORT_OTHER_WORLD_BYPASS_COOLDOWN_ANY_SHOP,
        TELEPORT_COOLDOWN_KEY + "-other-world",
        getCooldownMillis(player, CooldownType.OTHER_WORLD_ANY_SHOP),
        config.rootSection.playerMessages.pendingCooldownFeatureTeleportOtherWorldAnyShop
      );

      if (checkAndNotifyOfCooldown(player, cooldownType))
        return null;

      applicableCooldowns.add(cooldownType);
    }

    cooldownType = new TeleportCooldownType(
      PluginPermission.FEATURE_TELEPORT_BYPASS_COOLDOWN_SAME_SHOP,
      TELEPORT_COOLDOWN_KEY + "-shopId-" + shopId,
      getCooldownMillis(player, CooldownType.SAME_SHOP),
      config.rootSection.playerMessages.pendingCooldownFeatureTeleportSameShop
    );

    if (checkAndNotifyOfCooldown(player, cooldownType))
      return null;

    applicableCooldowns.add(cooldownType);

    cooldownType = new TeleportCooldownType(
      PluginPermission.FEATURE_TELEPORT_BYPASS_COOLDOWN_ANY_SHOP,
      TELEPORT_COOLDOWN_KEY,
      getCooldownMillis(player, CooldownType.ANY_SHOP),
      config.rootSection.playerMessages.pendingCooldownFeatureTeleportAnyShop
    );

    if (checkAndNotifyOfCooldown(player, cooldownType))
      return null;

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

    var nearestPlayerWarp = display.getNearestPlayerWarp(cachedShop);
    var nearestEssentialsWarp = display.getNearestEssentialsWarp(cachedShop);

    var canUseShopLocation = PluginPermission.FEATURE_TELEPORT_SHOP.has(player);
    var canUsePlayerWarp = PluginPermission.FEATURE_TELEPORT_PLAYER_WARP.has(player);
    var canUseEssentialsWarp = PluginPermission.FEATURE_TELEPORT_ESSENTIALS_WARP.has(player);

    var teleportDisplayData = new TeleportDisplayData(
      canUseShopLocation,
      targetLocation,
      canUsePlayerWarp,
      nearestPlayerWarp,
      canUseEssentialsWarp,
      nearestEssentialsWarp,
      display.getExtendedShopEnvironment(cachedShop),
      () -> reopen(display),
      () -> {
        for (var applicableCooldown : applicableCooldowns)
          stampStore.write(player.getUniqueId(), applicableCooldown.stampKey(), System.currentTimeMillis());
      }
    );

    if (!(teleportDisplayData.canUseShopLocation() || teleportDisplayData.playerWarpAvailable() || teleportDisplayData.essentialsWarpAvailable()))
      return false;

    teleportDisplayHandler.showOrTeleportDirectly(player, teleportDisplayData);
    return true;
  }

  private long getCooldownMillis(Player player, CooldownType type) {
    for (var groupEntry : config.rootSection.cooldowns.teleportToShop.groups.entrySet()) {
      var groupPermission = PluginPermission.TELEPORT_COOLDOWN_GROUP_BASE.nodeWithSuffix(groupEntry.getKey());

      if (player.hasPermission(groupPermission))
        return groupEntry.getValue().getCooldownMillis(type);
    }

    return config.rootSection.cooldowns.teleportToShop._default.getCooldownMillis(type);
  }

  private void ensurePermission(Player player, PluginPermission permission, @Nullable BukkitEvaluable missingMessage, Runnable handler) {
    if (permission.has(player)) {
      handler.run();
      return;
    }

    if (missingMessage != null)
      missingMessage.sendMessage(player, config.rootSection.builtBaseEnvironment);
  }

  private MaxUnitsResult calculateMaxUnits(Player player, CachedShop cachedShop, CalculatedFees calculatedFees) {
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

    double shopPrice;
    int maxUnitsByBalance;

    if (cachedShop.handle.isSelling()) {
      // The customer always pays the fees - thus, use the increased price
      shopPrice = cachedShop.cachedPrice + calculatedFees.relativeFeesValue() + calculatedFees.absoluteFees();
      var playerBalance = remoteInteractionApi.getPlayerBalance(player, cachedShop.handle);
      maxUnitsByBalance = (int) (playerBalance / shopPrice);
    } else {
      // The seller does not pay any fees - the fees will also not exceed the yield
      // Thus, calculate max-units based on the unaltered price
      shopPrice = cachedShop.cachedPrice;
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
