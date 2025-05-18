package me.blvckbytes.quick_shop_search.integration;

import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.integration.essentials_warps.EssentialsWarpsIntegration;
import me.blvckbytes.quick_shop_search.integration.essentials_warps.IEssentialsWarpsIntegration;
import me.blvckbytes.quick_shop_search.integration.player_warps.IPlayerWarpsIntegration;
import me.blvckbytes.quick_shop_search.integration.player_warps.OlzieDevPlayerWarpsIntegration;
import me.blvckbytes.quick_shop_search.integration.player_warps.RevivaloPlayerWarpsIntegration;
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
  }

  public @Nullable IEssentialsWarpsIntegration getEssentialsWarpsIntegration() {
    return this.essentialsWarpsIntegration;
  }

  public @Nullable IPlayerWarpsIntegration getPlayerWarpsIntegration() {
    return this.playerWarpsIntegration;
  }

  private void loadIntegrations() {
    this.essentialsWarpsIntegration = tryLoadEssentialsWarpsIntegration();
    this.playerWarpsIntegration = tryLoadPlayerWarpsIntegration();
  }

  private @Nullable IEssentialsWarpsIntegration tryLoadEssentialsWarpsIntegration() {
    if (!config.rootSection.essentialsWarpsIntegration.enabled)
      return null;

    if (!Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
      logger.warning("Essentials-Warps-Integration is enabled, but the corresponding plugin could not be located!");
      return null;
    }

    try {
      var essentialsWarpsIntegration = new EssentialsWarpsIntegration(logger, scheduler);
      Bukkit.getPluginManager().registerEvents(essentialsWarpsIntegration, plugin);
      logger.info("Successfully loaded the Essentials-Warps-integration!");
      return essentialsWarpsIntegration;
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "Could not load Essentials-Warps-integration!", e);
    }

    return null;
  }

  private @Nullable IPlayerWarpsIntegration tryLoadPlayerWarpsIntegration() {
    IPlayerWarpsIntegration integration;

    if (!config.rootSection.playerWarpsIntegration.enabled)
      return null;

    if (!Bukkit.getPluginManager().isPluginEnabled("PlayerWarps")) {
      logger.warning("PlayerWarps-Integration is enabled, but the corresponding plugin could not be located!");
      return null;
    }

    try {
      integration = new OlzieDevPlayerWarpsIntegration(logger, scheduler);
      Bukkit.getPluginManager().registerEvents(integration, plugin);
      logger.info("Successfully loaded the com.olziedev PlayerWarps-integration!");
      return integration;
    } catch (Throwable firstError) {
      try {
        integration = new RevivaloPlayerWarpsIntegration(logger, scheduler, config);
        logger.info("Successfully loaded the dev.revivalo PlayerWarps-integration!");
        return integration;
      } catch (Throwable secondError) {
        logger.log(Level.SEVERE, "Could not load the dev.revivalo PlayerWarps-integration!", secondError);
      }

      logger.log(Level.SEVERE, "Could not load the com.olziedev PlayerWarps-integration!", firstError);
    }

    return null;
  }
}
