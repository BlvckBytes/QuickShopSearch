package me.blvckbytes.quick_shop_search.display;

import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.quick_shop_search.CachedShop;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ResultDisplayHandler implements Listener {

  private final Plugin plugin;
  private final MainSection mainSection;

  private final SelectionStateStore stateStore;
  private final Map<UUID, ResultDisplay> displayByPlayer;

  public ResultDisplayHandler(
    Plugin plugin,
    MainSection mainSection,
    SelectionStateStore stateStore
  ) {
    this.plugin = plugin;
    this.mainSection = mainSection;
    this.stateStore = stateStore;
    this.displayByPlayer = new HashMap<>();
  }

  public void show(Player player, Collection<CachedShop> shops) {
    displayByPlayer.put(player.getUniqueId(), new ResultDisplay(plugin, mainSection, player, shops, stateStore.loadState(player)));
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player player))
      return;

    var playerId = player.getUniqueId();
    var display = displayByPlayer.get(playerId);

    // Only remove on inventory match, as to prevent removal on title update
    if (display != null && display.isInventory(event.getInventory())) {
      displayByPlayer.remove(playerId);
      display.cleanup(false);
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

    if (display == null || !display.isInventory(player.getOpenInventory().getTopInventory()))
      return;

    event.setCancelled(true);

    var slot = event.getRawSlot();

    if (slot < 0 || slot >= ResultDisplay.INVENTORY_N_ROWS * 9)
      return;

    var targetShop = display.getShopCorrespondingToSlot(slot);
    var clickType = event.getClick();

    if (clickType == ClickType.LEFT) {
      if (slot == ResultDisplay.BACKWARDS_SLOT_ID) {
        display.previousPage();
        return;
      }

      if (slot == ResultDisplay.FORWARDS_SLOT_ID) {
        display.nextPage();
        return;
      }

      if (slot == ResultDisplay.SORTING_SLOT_ID) {
        ensurePermission(player, PluginPermission.FEATURE_SORT, mainSection.playerMessages.missingPermissionFeatureSort, display::nextSortingSelection);
        return;
      }

      if (slot == ResultDisplay.FILTERING_SLOT_ID) {
        ensurePermission(player, PluginPermission.FEATURE_FILTER, mainSection.playerMessages.missingPermissionFeatureFilter, display::nextFilteringCriterion);
        return;
      }

      if (targetShop != null) {
        var shopLocation = targetShop.getShop().getLocation();

        if (shopLocation.getWorld() != player.getWorld()) {
          ensurePermission(
            player, PluginPermission.FEATURE_TELEPORT_OTHER_WORLD,
            mainSection.playerMessages.missingPermissionFeatureTeleportOtherWorld,
            () -> teleportPlayerToShop(player, display, targetShop)
          );
          return;
        }

        ensurePermission(
          player, PluginPermission.FEATURE_TELEPORT,
          mainSection.playerMessages.missingPermissionFeatureTeleport,
          () -> teleportPlayerToShop(player, display, targetShop)
        );
        return;
      }

      return;
    }

    if (clickType == ClickType.RIGHT) {
      if (slot == ResultDisplay.BACKWARDS_SLOT_ID) {
        display.firstPage();
        return;
      }

      if (slot == ResultDisplay.FORWARDS_SLOT_ID) {
        display.lastPage();
        return;
      }

      if (slot == ResultDisplay.SORTING_SLOT_ID) {
        ensurePermission(player, PluginPermission.FEATURE_SORT, mainSection.playerMessages.missingPermissionFeatureSort, display::nextSortingOrder);
        return;
      }

      if (slot == ResultDisplay.FILTERING_SLOT_ID) {
        ensurePermission(player, PluginPermission.FEATURE_FILTER, mainSection.playerMessages.missingPermissionFeatureFilter, display::nextFilteringState);
        return;
      }

      if (targetShop != null)
        targetShop.getShop().openPreview(player);

      return;
    }

    if (clickType == ClickType.DROP) {
      if (slot == ResultDisplay.SORTING_SLOT_ID)
        ensurePermission(player, PluginPermission.FEATURE_SORT, mainSection.playerMessages.missingPermissionFeatureSort, display::moveSortingSelectionDown);
    }
  }

  private void teleportPlayerToShop(Player player, ResultDisplay display, CachedShop cachedShop) {
    BukkitEvaluable message;

    if ((message = mainSection.playerMessages.beforeTeleporting) != null) {
      player.sendMessage(message.stringify(
        mainSection.getBaseEnvironment().build(display.getDistanceExtendedShopEnvironment(cachedShop))
      ));
    }

    // TODO: Implement safe teleport
    player.teleport(cachedShop.getShop().getLocation().clone().add(.5, 0, .5));
    player.closeInventory();
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player player))
      return;

    var display = displayByPlayer.get(player.getUniqueId());

    if (display == null || display.isInventory(event.getInventory()))
      return;

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
      player.sendMessage(missingMessage.stringify(mainSection.getBaseEnvironment().build()));
  }
}
