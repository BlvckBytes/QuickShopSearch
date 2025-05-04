package me.blvckbytes.quick_shop_search.display;

import org.bukkit.inventory.Inventory;

public interface Display {

  void onConfigReload();

  void onInventoryClose();

  void onShutdown();

  boolean isInventory(Inventory inventory);

}
