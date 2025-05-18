package me.blvckbytes.quick_shop_search.compatibility;

import com.ghostchu.quickshop.api.shop.Shop;
import org.bukkit.entity.Player;

public interface RemoteInteractionApi {

  /**
   * Interact with a shop remotely, dispatching the same action as is completed when
   * trading manually, at the shop's physical location. Whether items are bought or
   * sold is automatically determined by the Shop's type and thus not a user-choice.
   */
  void interact(Player player, Shop shop, int amount);

  /**
   * Get the balance of a player within the shop's world, using the shop's currency
   */
  double getPlayerBalance(Player player, Shop shop);

  /**
   * Get the balance of the shop-owning player, within the shop's world, using the shop's currency
   */
  double getOwnerBalance(Shop shop);

  boolean withdrawAmount(Player player, Shop shop, double amount);

  boolean depositAmount(Player player, Shop shop, double amount);

}
