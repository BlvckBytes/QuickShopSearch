package me.blvckbytes.quick_shop_search.display;

import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public abstract class Display<DisplayDataType> {

  protected final Player player;
  protected final ConfigKeeper<MainSection> config;
  protected final DisplayDataType displayData;

  protected Display(
    Player player,
    DisplayDataType displayData,
    ConfigKeeper<MainSection> config
  ) {
    this.player = player;
    this.displayData = displayData;
    this.config = config;
  }

  public abstract void onConfigReload();

  public abstract void onInventoryClose();

  public abstract void onShutdown();

  public abstract boolean isInventory(Inventory inventory);

}
