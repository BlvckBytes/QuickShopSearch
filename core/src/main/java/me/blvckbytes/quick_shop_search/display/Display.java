package me.blvckbytes.quick_shop_search.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public abstract class Display<DisplayDataType> {

  protected final Player player;
  protected final ConfigKeeper<MainSection> config;
  protected final PlatformScheduler scheduler;
  public final DisplayDataType displayData;
  protected Inventory inventory;

  protected Display(
    Player player,
    DisplayDataType displayData,
    ConfigKeeper<MainSection> config,
    PlatformScheduler scheduler
  ) {
    this.player = player;
    this.displayData = displayData;
    this.config = config;
    this.scheduler = scheduler;
  }

  public void show() {
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

  protected abstract void renderItems();

  protected abstract Inventory makeInventory();

  public abstract void onConfigReload();

  public void onInventoryClose() {
    if (this.inventory != null)
      this.inventory.clear();
  }

  public void onShutdown() {
    if (inventory != null)
      inventory.clear();

    if (player.getOpenInventory().getTopInventory() == inventory)
      player.closeInventory();
  }

  public boolean isInventory(Inventory inventory) {
    return this.inventory == inventory;
  }
}
