package me.blvckbytes.quick_shop_search.display.teleport;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.SlowTeleportManager;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.display.DisplayHandler;
import me.blvckbytes.quick_shop_search.integration.player_warps.PlayerWarpData;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TeleportDisplayHandler extends DisplayHandler<TeleportDisplay, TeleportDisplayData> {

  private final SlowTeleportManager slowTeleportManager;
  private final Logger logger;

  public TeleportDisplayHandler(
    ConfigKeeper<MainSection> config,
    PlatformScheduler scheduler,
    SlowTeleportManager slowTeleportManager,
    Logger logger
  ) {
    super(config, scheduler);

    this.slowTeleportManager = slowTeleportManager;
    this.logger = logger;
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

    ComponentMarkup message;

    if (!displayData.canUseEssentialsWarp()) {
      if (sendFailureMessage && (message = config.rootSection.playerMessages.missingPermissionFeatureTeleport) != null)
        message.sendMessage(player);

      return false;
    }

    scheduler.runAtEntity(player, scheduleTask -> player.closeInventory());

    if ((message = config.rootSection.playerMessages.beforeTeleportingNearestEssentialsWarp) != null)
      message.sendMessage(player, displayData.extendedShopEnvironment());

    slowTeleportManager.initializeTeleportation(
      player,
      scheduler -> scheduler.teleportAsync(player, nearestEssentialsWarp.location()),
      () -> {
        if (displayData.afterTeleporting() != null)
          displayData.afterTeleporting().run();
      }
    );

    return true;
  }

  private boolean teleportToPlayerWarp(Player player, TeleportDisplayData displayData, boolean sendFailureMessage) {
    var nearestPlayerWarp = displayData.nearestPlayerWarp();

    if (nearestPlayerWarp == null)
      return false;

    ComponentMarkup message;

    if (!displayData.canUsePlayerWarp()) {
      if (sendFailureMessage && (message = config.rootSection.playerMessages.missingPermissionFeatureTeleport) != null)
        message.sendMessage(player);

      return false;
    }

    scheduler.runAtEntity(player, scheduleTask -> player.closeInventory());

    if (nearestPlayerWarp.isBanned() && !PluginPermission.FEATURE_TELEPORT_NEAREST_PLAYER_WARP_BAN_BYPASS.has(player)) {
      if (sendFailureMessage && (message = config.rootSection.playerMessages.nearestPlayerWarpBanned) != null)
        message.sendMessage(player, displayData.extendedShopEnvironment());

      return false;
    }

    if ((message = config.rootSection.playerMessages.beforeTeleportingNearestPlayerWarp) != null)
      message.sendMessage(player, displayData.extendedShopEnvironment());

    if (config.rootSection.playerWarpsIntegration.doNotUseSlowTeleport) {
      performPlayerWarpTeleportCommand(player, nearestPlayerWarp);

      if (displayData.afterTeleporting() != null)
        displayData.afterTeleporting().run();

      return true;
    }

    slowTeleportManager.initializeTeleportation(
      player,
      scheduler -> performPlayerWarpTeleportCommand(player, nearestPlayerWarp),
      () -> {
        if (displayData.afterTeleporting() != null)
          displayData.afterTeleporting().run();
      }
    );

    return true;
  }

  private void performPlayerWarpTeleportCommand(Player player, PlayerWarpData data) {
    ComponentMarkup command;

    switch (data.source()) {
      case REVIVALO -> command = config.rootSection.playerWarpsIntegration.teleportCommand.revivalo;
      case OLZIE_DEV -> command = config.rootSection.playerWarpsIntegration.teleportCommand.olzieDev;
      case AX -> command = config.rootSection.playerWarpsIntegration.teleportCommand.ax;
      default -> {
        return;
      }
    }

    try {
      var commandString = command.asPlainString(
        new InterpretationEnvironment()
          .withVariable("name", data.warpName())
      );

      player.performCommand(commandString);
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "Could not evaluate/dispatch a player-warp teleportation-command", e);
    }
  }

  private boolean teleportToShopLocation(Player player, TeleportDisplayData displayData, boolean sendFailureMessage) {
    ComponentMarkup message;

    if (!displayData.canUseShopLocation()) {
      if (sendFailureMessage && (message = config.rootSection.playerMessages.missingPermissionFeatureTeleport) != null)
        message.sendMessage(player);

      return false;
    }

    scheduler.runAtEntity(player, scheduleTask -> player.closeInventory());

    if ((message = config.rootSection.playerMessages.beforeTeleporting) != null)
      message.sendMessage(player, displayData.extendedShopEnvironment());

    slowTeleportManager.initializeTeleportation(
      player,
      scheduler -> scheduler.teleportAsync(player, displayData.finalShopLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN),
      () -> {
        if (displayData.afterTeleporting() != null)
          displayData.afterTeleporting().run();
      }
    );

    return true;
  }
}
