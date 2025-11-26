package me.blvckbytes.quick_shop_search.cache;

import me.blvckbytes.quick_shop_search.compatibility.*;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class QuickShopVersionDependentFactory {

  private static final String PLUGIN_NAME = "QuickShop-Hikari";
  private static final int[]
    VER_6207 = new int[] { 6, 2, 0, 7 },
    VER_6208 = new int[] { 6, 2, 0, 8 },
    VER_62010 = new int[] { 6, 2, 0, 10 }
    ;

  private final Logger logger;
  private final int[] quickShopVersion;

  public QuickShopVersionDependentFactory(Logger logger) {
    this.logger = logger;

    var quickShopReference = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);

    if (!(quickShopReference instanceof JavaPlugin quickShopPlugin))
      throw new IllegalStateException("Could not get a reference to " + PLUGIN_NAME);

    var quickShopVersionString = quickShopPlugin.getDescription().getVersion();
    quickShopVersion = parseVersionString(quickShopVersionString);

    logger.info("Detected " + PLUGIN_NAME + " version " + quickShopVersionString);

    if (quickShopVersion.length != 4)
      throw new IllegalStateException("Expected " + PLUGIN_NAME + "'s version to be comprised of four parts");
  }

  public Listener createListener(QuickShopEventConsumer consumer) {
    if (compareVersions(quickShopVersion, VER_6207) <= 0) {
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

    if (compareVersions(quickShopVersion, VER_6208) <= 0) {
      logger.info("Loaded listener-support for <= " + PLUGIN_NAME + " 6.2.0.8 and > 6.2.0.7");
      return new QuickShopListener_GT_6207_LTE_6208(consumer);
    }

    if (compareVersions(quickShopVersion, VER_62010) <= 0) {
      logger.info("Loaded listener-support for <= " + PLUGIN_NAME + " 6.2.0.10 and > 6.2.0.8");
      return new QuickShopListener_GT_6208_LTE_62010(consumer);
    }

    logger.info("Loaded listener-support for >= " + PLUGIN_NAME + " 6.2.0.11");
    return new QuickShopListener_GTE_62011(consumer);
  }

  public RemoteInteractionApi createInteractionApi() {
    if (compareVersions(quickShopVersion, VER_6207) <= 0) {
      try {
        var result = new RemoteInteractionApi_LTE_6207();
        logger.info("Loaded remote-interaction-support for <= " + PLUGIN_NAME + " 6.2.0.7");
        return result;
      }

      catch (Exception ignored) {
        logger.info("Detected dev-version of 6.2.0.8, which introduced breaking-changes to remote-interaction");
      }
    }

    if (compareVersions(quickShopVersion, VER_6208) <= 0) {
      logger.info("Loaded remote-interaction-support for <= " + PLUGIN_NAME + " 6.2.0.8 and > 6.2.0.7");
      return new RemoteInteractionApi_GT_6207_LTE_6208();
    }

    if (compareVersions(quickShopVersion, VER_62010) <= 0) {
      logger.info("Loaded remote-interaction-support for <= " + PLUGIN_NAME + " 6.2.0.10 and > 6.2.0.8");
      return new RemoteInteractionApi_GT_6208_LTE_62010();
    }

    logger.info("Loaded remote-interaction-support for >= " + PLUGIN_NAME + " 6.2.0.11");
    return new RemoteInteractionApi_GTE_62011();
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
        var versionPart = versionParts[i];

        // They've decided to append additional information with a dash to the last part
        // Example: "6.2.0.9-RELEASE-1"
        if (i == versionParts.length - 1) {
          var dashIndex = versionPart.indexOf('-');

          if (dashIndex > 0)
            versionPart = versionPart.substring(0, dashIndex);
        }

        versionNumbers[i] = Integer.parseInt(versionPart);
      } catch (NumberFormatException e) {
        throw new IllegalStateException("Could not parse " + (i + 1) + "-th version-part of version " + versionString, e);
      }
    }

    return versionNumbers;
  }
}
