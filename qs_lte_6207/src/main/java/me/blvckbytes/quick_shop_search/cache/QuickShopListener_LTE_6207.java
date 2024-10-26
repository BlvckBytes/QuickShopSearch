package me.blvckbytes.quick_shop_search.cache;

import com.ghostchu.quickshop.api.event.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class QuickShopListener_LTE_6207 implements Listener {

  private final QuickShopEventConsumer consumer;

  public QuickShopListener_LTE_6207(QuickShopEventConsumer consumer) {
    this.consumer = consumer;
  }

  @EventHandler
  public void onShopCreate(ShopCreateSuccessEvent event) {
    consumer.onShopCreate(event.getShop());
  }

  @EventHandler
  public void onShopDelete(ShopDeleteEvent event) {
    if (event.isCancelled())
      return;

    consumer.onShopDelete(event.getShop());
  }

  @EventHandler
  public void onShopItemChange(ShopItemChangeEvent event) {
    if (event.isCancelled())
      return;

    consumer.onShopItemChange(event.getShop(), event.getNewItem());
  }

  @EventHandler
  public void onShopOwnerChange(ShopOwnershipTransferEvent event) {
    if (event.isCancelled())
      return;

    consumer.onShopOwnerChange(event.getShop());
  }

  @EventHandler
  public void onShopSignUpdate(ShopSignUpdateEvent event) {
    consumer.onShopSignUpdate(event.getShop());
  }

  @EventHandler
  public void onShopInventoryCalculate(ShopInventoryCalculateEvent event) {
    consumer.onShopInventoryCalculate(event.getShop(), event.getStock(), event.getSpace());
  }

  @EventHandler
  public void onShopNameChange(ShopNamingEvent event) {
    if (event.isCancelled())
      return;

    consumer.onShopNameChange(event.getShop());
  }

  @EventHandler
  public void onShopPriceChange(ShopPriceChangeEvent event) {
    if (event.isCancelled())
      return;

    consumer.onShopPriceChange(event.getShop());
  }

  @EventHandler
  public void onShopTypeChange(ShopTypeChangeEvent event) {
    if (event.isCancelled())
      return;

    consumer.onShopTypeChange(event.getShop());
  }
}
