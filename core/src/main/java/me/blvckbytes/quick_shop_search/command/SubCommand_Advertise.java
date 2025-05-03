package me.blvckbytes.quick_shop_search.command;

import com.ghostchu.quickshop.api.command.CommandHandler;
import com.ghostchu.quickshop.api.command.CommandParser;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.cache.CachedShopRegistry;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SubCommand_Advertise implements CommandHandler<Player> {

  private final CachedShopRegistry shopRegistry;
  private final ConfigKeeper<MainSection> config;

  public SubCommand_Advertise(CachedShopRegistry shopRegistry, ConfigKeeper<MainSection> config) {
    this.shopRegistry = shopRegistry;
    this.config = config;
  }

  @Override
  public void onCommand(Player player, @NotNull String commandLabel, @NotNull CommandParser parser) {
    BukkitEvaluable message;

    if (!PluginPermission.ADVERTISE_COMMAND.has(player)) {
      if ((message = config.rootSection.playerMessages.missingPermissionAdvertiseCommand) != null)
        message.sendMessage(player, config.rootSection.builtBaseEnvironment);

      return;
    }

    var targetShop = getLookedAtShop(player);

    if (targetShop == null) {
      if ((message = config.rootSection.playerMessages.commandAdvertiseNotLookingAtShop) != null)
        message.sendMessage(player, config.rootSection.builtBaseEnvironment);

      return;
    }

    var shopOwner = targetShop.handle.getOwner().getBukkitPlayer().orElse(null);
    var isShopOwner = shopOwner != null && shopOwner == player;

    if (!PluginPermission.ADVERTISE_COMMAND_OWNER_BYPASS.has(player) && !isShopOwner) {
      if ((message = config.rootSection.playerMessages.commandAdvertiseNotTheOwner) != null)
        message.sendMessage(player, config.rootSection.builtBaseEnvironment);

      return;
    }

    var toggleResult = targetShop.toggleAdvertising();

    switch (toggleResult) {
      case NOW_ON: {
        if (isShopOwner)
          message = config.rootSection.playerMessages.commandAdvertiseEnabledSelf;
        else
          message = config.rootSection.playerMessages.commandAdvertiseEnabledOther;
        break;
      }

      case NOW_OFF: {
        if (isShopOwner)
          message = config.rootSection.playerMessages.commandAdvertiseDisabledSelf;
        else
          message = config.rootSection.playerMessages.commandAdvertiseDisabledOther;
        break;
      }

      default: {
        message = config.rootSection.playerMessages.commandAdvertiseToggleError;
        break;
      }
    }

    if (message != null)
      message.sendMessage(player, targetShop.getShopEnvironment().build());
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull Player sender, @NotNull String commandLabel, @NotNull CommandParser parser) {
    return List.of();
  }

  private @Nullable CachedShop getLookedAtShop(Player player) {
    var blockIterator = new BlockIterator(player, 10);

    while(blockIterator.hasNext()) {
      var block = blockIterator.next();
      var shop = shopRegistry.findByLocation(block.getLocation());

      if(shop == null)
        continue;

      return shop;
    }

    return null;
  }
}
