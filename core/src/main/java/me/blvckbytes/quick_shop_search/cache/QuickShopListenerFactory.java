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

    if (compareVersions(quickShopVersion, new int[] { 6, 2, 0, 7 }) <= 0) {
      try {
        // On dev-builds, they like to make breaking changes, but not bump the version-string, :)
        // Thus, ensure that it's actually a version prior to 6.2.0.7, by trying to access an event-class
        Class.forName("com.ghostchu.quickshop.api.event.ShopInventoryCalculateEvent");

        logger.info("Loaded listener-support for <= " + PLUGIN_NAME + " 6.2.0.7");
        return new QuickShopListener_LTE_6207(consumer);
      }

      // Fall through to loading the version which made the breaking changes
      catch (ClassNotFoundException ignored) {
        logger.info("Detected dev-version of 6.2.0.8, which introduced breaking-changes to events");
      }
    }

    logger.info("Loaded listener-support for > " + PLUGIN_NAME + " 6.2.0.7");
    return new QuickShopListener_GT_6207(consumer);
  }

  private static int compareVersions(int[] a, int[] b) {
    if (a.length != b.length)
      throw new IllegalStateException("Tried to compare versions of different lengths: " + a.length + " and " + b.length);

    for (var i = 0; i < a.length; ++i) {
      var aPart = a[i];
      var bPart = b[i];

      if (aPart == bPart)
        continue;

      return Integer.compare(aPart, bPart);
    }

    return 0;
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
