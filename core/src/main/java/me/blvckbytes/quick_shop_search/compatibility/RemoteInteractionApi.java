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
import java.util.logging.Logger;

public class RemoteInteractionApi {

  private final Logger logger;

  public RemoteInteractionApi(Logger logger) {
    this.logger = logger;
  }

  /**
   * Interact with a shop remotely, dispatching the same action as is completed when
   * trading manually, at the shop's physical location. Whether items are bought or
   * sold is automatically determined by the Shop's type and thus not a user-choice.
   */
  public void interact(Player player, Shop shop, int amount) {
    var economy = QuickShop.getInstance().getEconomyManager().provider();

    if (economy == null) {
      logger.severe("Economy-Provider of QuickShop-Hikari was null - cancelled remote interaction issued by " + player.getName() + "!");
      return;
    }

    var tradeInfo = new SimpleInfo(
      shop.getLocation(),
      shop.isBuying() ? ShopAction.PURCHASE_SELL : ShopAction.PURCHASE_BUY,
      null, null, shop, false
    );

    var wrappedInventory = new BukkitInventoryWrapper(player.getInventory());

    if (shop.isBuying()) {
      QuickShopAPI.getInstance().getShopManager().actionBuying(
        player, wrappedInventory,
        economy,
        tradeInfo, shop, amount
      );
    } else {
      QuickShopAPI.getInstance().getShopManager().actionSelling(
        player, wrappedInventory,
        economy,
        tradeInfo, shop, amount
      );
    }
  }

  /**
   * Get the balance of a player within the shop's world, using the shop's currency
   */
  public double getPlayerBalance(Player player, Shop shop) {
    var shopLocation = shop.getLocation();
    var shopWorld = shopLocation.getWorld();

    if (shopWorld == null)
      return 0;

    var economy = QuickShop.getInstance().getEconomyManager().provider();

    if (economy == null)
      return 0;

    return economy.balance(QUserImpl.createFullFilled(player), shopWorld.getName(), shop.getCurrency()).doubleValue();
  }

  /**
   * Get the balance of the shop-owning player, within the shop's world, using the shop's currency
   */
  public double getOwnerBalance(Shop shop) {
    var shopLocation = shop.getLocation();
    var shopWorld = shopLocation.getWorld();

    if (shopWorld == null)
      return 0;

    var economy = QuickShop.getInstance().getEconomyManager().provider();

    if (economy == null)
      return 0;

    return economy.balance(shop.getOwner(), shopWorld.getName(), shop.getCurrency()).doubleValue();
  }

  public boolean withdrawAmount(Player player, Shop shop, double amount) {
    var shopLocation = shop.getLocation();
    var shopWorld = shopLocation.getWorld();

    if (shopWorld == null)
      return false;

    var economy = QuickShop.getInstance().getEconomyManager().provider();

    if (economy == null) {
      logger.severe("Economy-Provider of QuickShop-Hikari was null - cancelled withdrawing from " + player.getName() + "'s account!");
      return false;
    }

    return economy.withdraw(QUserImpl.createFullFilled(player), shopWorld.getName(), shop.getCurrency(), BigDecimal.valueOf(amount));
  }

  public boolean depositAmount(Player player, Shop shop, double amount) {
    var shopLocation = shop.getLocation();
    var shopWorld = shopLocation.getWorld();

    if (shopWorld == null)
      return false;

    var economy = QuickShop.getInstance().getEconomyManager().provider();

    if (economy == null) {
      logger.severe("Economy-Provider of QuickShop-Hikari was null - cancelled depositing on " + player.getName() + "'s account!");
      return false;
    }

    return economy
      .deposit(QUserImpl.createFullFilled(player), shopWorld.getName(), shop.getCurrency(), BigDecimal.valueOf(amount));
  }
}
