package me.blvckbytes.quick_shop_search.command;

import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.cache.AdvertiseChangeResult;
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

  // /qss advertise [on|off|unset]

  @Override
  public ExitCode onCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player))
      return ExitCode.PLAYER_ONLY;

    var targetShop = getLookedAtShop(player);

    if (targetShop == null) {
      config.rootSection.playerMessages.commandAdvertiseNotLookingAtShop.sendMessage(
        player, config.rootSection.builtBaseEnvironment
      );

      return ExitCode.SUCCESS;
    }

    var isShopOwner = isShopOwnerOf(player, targetShop);

    if (!PluginPermission.SUB_COMMAND_ADVERTISE_OWNER_BYPASS.has(player) && !isShopOwner) {
      config.rootSection.playerMessages.commandAdvertiseNotTheOwner.sendMessage(
        player, config.rootSection.builtBaseEnvironment
      );

      return ExitCode.SUCCESS;
    }

    var previousMode = getCurrentAdvertiseMode(targetShop);
    BukkitEvaluable message;

    if (args.length == 0) {
      if (isShopOwner)
        message = config.rootSection.playerMessages.commandAdvertiseReadSelf;
      else
        message = config.rootSection.playerMessages.commandAdvertiseReadOther;

      message.sendMessage(
        sender,
        targetShop.getShopEnvironment()
          .withStaticVariable("current_mode", getEnumName(previousMode))
          .build(config.rootSection.builtBaseEnvironment)
      );

      return ExitCode.SUCCESS;
    }

    if (args.length != 1)
      return ExitCode.MALFORMED_USAGE;

    var mode = parseEnumConstant(AdvertiseMode.class, args[0]);

    if (mode == null)
      return ExitCode.MALFORMED_USAGE;

    alterAdvertisingMode(sender, targetShop, mode, isShopOwner, previousMode);

    return ExitCode.SUCCESS;
  }

  /**
   * @return Whether any actual changes have been made to this shop's advertising-state
   */
  public boolean alterAdvertisingMode(CommandSender sender, CachedShop targetShop, AdvertiseMode mode) {
    return alterAdvertisingMode(sender, targetShop, mode, isShopOwnerOf(sender, targetShop), getCurrentAdvertiseMode(targetShop));
  }

  private AdvertiseMode getCurrentAdvertiseMode(CachedShop targetShop) {
    AdvertiseMode currentMode;

    if (!targetShop.isAdvertisingSet())
      currentMode = AdvertiseMode.UNSET;
    else
      currentMode = targetShop.isAdvertising() ? AdvertiseMode.ON : AdvertiseMode.OFF;

    return currentMode;
  }

  private boolean isShopOwnerOf(CommandSender sender, CachedShop targetShop) {
    var shopOwner = targetShop.handle.getOwner().getBukkitPlayer().orElse(null);
    return shopOwner != null && shopOwner == sender;
  }

  private boolean alterAdvertisingMode(CommandSender sender, CachedShop targetShop, AdvertiseMode mode, boolean isShopOwner, AdvertiseMode previousMode) {
    BukkitEvaluable message;

    if (previousMode == mode) {
      if (isShopOwner)
        message = config.rootSection.playerMessages.commandAdvertiseUnchangedSelf;
      else
        message = config.rootSection.playerMessages.commandAdvertiseUnchangedOther;

      message.sendMessage(
        sender,
        targetShop.getShopEnvironment()
          .withStaticVariable("current_mode", getEnumName(previousMode))
          .build(config.rootSection.builtBaseEnvironment)
      );

      return false;
    }

    var toggleResult = targetShop.setAdvertising(switch (mode) {
      case ON -> true;
      case OFF -> false;
      case UNSET -> null;
    });

    if (toggleResult == AdvertiseChangeResult.ERROR) {
      config.rootSection.playerMessages.commandAdvertiseInternalError.sendMessage(
        sender,
        targetShop.getShopEnvironment()
          .withStaticVariable("current_mode", getEnumName(previousMode))
          .build(config.rootSection.builtBaseEnvironment)
      );

      return false;
    }

    if (isShopOwner)
      message = config.rootSection.playerMessages.commandAdvertiseChangedSelf;
    else
      message = config.rootSection.playerMessages.commandAdvertiseChangedOther;

    var currentMode = switch (toggleResult) {
      case NOW_ON -> AdvertiseMode.ON;
      case NOW_OFF -> AdvertiseMode.OFF;
      default -> AdvertiseMode.UNSET;
    };

    message.sendMessage(
      sender,
      targetShop.getShopEnvironment()
        .withStaticVariable("previous_mode", getEnumName(previousMode))
        .withStaticVariable("current_mode", getEnumName(currentMode))
        .build(config.rootSection.builtBaseEnvironment)
    );

    return true;
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
