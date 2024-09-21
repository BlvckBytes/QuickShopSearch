package me.blvckbytes.quick_shop_search.display;

import com.ghostchu.quickshop.api.shop.Shop;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ResultDisplayHandler implements Listener {

  private final MainSection mainSection;
  private final SelectionStateStore stateStore;
  private final Map<UUID, ResultDisplay> displayByPlayer;

  public ResultDisplayHandler(MainSection mainSection, SelectionStateStore stateStore) {
    this.mainSection = mainSection;
    this.stateStore = stateStore;
    this.displayByPlayer = new HashMap<>();
  }

  public void show(Player player, List<Shop> shops) {
    displayByPlayer.put(player.getUniqueId(), new ResultDisplay(mainSection, player, shops, stateStore.loadState(player)));
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
        display.nextSortingCriterion();
        return;
      }

      if (slot == ResultDisplay.FILTERING_SLOT_ID) {
        display.nextFilteringCriterion();
        return;
      }

      if (targetShop != null) {
        player.teleport(targetShop.getLocation().add(.5, 0, .5));
        player.closeInventory();
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
        display.toggleSortingOrder();
        return;
      }

      if (slot == ResultDisplay.FILTERING_SLOT_ID) {
        display.nextFilteringState();
        return;
      }

      if (targetShop != null)
        targetShop.openPreview(player);
    }
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
}
