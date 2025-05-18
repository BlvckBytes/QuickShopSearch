package me.blvckbytes.quick_shop_search.display.teleport;

import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.SlowTeleportManager;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.display.DisplayHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class TeleportDisplayHandler extends DisplayHandler<TeleportDisplay, TeleportDisplayData> {

  private final SlowTeleportManager slowTeleportManager;

  public TeleportDisplayHandler(
    ConfigKeeper<MainSection> config,
    PlatformScheduler scheduler,
    SlowTeleportManager slowTeleportManager
  ) {
    super(config, scheduler);

    this.slowTeleportManager = slowTeleportManager;
  }

  @Override
  public TeleportDisplay instantiateDisplay(Player player, TeleportDisplayData displayData) {
    return new TeleportDisplay(player, displayData, config, scheduler);
  }

  @Override
  protected void handleClick(Player player, TeleportDisplay display, ClickType clickType, int slot) {
    if (clickType != ClickType.LEFT)
      return;

    if (config.rootSection.teleportDisplay.items.back.getDisplaySlots().contains(slot)) {
      display.displayData.reopenResultDisplay().run();
      return;
    }

    BukkitEvaluable message;

    if (config.rootSection.teleportDisplay.items.shopLocation.getDisplaySlots().contains(slot)) {
      directlyTeleportToShopLocation(player, display.displayData);
      return;
    }

    if (config.rootSection.teleportDisplay.items.nearestPlayerWarpLocation.getDisplaySlots().contains(slot)) {
      var nearestPlayerWarp = display.displayData.nearestPlayerWarp();

      if (nearestPlayerWarp == null)
        return;

      scheduler.runAtEntity(player, scheduleTask -> player.closeInventory());

      if (nearestPlayerWarp.isBanned() && !PluginPermission.FEATURE_TELEPORT_NEAREST_PLAYER_WARP_BAN_BYPASS.has(player)) {
        if ((message = config.rootSection.playerMessages.nearestPlayerWarpBanned) != null)
          message.sendMessage(player, display.displayData.extendedShopEnvironment());

        return;
      }

      if ((message = config.rootSection.playerMessages.beforeTeleportingNearestPlayerWarp) != null)
        message.sendMessage(player, config.rootSection.getBaseEnvironment().build(display.displayData.extendedShopEnvironment()));

      slowTeleportManager.initializeTeleportation(player, nearestPlayerWarp.location(), () -> {
        if (display.displayData.afterTeleporting() != null)
          display.displayData.afterTeleporting().run();
      });

      return;
    }

    if (config.rootSection.teleportDisplay.items.nearestEssentialsWarpLocation.getDisplaySlots().contains(slot)) {
      var nearestEssentialsWarp = display.displayData.nearestEssentialsWarp();

      if (nearestEssentialsWarp == null)
        return;

      scheduler.runAtEntity(player, scheduleTask -> player.closeInventory());

      if ((message = config.rootSection.playerMessages.beforeTeleportingNearestEssentialsWarp) != null)
        message.sendMessage(player, config.rootSection.getBaseEnvironment().build(display.displayData.extendedShopEnvironment()));

      slowTeleportManager.initializeTeleportation(player, nearestEssentialsWarp.location(), () -> {
        if (display.displayData.afterTeleporting() != null)
          display.displayData.afterTeleporting().run();
      });
    }
  }

  public void directlyTeleportToShopLocation(Player player, TeleportDisplayData displayData) {
    scheduler.runAtEntity(player, scheduleTask -> player.closeInventory());

    BukkitEvaluable message;

    if ((message = config.rootSection.playerMessages.beforeTeleporting) != null)
      message.sendMessage(player, config.rootSection.getBaseEnvironment().build(displayData.extendedShopEnvironment()));

    slowTeleportManager.initializeTeleportation(player, displayData.finalShopLocation(), () -> {
      if (displayData.afterTeleporting() != null)
        displayData.afterTeleporting().run();
    });
  }
}
