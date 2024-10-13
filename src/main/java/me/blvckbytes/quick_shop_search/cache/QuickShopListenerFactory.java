package me.blvckbytes.quick_shop_search.cache;

import org.bukkit.event.Listener;

public class QuickShopListenerFactory {

  // TODO: There will be breaking Event-API-changes soon; create sub-modules linking against these
  //       differing versions and load the class conditionally, based on QuickShop's version

  public Listener create(QuickShopEventConsumer consumer) {
    return new QuickShopListener(consumer);
  }
}
