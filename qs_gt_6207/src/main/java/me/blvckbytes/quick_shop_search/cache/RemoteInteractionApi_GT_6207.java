package me.blvckbytes.quick_shop_search.cache;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopAction;
import com.ghostchu.quickshop.shop.SimpleInfo;
import com.ghostchu.quickshop.shop.inventory.BukkitInventoryWrapper;
import org.bukkit.entity.Player;

public class RemoteInteractionApi_GT_6207 implements RemoteInteractionApi {

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
        QuickShop.getInstance().getEconomy(),
        tradeInfo, shop, amount
      );
    } else {
      QuickShopAPI.getInstance().getShopManager().actionSelling(
        player, wrappedInventory,
        QuickShop.getInstance().getEconomy(),
        tradeInfo, shop, amount
      );
    }
  }
}
