package me.blvckbytes.quick_shop_search.display.teleport;

import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
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

    BukkitEvaluable message;

    if (!displayData.canUseEssentialsWarp()) {
      if (sendFailureMessage && (message = config.rootSection.playerMessages.missingPermissionFeatureTeleport) != null)
        message.sendMessage(player, config.rootSection.builtBaseEnvironment);

      return false;
    }

    scheduler.runAtEntity(player, scheduleTask -> player.closeInventory());

    if ((message = config.rootSection.playerMessages.beforeTeleportingNearestEssentialsWarp) != null)
      message.sendMessage(player, config.rootSection.getBaseEnvironment().build(displayData.extendedShopEnvironment()));

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
    BukkitEvaluable command;

    switch (data.source()) {
      case REVIVALO -> command = config.rootSection.playerWarpsIntegration.teleportCommand.revivalo;
      case OLZIE_DEV -> command = config.rootSection.playerWarpsIntegration.teleportCommand.olzieDev;
      default -> {
        return;
      }
    }

    try {
      String commandString = command.asScalar(
        ScalarType.STRING,
        new EvaluationEnvironmentBuilder()
          .withStaticVariable("name", data.warpName())
          .build()
      );

      player.performCommand(commandString);
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "Could not evaluate/dispatch a player-warp teleportation-command", e);
    }
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
