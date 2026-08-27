package me.blvckbytes.quick_shop_search.compatibility;

import com.ghostchu.quickshop.api.shop.Shop;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public interface QuickShopEventConsumer {

  void onPurchaseSuccess(Shop<Double, Location> shop, int amount, UUID purchaserId);

  void onShopCreate(Shop<Double, Location> shop);

  void onShopDelete(Shop<Double, Location> shop);

  void onShopItemChange(Shop<Double, Location> shop, ItemStack newItem);

  void onShopOwnerChange(Shop<Double, Location> shop);

  void onShopSignUpdate(Shop<Double, Location> shop);

  void onShopInventoryCalculate(Shop<Double, Location> shop, int stock, int space);

  void onShopNameChange(Shop<Double, Location> shop);

  void onShopPriceChange(Shop<Double, Location> shop);

  void onShopTypeChange(Shop<Double, Location> shop);

}
