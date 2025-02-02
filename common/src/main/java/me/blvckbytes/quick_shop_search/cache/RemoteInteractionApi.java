package me.blvckbytes.quick_shop_search.cache;

import com.ghostchu.quickshop.api.shop.Shop;
import org.bukkit.entity.Player;

public interface RemoteInteractionApi {

  /**
   * Interact with a shop remotely, dispatching the same action as is completed when
   * trading manually, at the shop's physical location. Whether items are bought or
   * sold is automatically determined by the Shop's type and thus not a user-choice.
   */
  void interact(Player player, Shop shop, int amount);

}
