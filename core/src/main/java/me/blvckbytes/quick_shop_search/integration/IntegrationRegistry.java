package me.blvckbytes.quick_shop_search.integration;

import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.bukkitevaluable.ReloadPriority;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.integration.essentials_warps.EssentialsWarpsIntegration;
import me.blvckbytes.quick_shop_search.integration.essentials_warps.IEssentialsWarpsIntegration;
import me.blvckbytes.quick_shop_search.integration.player_warps.AxPlayerWarpsIntegration;
import me.blvckbytes.quick_shop_search.integration.player_warps.IPlayerWarpsIntegration;
import me.blvckbytes.quick_shop_search.integration.player_warps.OlzieDevPlayerWarpsIntegration;
import me.blvckbytes.quick_shop_search.integration.player_warps.RevivaloPlayerWarpsIntegration;
import me.blvckbytes.quick_shop_search.integration.worldguard.IWorldGuardIntegration;
import me.blvckbytes.quick_shop_search.integration.worldguard.WorldGuardIntegration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

public class IntegrationRegistry {

  private final ConfigKeeper<MainSection> config;
  private final Logger logger;
  private final PlatformScheduler scheduler;
  private final Plugin plugin;

  private @Nullable IEssentialsWarpsIntegration essentialsWarpsIntegration;
  private @Nullable IPlayerWarpsIntegration playerWarpsIntegration;
  private @Nullable IWorldGuardIntegration worldGuardIntegration;

  public IntegrationRegistry(
    ConfigKeeper<MainSection> config,
    Logger logger,
    PlatformScheduler scheduler,
    Plugin plugin
  ) {
    this.config = config;
    this.logger = logger;
    this.scheduler = scheduler;
    this.plugin = plugin;

    this.loadIntegrations();

    config.registerReloadListener(this::loadIntegrations, ReloadPriority.HIGHEST);
  }

  public @Nullable IWorldGuardIntegration getWorldGuardIntegration() {
    if (!config.rootSection.worldGuardIntegration.enabled)
      return null;

    return this.worldGuardIntegration;
  }

  public @Nullable IEssentialsWarpsIntegration getEssentialsWarpsIntegration() {
    if (!config.rootSection.essentialsWarpsIntegration.enabled)
      return null;

    return this.essentialsWarpsIntegration;
  }

  public @Nullable IPlayerWarpsIntegration getPlayerWarpsIntegration() {
    if (!config.rootSection.playerWarpsIntegration.enabled)
      return null;

    return this.playerWarpsIntegration;
  }

  private void loadIntegrations() {
    if (this.worldGuardIntegration == null)
      this.worldGuardIntegration = tryLoadWorldGuardIntegration();

    if (this.essentialsWarpsIntegration == null)
      this.essentialsWarpsIntegration = tryLoadEssentialsWarpsIntegration();

    if (this.playerWarpsIntegration == null)
      this.playerWarpsIntegration = tryLoadPlayerWarpsIntegration();
  }

  private @Nullable IWorldGuardIntegration tryLoadWorldGuardIntegration() {
    if (!config.rootSection.worldGuardIntegration.enabled)
      return null;

    if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
      logger.warning("WorldGuard-Integration is enabled, but the corresponding plugin could not be located!");
      return null;
    }

    if (this.worldGuardIntegration != null)
      return this.worldGuardIntegration;

    try {
      var worldGuardIntegration = new WorldGuardIntegration(logger, config);
      logger.info("Successfully loaded the WorldGuard-integration!");
      return worldGuardIntegration;
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "Could not load the WorldGuard-integration!", e);
    }

    return null;
  }

  private @Nullable IEssentialsWarpsIntegration tryLoadEssentialsWarpsIntegration() {
    if (!config.rootSection.essentialsWarpsIntegration.enabled)
      return null;

    if (!Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
      logger.warning("Essentials-Warps-Integration is enabled, but the corresponding plugin could not be located!");
      return null;
    }

    if (this.essentialsWarpsIntegration != null)
      return this.essentialsWarpsIntegration;

    try {
      var essentialsWarpsIntegration = new EssentialsWarpsIntegration(logger, scheduler, worldGuardIntegration, config);
      Bukkit.getPluginManager().registerEvents(essentialsWarpsIntegration, plugin);
      logger.info("Successfully loaded the Essentials-Warps-integration!");
      return essentialsWarpsIntegration;
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "Could not load the Essentials-Warps-integration!", e);
    }

    return null;
  }

  private @Nullable IPlayerWarpsIntegration tryLoadPlayerWarpsIntegration() {
    IPlayerWarpsIntegration integration;

    if (!config.rootSection.playerWarpsIntegration.enabled)
      return null;

    if (!(
      Bukkit.getPluginManager().isPluginEnabled("PlayerWarps") ||
      Bukkit.getPluginManager().isPluginEnabled("AxPlayerWarps")
    )) {
      logger.warning("PlayerWarps-Integration is enabled, but the corresponding plugin could not be located!");
      return null;
    }

    if (this.playerWarpsIntegration != null)
      return this.playerWarpsIntegration;

    try {
      integration = new OlzieDevPlayerWarpsIntegration(logger, scheduler, config, worldGuardIntegration);
      Bukkit.getPluginManager().registerEvents(integration, plugin);
      logger.info("Successfully loaded the com.olziedev PlayerWarps-integration!");
      return integration;
    } catch (Throwable firstError) {
      try {
        integration = new RevivaloPlayerWarpsIntegration(logger, scheduler, config, worldGuardIntegration);
        logger.info("Successfully loaded the dev.revivalo PlayerWarps-integration!");
        return integration;
      } catch (Throwable secondError) {
        try {
          integration = new AxPlayerWarpsIntegration(logger, scheduler, config, worldGuardIntegration);
          logger.info("Successfully loaded the AxPlayerWarps-integration!");
          return integration;
        } catch (Throwable thirdError) {
          logger.log(Level.SEVERE, "Could not load the AxPlayerWarps-integration!", thirdError);
        }

        logger.log(Level.SEVERE, "Could not load the dev.revivalo PlayerWarps-integration!", secondError);
      }

      logger.log(Level.SEVERE, "Could not load the com.olziedev PlayerWarps-integration!", firstError);
    }

    return null;
  }
}
