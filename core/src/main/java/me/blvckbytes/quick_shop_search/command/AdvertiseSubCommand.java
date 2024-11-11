package me.blvckbytes.quick_shop_search.command;

import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.cache.CachedShopRegistry;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AdvertiseSubCommand extends SubCommand {

  private final CachedShopRegistry shopRegistry;
  private final ConfigKeeper<MainSection> config;

  public AdvertiseSubCommand(CachedShopRegistry shopRegistry, ConfigKeeper<MainSection> config) {
    super("advertise", PluginPermission.SUB_COMMAND_ADVERTISE.node);

    this.shopRegistry = shopRegistry;
    this.config = config;
  }

  // /qss advertise <on|off|unset>

  @Override
  public ExitCode onCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player))
      return ExitCode.PLAYER_ONLY;

    if (args.length != 1)
      return ExitCode.MALFORMED_USAGE;

    var mode = parseEnumConstant(AdvertiseMode.class, args[0]);

    if (mode == null)
      return ExitCode.MALFORMED_USAGE;

    var targetShop = getLookedAtShop(player);

    if (targetShop == null) {
      config.rootSection.playerMessages.commandAdvertiseNotLookingAtShop.sendMessage(
        player, config.rootSection.builtBaseEnvironment
      );

      return ExitCode.SUCCESS;
    }

    var shopOwner = targetShop.handle.getOwner().getBukkitPlayer().orElse(null);
    var isShopOwner = shopOwner != null && shopOwner == player;

    if (!PluginPermission.SUB_COMMAND_ADVERTISE_OWNER_BYPASS.has(player) && !isShopOwner) {
      config.rootSection.playerMessages.commandAdvertiseNotTheOwner.sendMessage(
        player, config.rootSection.builtBaseEnvironment
      );

      return ExitCode.SUCCESS;
    }

    var toggleResult = targetShop.setAdvertising(switch (mode) {
      case ON -> true;
      case OFF -> false;
      case UNSET -> null;
    });

    BukkitEvaluable message;

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

      case NOW_UNSET: {
        if (isShopOwner)
          message = config.rootSection.playerMessages.commandAdvertiseUnsetSelf;
        else
          message = config.rootSection.playerMessages.commandAdvertiseUnsetOther;
        break;
      }

      default: {
        message = config.rootSection.playerMessages.commandAdvertiseInternalError;
        break;
      }
    }

    message.sendMessage(player, targetShop.getShopEnvironment().build());

    return ExitCode.SUCCESS;
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

  @Override
  public List<String> onTabComplete(CommandSender sender, String[] args) {
    if (args.length == 1)
      return suggestEnumConstants(AdvertiseMode.class, args[0]);

    return List.of();
  }

  @Override
  public String getUsage(CommandSender sender) {
    return config.rootSection.playerMessages.commandAdvertiseUsage.asScalar(
      ScalarType.STRING,
      config.rootSection.getBaseEnvironment()
        .withStaticVariable("sub_label", label)
        .withStaticVariable("advertise_mode_names", suggestEnumConstants(AdvertiseMode.class, null))
        .build()
    );
  }

  @Override
  public String getDescription(CommandSender sender) {
    return config.rootSection.playerMessages.commandAdvertiseDescription.asScalar(
      ScalarType.STRING, config.rootSection.builtBaseEnvironment
    );
  }
}
