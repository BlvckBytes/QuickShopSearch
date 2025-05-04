package me.blvckbytes.quick_shop_search.display.teleport;

import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.display.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class TeleportDisplay extends Display<TeleportDisplayData> {

  public TeleportDisplay(
    Player player,
    TeleportDisplayData displayData,
    ConfigKeeper<MainSection> config,
    PlatformScheduler scheduler
  ) {
    super(player, displayData, config, scheduler);

    show();
  }

  @Override
  protected void renderItems() {
    config.rootSection.teleportDisplay.items.filler.renderInto(inventory, displayData.extendedShopEnvironment());
    config.rootSection.teleportDisplay.items.back.renderInto(inventory, displayData.extendedShopEnvironment());
    config.rootSection.teleportDisplay.items.shopLocation.renderInto(inventory, displayData.extendedShopEnvironment());
    config.rootSection.teleportDisplay.items.nearestPlayerWarpLocation.renderInto(inventory, displayData.extendedShopEnvironment());
  }

  @Override
  protected Inventory makeInventory() {
    return config.rootSection.teleportDisplay.createInventory(displayData.extendedShopEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }
}
