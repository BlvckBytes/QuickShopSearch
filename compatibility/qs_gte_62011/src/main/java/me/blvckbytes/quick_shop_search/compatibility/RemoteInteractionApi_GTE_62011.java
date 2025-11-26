package me.blvckbytes.quick_shop_search.compatibility;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopAction;
import com.ghostchu.quickshop.obj.QUserImpl;
import com.ghostchu.quickshop.shop.SimpleInfo;
import com.ghostchu.quickshop.shop.inventory.BukkitInventoryWrapper;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

public class RemoteInteractionApi_GTE_62011 implements RemoteInteractionApi {

  @Override
  public void interact(Player player, Shop shop, int amount) {
    var tradeInfo = new SimpleInfo(
      shop.getLocation(),
      shop.isBuying() ? ShopAction.PURCHASE_SELL : ShopAction.PURCHASE_BUY,
      null, null, shop, false
    );

    var wrappedInventory = new BukkitInventoryWrapper(player.getInventory());

    if (shop.isBuying()) {
      QuickShopAPI.getInstance().getShopManager().actionBuying(
        player, wrappedInventory,
        QuickShop.getInstance().getEconomyManager().provider(),
        tradeInfo, shop, amount
      );
    } else {
      QuickShopAPI.getInstance().getShopManager().actionSelling(
        player, wrappedInventory,
        QuickShop.getInstance().getEconomyManager().provider(),
        tradeInfo, shop, amount
      );
    }
  }

  @Override
  public double getPlayerBalance(Player player, Shop shop) {
    var shopLocation = shop.getLocation();
    var shopWorld = shopLocation.getWorld();

    if (shopWorld == null)
      return 0;

    return QuickShop.getInstance().getEconomyManager().provider()
      .balance(QUserImpl.createFullFilled(player), shopWorld.getName(), shop.getCurrency()).doubleValue();
  }

  @Override
  public double getOwnerBalance(Shop shop) {
    var shopLocation = shop.getLocation();
    var shopWorld = shopLocation.getWorld();

    if (shopWorld == null)
      return 0;

    return QuickShop.getInstance().getEconomyManager().provider()
      .balance(shop.getOwner(), shopWorld.getName(), shop.getCurrency()).doubleValue();
  }

  @Override
  public boolean withdrawAmount(Player player, Shop shop, double amount) {
    var shopLocation = shop.getLocation();
    var shopWorld = shopLocation.getWorld();

    if (shopWorld == null)
      return false;

    return QuickShop.getInstance().getEconomyManager().provider()
      .withdraw(QUserImpl.createFullFilled(player), shopWorld.getName(), shop.getCurrency(), BigDecimal.valueOf(amount));
  }

  @Override
  public boolean depositAmount(Player player, Shop shop, double amount) {
    var shopLocation = shop.getLocation();
    var shopWorld = shopLocation.getWorld();

    if (shopWorld == null)
      return false;

    return QuickShop.getInstance().getEconomyManager().provider()
      .deposit(QUserImpl.createFullFilled(player), shopWorld.getName(), shop.getCurrency(), BigDecimal.valueOf(amount));
  }
}
