package me.blvckbytes.quick_shop_search.cache;

import com.ghostchu.quickshop.api.event.general.ShopSignUpdateEvent;
import com.ghostchu.quickshop.api.event.inventory.ShopInventoryCalculateEvent;
import com.ghostchu.quickshop.api.event.management.ShopCreateEvent;
import com.ghostchu.quickshop.api.event.management.ShopDeleteEvent;
import com.ghostchu.quickshop.api.event.settings.type.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class QuickShopListener_GT_6208 implements Listener {

  private final QuickShopEventConsumer consumer;

  public QuickShopListener_GT_6208(QuickShopEventConsumer consumer) {
    this.consumer = consumer;
  }

  @EventHandler
  public void onShopCreate(ShopCreateEvent event) {
    event.shop().ifPresent(consumer::onShopCreate);
  }

  @EventHandler
  public void onShopDelete(ShopDeleteEvent event) {
    if (event.isCancelled())
      return;

    event.shop().ifPresent(consumer::onShopDelete);
  }

  @EventHandler
  public void onShopItemChange(ShopItemEvent event) {
    if (event.isCancelled())
      return;

    consumer.onShopItemChange(event.shop(), event.updated());
  }

  @EventHandler
  public void onShopOwnerChange(ShopOwnerEvent event) {
    if (event.isCancelled())
      return;

    consumer.onShopOwnerChange(event.shop());
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
  public void onShopNameChange(ShopNameEvent event) {
    if (event.isCancelled())
      return;

    consumer.onShopNameChange(event.shop());
  }

  @EventHandler
  public void onShopPriceChange(ShopPriceEvent event) {
    if (event.isCancelled())
      return;

    consumer.onShopPriceChange(event.shop());
  }

  @EventHandler
  public void onShopTypeChange(ShopTypeEvent event) {
    if (event.isCancelled())
      return;

    consumer.onShopTypeChange(event.shop());
  }

  @EventHandler
  public void onShopUnlimitedChange(ShopUnlimitedEvent event) {
    if (event.isCancelled())
      return;

    consumer.onShopTypeChange(event.shop());
  }
}
