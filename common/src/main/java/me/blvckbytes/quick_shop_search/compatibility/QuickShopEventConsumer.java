package me.blvckbytes.quick_shop_search.compatibility;

import com.ghostchu.quickshop.api.shop.Shop;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public interface QuickShopEventConsumer {

  void onPurchaseSuccess(Shop shop, int amount, UUID purchaserId);

  void onShopCreate(Shop shop);

  void onShopDelete(Shop shop);

  void onShopItemChange(Shop shop, ItemStack newItem);

  void onShopOwnerChange(Shop shop);

  void onShopSignUpdate(Shop shop);

  void onShopInventoryCalculate(Shop shop, int stock, int space);

  void onShopNameChange(Shop shop);

  void onShopPriceChange(Shop shop);

  void onShopTypeChange(Shop shop);

}
