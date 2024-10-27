package me.blvckbytes.quick_shop_search.cache;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class QuickShopListenerFactory {

  private static final String PLUGIN_NAME = "QuickShop-Hikari";

  public static Listener create(Logger logger, QuickShopEventConsumer consumer) {
    var quickShopReference = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);

    if (!(quickShopReference instanceof JavaPlugin quickShopPlugin))
      throw new IllegalStateException("Could not get a reference to " + PLUGIN_NAME);

    var quickShopVersionString = quickShopPlugin.getDescription().getVersion();
    var quickShopVersion = parseVersionString(quickShopVersionString);

    logger.info("Detected " + PLUGIN_NAME + " version " + quickShopVersionString);

    if (quickShopVersion.length != 4)
      throw new IllegalStateException("Expected " + PLUGIN_NAME + "'s version to be comprised of four parts");

    if (
      quickShopVersion[0] >= 6 &&
      quickShopVersion[1] >= 2 &&
      quickShopVersion[2] >= 0 &&
      quickShopVersion[3] >  7
    ) {
      logger.info("Loaded listener-support for > " + PLUGIN_NAME + " 6.2.0.7");
      return new QuickShopListener_GT_6207(consumer);
    }

    logger.info("Loaded listener-support for <= " + PLUGIN_NAME + " 6.2.0.7");
    return new QuickShopListener_LTE_6207(consumer);
  }

  private static int[] parseVersionString(String versionString) {
    var versionParts = versionString.split("\\.");
    var versionNumbers = new int[versionParts.length];

    for (var i = 0; i < versionParts.length; ++i) {
      try {
        versionNumbers[i] = Integer.parseInt(versionParts[i]);
      } catch (NumberFormatException e) {
        throw new IllegalStateException("Could not parse " + (i + 1) + "-th version-part of version " + versionString, e);
      }
    }

    return versionNumbers;
  }
}
