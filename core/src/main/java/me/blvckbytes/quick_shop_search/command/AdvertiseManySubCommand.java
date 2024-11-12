package me.blvckbytes.quick_shop_search.command;

import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.cache.CachedShopRegistry;
import me.blvckbytes.quick_shop_search.cache.OfflinePlayerCache;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.syllables_matcher.EnumMatcher;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AdvertiseManySubCommand extends SubCommand {

  private enum ManyTarget {
    ALL,
    SET,
    ON,
    OFF,
    UNSET,
    ;

    static final EnumMatcher<ManyTarget> matcher = new EnumMatcher<>(values());
  }

  private final CachedShopRegistry shopRegistry;
  private final OfflinePlayerCache offlinePlayerCache;
  private final AdvertiseSubCommand advertiseSubCommand;
  private final ConfigKeeper<MainSection> config;

  public AdvertiseManySubCommand(
    CachedShopRegistry shopRegistry,
    OfflinePlayerCache offlinePlayerCache,
    AdvertiseSubCommand advertiseSubCommand,
    ConfigKeeper<MainSection> config
  ) {
    super("advertise-many", PluginPermission.SUB_COMMAND_ADVERTISE_MANY.node);

    this.shopRegistry = shopRegistry;
    this.offlinePlayerCache = offlinePlayerCache;
    this.advertiseSubCommand = advertiseSubCommand;
    this.config = config;
  }

  // /qss advertise-many <all|set|unset> <on|off|unset> [player]

  @Override
  public ExitCode onCommand(CommandSender sender, String[] args) {
    if (args.length > 3 || args.length < 2)
      return ExitCode.MALFORMED_USAGE;

    OfflinePlayer shopOwner;

    if (args.length < 3) {
      if (!(sender instanceof Player player))
        return ExitCode.PLAYER_ONLY;

      shopOwner = player;
    } else {
      if (!PluginPermission.SUB_COMMAND_ADVERTISE_MANY_OTHER.has(sender)) {
        config.rootSection.playerMessages.missingPermissionAdvertiseManyOther.sendMessage(
          sender, config.rootSection.builtBaseEnvironment
        );

        return ExitCode.SUCCESS;
      }

      shopOwner = offlinePlayerCache.getByName(args[2]);

      if (shopOwner == null) {
        config.rootSection.playerMessages.commandArgumentUnknownPlayerName.sendMessage(
          sender,
          config.rootSection.getBaseEnvironment()
            .withStaticVariable("name", args[2])
            .build()
        );

        return ExitCode.SUCCESS;
      }
    }

    var normalizedManyTarget = ManyTarget.matcher.matchFirst(args[0]);

    if (normalizedManyTarget == null)
      return ExitCode.MALFORMED_USAGE;

    var normalizedMode = AdvertiseMode.matcher.matchFirst(args[1]);

    if (normalizedMode == null)
      return ExitCode.MALFORMED_USAGE;

    var targetShops = shopRegistry.getShopsWithOwner(shopOwner);

    if (targetShops.isEmpty()) {
      if (shopOwner == sender) {
        config.rootSection.playerMessages.commandAdvertiseManyOwnsNoShopsSelf.sendMessage(
          sender, config.rootSection.builtBaseEnvironment
        );

        return ExitCode.SUCCESS;
      }

      config.rootSection.playerMessages.commandAdvertiseManyOwnsNoShopsOther.sendMessage(
        sender,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("name", shopOwner.getName())
          .build()
      );

      return ExitCode.SUCCESS;
    }

    BukkitEvaluable message;

    var hasHeaderBeenSent = false;
    var numberOfChangedShops = 0;

    for (var targetShop : targetShops) {
      var isShopTargeted = switch (normalizedManyTarget.constant) {
        case ALL -> true;
        case SET -> targetShop.isAdvertisingSet();
        case UNSET -> !targetShop.isAdvertisingSet();
        case ON -> targetShop.isAdvertisingSet() && targetShop.isAdvertising();
        case OFF -> targetShop.isAdvertisingSet() && !targetShop.isAdvertising();
      };

      if (!isShopTargeted)
        continue;

      if (!hasHeaderBeenSent) {
        hasHeaderBeenSent = true;

        if ((message = config.rootSection.playerMessages.commandAdvertiseManyAlteredScreenHeader) != null) {
          message.sendMessage(
            sender,
            config.rootSection.getBaseEnvironment()
              .withStaticVariable("advertise_mode", normalizedMode.normalizedName)
              .build()
          );
        }
      }

      if (advertiseSubCommand.alterAdvertisingMode(sender, targetShop, normalizedMode.constant))
        ++numberOfChangedShops;
    }

    if (!hasHeaderBeenSent) {
      if (shopOwner == sender) {
        config.rootSection.playerMessages.commandAdvertiseManyNoShopsMatchedTargetSelf.sendMessage(
          sender, config.rootSection.getBaseEnvironment()
            .withStaticVariable("target_mode", normalizedManyTarget.normalizedName)
            .build()
        );

        return ExitCode.SUCCESS;
      }

      config.rootSection.playerMessages.commandAdvertiseManyNoShopsMatchedTargetOther.sendMessage(
        sender,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("target_mode", normalizedManyTarget.normalizedName)
          .withStaticVariable("name", shopOwner.getName())
          .build()
      );

      return ExitCode.SUCCESS;
    }

    if ((message = config.rootSection.playerMessages.commandAdvertiseManyAlteredScreenFooter) != null) {
      message.sendMessage(
        sender,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("number_of_changed_shops", numberOfChangedShops)
          .withStaticVariable("advertise_mode", normalizedMode.normalizedName)
          .build()
      );
    }

    return ExitCode.SUCCESS;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, String[] args) {
    if (args.length == 1)
      return ManyTarget.matcher.createCompletions(args[0]);

    if (args.length == 2)
      return AdvertiseMode.matcher.createCompletions(args[1]);

    if (args.length == 3 && PluginPermission.SUB_COMMAND_ADVERTISE_MANY_OTHER.has(sender))
      return offlinePlayerCache.createSuggestions(args[2]);

    return List.of();
  }

  @Override
  public String getUsage(CommandSender sender) {
    var usageEnvironment = config.rootSection.getBaseEnvironment()
      .withStaticVariable("sub_label", label)
      .withStaticVariable("many_target_names", ManyTarget.matcher.createCompletions(null))
      .withStaticVariable("advertise_mode_names", AdvertiseMode.matcher.createCompletions(null))
      .build();

    BukkitEvaluable usage;

    if (PluginPermission.SUB_COMMAND_ADVERTISE_MANY_OTHER.has(sender))
      usage = config.rootSection.playerMessages.commandAdvertiseManyUsageOther;
    else
      usage = config.rootSection.playerMessages.commandAdvertiseManyUsageSelf;

    return usage.asScalar(ScalarType.STRING, usageEnvironment);
  }

  @Override
  public String getDescription(CommandSender sender) {
    BukkitEvaluable description;

    if (PluginPermission.SUB_COMMAND_ADVERTISE_MANY_OTHER.has(sender))
      description = config.rootSection.playerMessages.commandAdvertiseManyDescriptionOther;
    else
      description = config.rootSection.playerMessages.commandAdvertiseManyDescriptionSelf;

    return description.asScalar(ScalarType.STRING, config.rootSection.builtBaseEnvironment);
  }
}
