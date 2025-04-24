package me.blvckbytes.quick_shop_search;

import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.config.SlowTeleportNotification;
import me.blvckbytes.quick_shop_search.config.SlowTeleportParametersSection;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SlowTeleportManager implements Listener {

  private final Map<UUID, Long> lastCombatTimestampById;
  private final Set<UUID> permittedToTeleport;

  private final PlatformScheduler scheduler;
  private final ConfigKeeper<MainSection> config;

  public SlowTeleportManager(PlatformScheduler platformScheduler, ConfigKeeper<MainSection> config) {
    this.scheduler = platformScheduler;
    this.config = config;
    this.lastCombatTimestampById = new HashMap<>();
    this.permittedToTeleport = new HashSet<>();
  }

  public void initializeTeleportation(Player player, Location location, @Nullable Runnable afterTeleportation) {
    var parameters = decideParameters(player);
    var countdownValue = parameters.durationSeconds;

    if (countdownValue == 0 || PluginPermission.SLOW_TELEPORT_BYPASS.has(player)) {
      scheduler.teleportAsync(player, location, PlayerTeleportEvent.TeleportCause.PLUGIN);

      if (afterTeleportation != null)
        afterTeleportation.run();

      return;
    }

    permittedToTeleport.add(player.getUniqueId());

    new CountdownInstance(player, parameters, countdownValue, () -> {
      scheduler.teleportAsync(player, location, PlayerTeleportEvent.TeleportCause.PLUGIN);

      if (afterTeleportation != null)
        afterTeleportation.run();
    }).advanceCountdown();
  }

  private class CountdownInstance {
    Player player;
    SlowTeleportParametersSection parameters;
    @Nullable Runnable elapsedCallback;
    int remainingSeconds;
    long lastTitleDisplayStamp;

    CountdownInstance(Player player, SlowTeleportParametersSection parameters, int remainingSeconds, @Nullable Runnable elapsedCallback) {
      this.player = player;
      this.parameters = parameters;
      this.remainingSeconds = remainingSeconds;
      this.elapsedCallback = elapsedCallback;
      this.lastTitleDisplayStamp = 0;
    }

    void advanceCountdown() {
      if (!permittedToTeleport.contains(player.getUniqueId()))
        return;

      if (remainingSeconds == 0) {
        permittedToTeleport.remove(player.getUniqueId());

        if (elapsedCallback != null)
          elapsedCallback.run();
      }

      if (parameters._notificationAtSeconds.containsKey(remainingSeconds)) {
        SlowTeleportNotification notification = parameters._notificationAtSeconds.get(remainingSeconds);

        if (notification == null)
          notification = parameters.fallbackNotification;

        if (notification != null)
          playAtSecondsNotification(notification);
      }

      if (--remainingSeconds >= 0)
        scheduler.runLater(this::advanceCountdown, 20L);
    }

    void playAtSecondsNotification(SlowTeleportNotification notification) {
      if (notification._sound != null) {
        scheduler.runLater(
          () -> notification._sound.play(player, notification.soundVolume, notification.soundPitch),
          // Otherwise, the ending-sound may not "travel with them"
          remainingSeconds == 0 ? 2 : 0
        );
      }

      var messageEnvironment = config.rootSection.getBaseEnvironment()
        .withStaticVariable("remaining_seconds", remainingSeconds)
        .build();

      BukkitEvaluable message;

      if ((message = notification.messages.messageChat) != null)
        message.sendMessage(player, messageEnvironment);

      if ((message = notification.messages.messageActionBar) != null)
        message.sendActionBarMessage(player, messageEnvironment);

      if ((message = notification.messages.messageTitle) != null || notification.messages.messageSubTitle != null) {
        var applicator = message != null ? message.applicator : notification.messages.messageSubTitle.applicator;

        var stampNow = System.currentTimeMillis();
        var elapsedMillisSinceLastTitle = stampNow - lastTitleDisplayStamp;
        lastTitleDisplayStamp = stampNow;

        applicator.sendTitles(
          player,
          message, messageEnvironment,
          notification.messages.messageSubTitle, messageEnvironment,
          // Do not fade in unless the last title is completely gone already, as to avoid a flickering display
          elapsedMillisSinceLastTitle > 1750 ? 10 : 0,
          // Stay for a tad bit over a second, as to allow for latency
          23,
          // Fade out of the previous message will be cancelled by a new one - thus, no flicker
          10
        );
      }
    }
  }

  private SlowTeleportParametersSection decideParameters(Player player) {
    Long lastCombatTimestamp;

    var durationMillis = config.rootSection.slowTeleport.combatLogCoolOffDurationSeconds * 1000L;

    if (durationMillis != 0 && (lastCombatTimestamp = lastCombatTimestampById.get(player.getUniqueId())) != null) {
      var elapsedMillis = System.currentTimeMillis() - lastCombatTimestamp;

      if (elapsedMillis <= durationMillis)
        return config.rootSection.slowTeleport.whenInCombat;
    }

    return config.rootSection.slowTeleport.whenNotInCombat;
  }

  private void tookDamageByNonPlayer(Player victim) {
    if (!config.rootSection.slowTeleport.cancelIfDamagedByNonPlayer)
      return;

    if (!permittedToTeleport.remove(victim.getUniqueId()))
      return;

    BukkitEvaluable message;

    if ((message = config.rootSection.playerMessages.slowTeleportTookDamageByNonPlayer) != null)
      message.sendMessage(victim, config.rootSection.builtBaseEnvironment);
  }

  private void tookDamageByPlayer(Player victim, Player attacker) {
    long stampNow = System.currentTimeMillis();
    lastCombatTimestampById.put(victim.getUniqueId(), stampNow);
    lastCombatTimestampById.put(attacker.getUniqueId(), stampNow);

    if (!config.rootSection.slowTeleport.cancelIfDamagedByPlayer)
      return;

    if (permittedToTeleport.remove(victim.getUniqueId())) {
      BukkitEvaluable message;

      if ((message = config.rootSection.playerMessages.slowTeleportHasBeenAttacked) != null) {
        message.sendMessage(
          victim,
          config.rootSection.getBaseEnvironment()
            .withStaticVariable("attacker", attacker.getName())
            .build()
        );
      }
    }

    if (permittedToTeleport.remove(attacker.getUniqueId())) {
      BukkitEvaluable message;

      if ((message = config.rootSection.playerMessages.slowTeleportAttackedSomebody) != null) {
        message.sendMessage(
          attacker,
          config.rootSection.getBaseEnvironment()
            .withStaticVariable("victim", victim.getName())
            .build()
        );
      }
    }
  }

  @EventHandler
  public void onMove(PlayerMoveEvent event) {
    var destinationLocation = event.getTo();

    if (destinationLocation == null)
      return;

    var player = event.getPlayer();

    if (event.getFrom().distanceSquared(destinationLocation) < .005)
      return;

    if (!permittedToTeleport.remove(player.getUniqueId()))
      return;

    BukkitEvaluable message;

    if ((message = config.rootSection.playerMessages.slowTeleportHasMoved) != null)
      message.sendMessage(player, config.rootSection.builtBaseEnvironment);
  }

  @EventHandler
  public void onDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player victim))
      return;

    if (event instanceof EntityDamageByEntityEvent byEntityEvent) {
      if (byEntityEvent.getDamager() instanceof Projectile projectile) {
        if (projectile.getShooter() instanceof Player shooter) {
          tookDamageByPlayer(victim, shooter);
          return;
        }
      }

      else if (byEntityEvent.getDamager() instanceof Player attacker) {
        tookDamageByPlayer(victim, attacker);
        return;
      }
    }

    tookDamageByNonPlayer(victim);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var playerId = event.getPlayer().getUniqueId();
    lastCombatTimestampById.remove(playerId);
    permittedToTeleport.remove(playerId);
  }
}
