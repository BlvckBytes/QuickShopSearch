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

  public void showOrTeleportDirectly(Player player, TeleportDisplayData displayData) {
    if (displayData.canUseShopLocation() && !displayData.playerWarpAvailable() && !displayData.essentialsWarpAvailable()) {
      if (teleportToShopLocation(player, displayData, false))
        return;
    }

    if (!displayData.canUseShopLocation() && displayData.playerWarpAvailable() && !displayData.essentialsWarpAvailable()) {
      if (teleportToPlayerWarp(player, displayData, false))
        return;
    }

    if (!displayData.canUseShopLocation() && !displayData.playerWarpAvailable() && displayData.essentialsWarpAvailable()) {
      if (teleportToEssentialsWarp(player, displayData, false))
        return;
    }

    show(player, displayData);
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

    if (config.rootSection.teleportDisplay.items.shopLocation.getDisplaySlots().contains(slot)) {
      teleportToShopLocation(player, display.displayData, true);
      return;
    }

    if (config.rootSection.teleportDisplay.items.nearestPlayerWarpLocation.getDisplaySlots().contains(slot)) {
      teleportToPlayerWarp(player, display.displayData, true);
      return;
    }

    if (config.rootSection.teleportDisplay.items.nearestEssentialsWarpLocation.getDisplaySlots().contains(slot))
      teleportToEssentialsWarp(player, display.displayData, true);
  }

  private boolean teleportToEssentialsWarp(Player player, TeleportDisplayData displayData, boolean sendFailureMessage) {
    var nearestEssentialsWarp = displayData.nearestEssentialsWarp();

    if (nearestEssentialsWarp == null)
      return false;

    BukkitEvaluable message;

    if (!displayData.canUseEssentialsWarp()) {
      if (sendFailureMessage && (message = config.rootSection.playerMessages.missingPermissionFeatureTeleport) != null)
        message.sendMessage(player, config.rootSection.builtBaseEnvironment);

      return false;
    }

    scheduler.runAtEntity(player, scheduleTask -> player.closeInventory());

    if ((message = config.rootSection.playerMessages.beforeTeleportingNearestEssentialsWarp) != null)
      message.sendMessage(player, config.rootSection.getBaseEnvironment().build(displayData.extendedShopEnvironment()));

    slowTeleportManager.initializeTeleportation(player, nearestEssentialsWarp.location(), () -> {
      if (displayData.afterTeleporting() != null)
        displayData.afterTeleporting().run();
    });

    return true;
  }

  private boolean teleportToPlayerWarp(Player player, TeleportDisplayData displayData, boolean sendFailureMessage) {
    var nearestPlayerWarp = displayData.nearestPlayerWarp();

    if (nearestPlayerWarp == null)
      return false;

    BukkitEvaluable message;

    if (!displayData.canUsePlayerWarp()) {
      if (sendFailureMessage && (message = config.rootSection.playerMessages.missingPermissionFeatureTeleport) != null)
        message.sendMessage(player, config.rootSection.builtBaseEnvironment);

      return false;
    }

    scheduler.runAtEntity(player, scheduleTask -> player.closeInventory());

    if (nearestPlayerWarp.isBanned() && !PluginPermission.FEATURE_TELEPORT_NEAREST_PLAYER_WARP_BAN_BYPASS.has(player)) {
      if (sendFailureMessage && (message = config.rootSection.playerMessages.nearestPlayerWarpBanned) != null)
        message.sendMessage(player, displayData.extendedShopEnvironment());

      return false;
    }

    if ((message = config.rootSection.playerMessages.beforeTeleportingNearestPlayerWarp) != null)
      message.sendMessage(player, config.rootSection.getBaseEnvironment().build(displayData.extendedShopEnvironment()));

    slowTeleportManager.initializeTeleportation(player, nearestPlayerWarp.location(), () -> {
      if (displayData.afterTeleporting() != null)
        displayData.afterTeleporting().run();
    });

    return true;
  }

  private boolean teleportToShopLocation(Player player, TeleportDisplayData displayData, boolean sendFailureMessage) {
    BukkitEvaluable message;

    if (!displayData.canUseShopLocation()) {
      if (sendFailureMessage && (message = config.rootSection.playerMessages.missingPermissionFeatureTeleport) != null)
        message.sendMessage(player, config.rootSection.builtBaseEnvironment);

      return false;
    }

    scheduler.runAtEntity(player, scheduleTask -> player.closeInventory());

    if ((message = config.rootSection.playerMessages.beforeTeleporting) != null)
      message.sendMessage(player, config.rootSection.getBaseEnvironment().build(displayData.extendedShopEnvironment()));

    slowTeleportManager.initializeTeleportation(player, displayData.finalShopLocation(), () -> {
      if (displayData.afterTeleporting() != null)
        displayData.afterTeleporting().run();
    });

    return true;
  }
}
