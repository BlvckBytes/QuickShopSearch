package me.blvckbytes.quick_shop_search.display;

import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class DisplayHandler<DisplayType extends Display, DisplayDataType> implements Listener {

  // If players move to their own inventory and close the UI quickly enough, the server will send back a packet
  // undoing that slot which assumed the top-inventory to still be open, and thus the undo won't work. For survival,
  // it's merely cosmetic, but for creative, the client will actually call this item into existence. While not
  // necessarily critical, it just makes the plugin look bad. On closing the inventory, if the last move happened
  // within a certain threshold of time, let's just update the player's inventory, as to make that ghost-item vanish.
  private static final long MOVE_GHOST_ITEM_THRESHOLD_MS = 500;

  protected final ConfigKeeper<MainSection> config;
  protected final PlatformScheduler scheduler;

  private final Map<UUID, DisplayType> displayByPlayerId;
  private final Map<UUID, Long> lastMoveToOwnInventoryStampByPlayerId;

  protected DisplayHandler(
    ConfigKeeper<MainSection> config,
    PlatformScheduler scheduler
  ) {
    this.displayByPlayerId = new HashMap<>();
    this.lastMoveToOwnInventoryStampByPlayerId = new HashMap<>();
    this.config = config;
    this.scheduler = scheduler;

    config.registerReloadListener(() -> {
      for (var display : displayByPlayerId.values())
        display.onConfigReload();
    });
  }

  public abstract DisplayType instantiateDisplay(Player player, DisplayDataType displayData);

  public void show(Player player, DisplayDataType displayData) {
    displayByPlayerId.put(player.getUniqueId(), instantiateDisplay(player, displayData));
  }

  protected abstract void handleClick(Player player, DisplayType display, ClickType clickType, int slot);

  protected void forEachDisplay(Consumer<DisplayType> consumer) {
    for (var display : displayByPlayerId.values())
      consumer.accept(display);
  }

  public void onShutdown() {
    for (var displayIterator = displayByPlayerId.entrySet().iterator(); displayIterator.hasNext();) {
      displayIterator.next().getValue().onShutdown();
      displayIterator.remove();
    }
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player player))
      return;

    if (displayByPlayerId.containsKey(player.getUniqueId()))
      event.setCancelled(true);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var display = displayByPlayerId.remove(event.getPlayer().getUniqueId());

    if (display != null)
      display.onInventoryClose();
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player player))
      return;

    var playerId = player.getUniqueId();
    var display = displayByPlayerId.get(playerId);

    // Only remove on inventory match, as to prevent removal on title update
    if (display != null && display.isInventory(event.getInventory())) {
      display.onInventoryClose();
      displayByPlayerId.remove(playerId);

      var lastMoveToOwnInventoryStamp = lastMoveToOwnInventoryStampByPlayerId.remove(playerId);

      if (
        lastMoveToOwnInventoryStamp != null &&
        System.currentTimeMillis() - lastMoveToOwnInventoryStamp < MOVE_GHOST_ITEM_THRESHOLD_MS
      ) {
        scheduler.runNextTick(task -> player.updateInventory());
      }
    }
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player))
      return;

    var display = displayByPlayerId.get(player.getUniqueId());

    if (display == null)
      return;

    event.setCancelled(true);

    if (
      event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY &&
      event.getClickedInventory() != player.getInventory()
    ) {
      lastMoveToOwnInventoryStampByPlayerId.put(player.getUniqueId(), System.currentTimeMillis());
    }

    if (!display.isInventory(player.getOpenInventory().getTopInventory()))
      return;

    var slot = event.getRawSlot();

    if (slot < 0 || slot >= config.rootSection.resultDisplay.getRows() * 9)
      return;

    handleClick(player, display, event.getClick(), slot);
  }
}
