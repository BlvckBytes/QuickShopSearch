package me.blvckbytes.quick_shop_search.display;

import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.inventory.Inventory;

public abstract class Display<DisplayDataType> {

  protected final ConfigKeeper<MainSection> config;
  protected final DisplayDataType displayData;

  protected Display(
    ConfigKeeper<MainSection> config,
    DisplayDataType displayData
  ) {
    this.config = config;
    this.displayData = displayData;
  }

  public abstract void onConfigReload();

  public abstract void onInventoryClose();

  public abstract void onShutdown();

  public abstract boolean isInventory(Inventory inventory);

}
