package me.blvckbytes.quick_shop_search.cache;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopAction;
import com.ghostchu.quickshop.api.shop.ShopManager;
import com.ghostchu.quickshop.obj.QUserImpl;
import com.ghostchu.quickshop.shop.SimpleInfo;
import com.ghostchu.quickshop.shop.inventory.BukkitInventoryWrapper;
import org.bukkit.entity.Player;

import java.lang.reflect.Modifier;

public class RemoteInteractionApi_LTE_6207 implements RemoteInteractionApi {

  private final ShopManager shopManager;

  public RemoteInteractionApi_LTE_6207() {
    this.shopManager = QuickShopAPI.getInstance().getShopManager();

    for (var method : shopManager.getClass().getDeclaredMethods()) {
      if (Modifier.isStatic(method.getModifiers()))
        continue;

      if (!Modifier.isPublic(method.getModifiers()))
        continue;

      if (method.getName().equals("actionBuying") && method.getReturnType() != Void.class)
        throw new IllegalStateException("Expected actionBuying to have no return-value");

      if (method.getName().equals("actionSelling") && method.getReturnType() != Void.class)
        throw new IllegalStateException("Expected actionSelling to have no return-value");
    }
  }

  @Override
  public void interact(Player player, Shop shop, int amount) {
    var tradeInfo = new SimpleInfo(
      shop.getLocation(),
      shop.isBuying() ? ShopAction.PURCHASE_SELL : ShopAction.PURCHASE_BUY,
      null, null, shop, false
    );

    var wrappedInventory = new BukkitInventoryWrapper(player.getInventory());

    if (shop.isBuying()) {
      shopManager.actionBuying(
        player, wrappedInventory,
        QuickShop.getInstance().getEconomy(),
        tradeInfo, shop, amount
      );
    } else {
      shopManager.actionSelling(
        player, wrappedInventory,
        QuickShop.getInstance().getEconomy(),
        tradeInfo, shop, amount
      );
    }
  }

  @Override
  public double getPlayerBalance(Player player, Shop shop) {
    return QuickShop.getInstance().getEconomy()
      .getBalance(QUserImpl.createFullFilled(player), shop.getLocation().getWorld(), shop.getCurrency());
  }

  @Override
  public double getOwnerBalance(Shop shop) {
    return QuickShop.getInstance().getEconomy()
      .getBalance(shop.getOwner(), shop.getLocation().getWorld(), shop.getCurrency());
  }
}
