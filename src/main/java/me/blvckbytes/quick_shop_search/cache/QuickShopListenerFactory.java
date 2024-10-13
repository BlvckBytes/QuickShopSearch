package me.blvckbytes.quick_shop_search.cache;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;

public class QuickShopListenerFactory {

  // TODO: There will be breaking Event-API-changes soon; create sub-modules linking against these
  //       differing versions and load the class conditionally, based on QuickShop's version

  private static final String LISTENER_PATH = "me/blvckbytes/quick_shop_search/cache/QuickShopListener";

  public static Listener create(Logger logger, QuickShopEventConsumer consumer) {
    var quickShopReference = Bukkit.getPluginManager().getPlugin("QuickShop-Hikari");

    if (!(quickShopReference instanceof JavaPlugin quickShopPlugin))
      throw new IllegalStateException("Could not get a reference to QuickShop-Hikari");

    var quickShopVersionString = quickShopPlugin.getDescription().getVersion();
    var quickShopVersion = parseVersionString(quickShopVersionString);

    Constructor<?> listenerConstructor;

    try {
      listenerConstructor = Class.forName(LISTENER_PATH.replace('/', '.')).getConstructor(QuickShopEventConsumer.class);
    } catch (Exception e) {
      throw new IllegalStateException("Could not locate constructor of Listener for QuickShop-Hikari", e);
    }

    try {
      var result = (Listener) listenerConstructor.newInstance(consumer);
      logger.info("Instantiated Listener for QuickShop-Hikari version " + quickShopVersionString);
      return result;
    } catch (Exception e) {
      throw new IllegalStateException("Could not instantiate Listener for QuickShop-Hikari", e);
    }
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
