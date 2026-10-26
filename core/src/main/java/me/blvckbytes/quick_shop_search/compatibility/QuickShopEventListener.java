package me.blvckbytes.quick_shop_search.compatibility;

import com.ghostchu.quickshop.api.event.Phase;
import com.ghostchu.quickshop.api.event.economy.ShopSuccessPurchaseEvent;
import com.ghostchu.quickshop.api.event.general.ShopSignUpdateEvent;
import com.ghostchu.quickshop.api.event.inventory.ShopInventoryCalculateEvent;
import com.ghostchu.quickshop.api.event.management.ShopCreateEvent;
import com.ghostchu.quickshop.api.event.management.ShopDeleteEvent;
import com.ghostchu.quickshop.api.event.settings.type.*;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class QuickShopEventListener implements Listener {

  private final QuickShopEventConsumer consumer;

  public QuickShopEventListener(QuickShopEventConsumer consumer) {
    this.consumer = consumer;
  }

  @EventHandler
  public void onPurchaseSuccess(ShopSuccessPurchaseEvent event) {
    var supportedShop = CachedShop.castToTypedIfSupportedOrNull(event.getShop());

    if (supportedShop == null)
      return;

    consumer.onPurchaseSuccess(supportedShop, event.getAmount(), event.getPurchaser().getUniqueId());
  }

  @EventHandler
  public void onShopCreate(ShopCreateEvent event) {
    if (event.isCancelled() || event.phase() != Phase.POST)
      return;

    var supportedShop = CachedShop.castToTypedIfSupportedOrNull(event.shop().orElse(null));

    if (supportedShop == null)
      return;

    consumer.onShopCreate(supportedShop);
  }

  @EventHandler
  public void onShopDelete(ShopDeleteEvent event) {
    if (event.isCancelled() || event.phase() != Phase.POST)
      return;

    var supportedShop = CachedShop.castToTypedIfSupportedOrNull(event.shop().orElse(null));

    if (supportedShop == null)
      return;

    consumer.onShopDelete(supportedShop);
  }

  @EventHandler
  public void onShopItemChange(ShopItemEvent event) {
    if (event.isCancelled() || event.phase() != Phase.POST)
      return;

    var supportedShop = CachedShop.castToTypedIfSupportedOrNull(event.shop());

    if (supportedShop == null)
      return;

    consumer.onShopItemChange(supportedShop, event.updated());
  }

  @EventHandler
  public void onShopOwnerChange(ShopOwnerEvent event) {
    if (event.isCancelled() || event.phase() != Phase.POST)
      return;

    var supportedShop = CachedShop.castToTypedIfSupportedOrNull(event.shop());

    if (supportedShop == null)
      return;

    consumer.onShopOwnerChange(supportedShop);
  }

  @EventHandler
  public void onShopSignUpdate(ShopSignUpdateEvent event) {
    var supportedShop = CachedShop.castToTypedIfSupportedOrNull(event.getShop());

    if (supportedShop == null)
      return;

    consumer.onShopSignUpdate(supportedShop);
  }

  @EventHandler
  public void onShopInventoryCalculate(ShopInventoryCalculateEvent event) {
    var supportedShop = CachedShop.castToTypedIfSupportedOrNull(event.getShop());

    if (supportedShop == null)
      return;

    consumer.onShopInventoryCalculate(supportedShop, event.getStock(), event.getSpace());
  }

  @EventHandler
  public void onShopNameChange(ShopNameEvent event) {
    if (event.isCancelled() || event.phase() != Phase.POST)
      return;

    var supportedShop = CachedShop.castToTypedIfSupportedOrNull(event.shop());

    if (supportedShop == null)
      return;

    consumer.onShopNameChange(supportedShop);
  }

  @EventHandler
  public void onShopPriceChange(ShopPriceEvent event) {
    if (event.isCancelled() || event.phase() != Phase.POST)
      return;

    var supportedShop = CachedShop.castToTypedIfSupportedOrNull(event.shop());

    if (supportedShop == null)
      return;

    consumer.onShopPriceChange(supportedShop);
  }

  @EventHandler
  public void onShopTypeChange(ShopTypeEnhancedEvent event) {
    if (event.isCancelled() || event.phase() != Phase.POST)
      return;

    var supportedShop = CachedShop.castToTypedIfSupportedOrNull(event.shop());

    if (supportedShop == null)
      return;

    consumer.onShopTypeChange(supportedShop);
  }

  @EventHandler
  public void onShopUnlimitedChange(ShopUnlimitedEvent event) {
    if (event.isCancelled() || event.phase() != Phase.POST)
      return;

    var supportedShop = CachedShop.castToTypedIfSupportedOrNull(event.shop());

    if (supportedShop == null)
      return;

    consumer.onShopTypeChange(supportedShop);
  }
}
