package me.blvckbytes.quick_shop_search.display;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.shop.ShopAction;
import com.ghostchu.quickshop.api.shop.ShopType;
import com.ghostchu.quickshop.shop.SimpleInfo;
import com.ghostchu.quickshop.shop.inventory.BukkitInventoryWrapper;
import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.ChatPromptManager;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.ShopUpdate;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.*;

public class ResultDisplayHandler implements Listener {

  private static final long MOVE_GHOST_ITEM_THRESHOLD_MS = 500;
  private static final double PLAYER_EYE_HEIGHT = 1.5;

  private static final BlockFace[] SHOP_SIGN_FACES = new BlockFace[] {
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  private final PlatformScheduler scheduler;

  private final ConfigKeeper<MainSection> config;

  private final SelectionStateStore stateStore;
  private final ChatPromptManager chatPromptManager;
  private final Map<UUID, ResultDisplay> displayByPlayer;

  public ResultDisplayHandler(
    PlatformScheduler scheduler,
    ConfigKeeper<MainSection> config,
    SelectionStateStore stateStore,
    ChatPromptManager chatPromptManager
  ) {
    this.scheduler = scheduler;
    this.stateStore = stateStore;
    this.chatPromptManager = chatPromptManager;
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
    var extendedEnvironment = config.rootSection.getBaseEnvironment().build(
      display.getDistanceExtendedShopEnvironment(cachedShop)
    );

    scheduler.runAtEntity(player, scheduleTask -> player.closeInventory());

    var didOverwritePrevious = chatPromptManager.register(
      player,
      input -> {
        if (input.equalsIgnoreCase("cancel")) {
          config.rootSection.playerMessages.shopInteractPromptCancelCurrent.sendMessage(
            player, config.rootSection.builtBaseEnvironment
          );

          return;
        }

        var amount = tryParseStrictlyPositiveInteger(input);

        if (amount <= 0) {
          config.rootSection.playerMessages.shopInteractPromptInvalidInput.sendMessage(
            player,
            config.rootSection.getBaseEnvironment()
              .withStaticVariable("input", input)
              .build()
          );

          return;
        }

        config.rootSection.playerMessages.shopInteractPromptDispatch.sendMessage(
          player,
          new EvaluationEnvironmentBuilder()
            .withStaticVariable("amount", amount)
            .build(extendedEnvironment)
        );

        dispatchShopInteraction(player, cachedShop, amount);
      },
      () -> config.rootSection.playerMessages.shopInteractPromptTimeout.sendMessage(
        player, extendedEnvironment
      )
    );

    if (didOverwritePrevious)
      config.rootSection.playerMessages.shopInteractPromptCancelPrevious.sendMessage(player, config.rootSection.builtBaseEnvironment);

    if (cachedShop.handle.getShopType() == ShopType.BUYING) {
      config.rootSection.playerMessages.shopInteractPromptBuying.sendMessage(player, extendedEnvironment);

      return;
    }

    config.rootSection.playerMessages.shopInteractPromptSelling.sendMessage(player, extendedEnvironment);
  }

  private int tryParseStrictlyPositiveInteger(String input) {
    try {
      var result = Integer.parseInt(input);

      if (result > 0)
        return result;
    } catch (NumberFormatException ignored) {}

    return -1;
  }

  private void dispatchShopInteraction(Player player, CachedShop cachedShop, int amount) {
    scheduler.runAtLocation(cachedShop.handle.getLocation(), scheduleTask -> {
      var tradeInfo = new SimpleInfo(
        cachedShop.handle.getLocation(),
        cachedShop.handle.isBuying() ? ShopAction.PURCHASE_SELL : ShopAction.PURCHASE_BUY,
        null, null, cachedShop.handle, false
      );

      var wrappedInventory = new BukkitInventoryWrapper(player.getInventory());

      if (cachedShop.handle.isBuying()) {
        QuickShopAPI.getInstance().getShopManager().actionBuying(
          player, wrappedInventory,
          QuickShop.getInstance().getEconomy(),
          tradeInfo, cachedShop.handle, amount
        );
      } else {
        QuickShopAPI.getInstance().getShopManager().actionSelling(
          player, wrappedInventory,
          QuickShop.getInstance().getEconomy(),
          tradeInfo, cachedShop.handle, amount
        );
      }
    });
  }

  private void teleportPlayerToShop(Player player, ResultDisplay display, CachedShop cachedShop) {
    config.rootSection.playerMessages.beforeTeleporting.sendMessage(
      player,
      config.rootSection.getBaseEnvironment().build(display.getDistanceExtendedShopEnvironment(cachedShop))
    );

    var shop = cachedShop.handle;
    var shopLocation = shop.getLocation().clone(); // Not cloning can mess shops up (direct reference)!
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

    scheduler.teleportAsync(player, targetLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
    scheduler.runAtEntity(player, scheduleTask -> player.closeInventory());
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

  private void ensurePermission(Player player, PluginPermission permission, BukkitEvaluable missingMessage, Runnable handler) {
    if (permission.has(player)) {
      handler.run();
      return;
    }

    missingMessage.sendMessage(player, config.rootSection.builtBaseEnvironment);
  }
}
