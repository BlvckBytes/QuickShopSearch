package me.blvckbytes.quick_shop_search.command;

import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.cache.CachedShopRegistry;
import me.blvckbytes.quick_shop_search.cache.OfflinePlayerCache;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AdvertiseMultiSubCommand extends SubCommand {

  private enum MultiTarget {
    // All shops of the target-user
    ALL,
    // All shops of the target-user which have an advertising-state flag set
    SET,
    // All shops of the target-user which do not have an advertising-state flag set
    UNSET,
    // Has advertising enabled
    ON,
    // Has advertising disabled
    OFF
  }

  private final CachedShopRegistry shopRegistry;
  private final OfflinePlayerCache offlinePlayerCache;
  private final ConfigKeeper<MainSection> config;

  public AdvertiseMultiSubCommand(
    CachedShopRegistry shopRegistry,
    OfflinePlayerCache offlinePlayerCache,
    ConfigKeeper<MainSection> config
  ) {
    super("advertise-many", PluginPermission.SUB_COMMAND_ADVERTISE_MANY.node);

    this.shopRegistry = shopRegistry;
    this.offlinePlayerCache = offlinePlayerCache;
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
        config.rootSection.playerMessages.missingPermissionAdvertiseMultiOther.sendMessage(
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

    var multiTarget = parseEnumConstant(MultiTarget.class, args[0]);

    if (multiTarget == null)
      return ExitCode.MALFORMED_USAGE;

    var mode = parseEnumConstant(AdvertiseMode.class, args[1]);

    if (mode == null)
      return ExitCode.MALFORMED_USAGE;

    var targetShops = shopRegistry.getShopsWithOwner(shopOwner);

    if (targetShops.isEmpty()) {
      if (shopOwner == sender) {
        config.rootSection.playerMessages.commandAdvertiseMultiOwnsNoShopsSelf.sendMessage(
          sender, config.rootSection.builtBaseEnvironment
        );

        return ExitCode.SUCCESS;
      }

      config.rootSection.playerMessages.commandAdvertiseMultiOwnsNoShopsOther.sendMessage(
        sender,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("name", shopOwner.getName())
          .build()
      );

      return ExitCode.SUCCESS;
    }

    var alteredShops = new ArrayList<CachedShop>();

    for (var targetShop : targetShops) {
      var isShopTargeted = switch (multiTarget) {
        case ALL -> true;
        case SET -> targetShop.isAdvertisingSet();
        case UNSET -> !targetShop.isAdvertisingSet();
        case ON -> targetShop.isAdvertisingSet() && targetShop.isAdvertising();
        case OFF -> targetShop.isAdvertisingSet() && !targetShop.isAdvertising();
      };

      if (!isShopTargeted)
        continue;

      switch (mode) {
        case ON -> targetShop.setAdvertising(true);
        case OFF -> targetShop.setAdvertising(false);
        case UNSET -> targetShop.setAdvertising(null);
      }

      alteredShops.add(targetShop);
    }

    if (alteredShops.isEmpty()) {
      if (shopOwner == sender) {
        config.rootSection.playerMessages.commandAdvertiseMultiNoShopsMatchedTargetSelf.sendMessage(
          sender, config.rootSection.builtBaseEnvironment
        );

        return ExitCode.SUCCESS;
      }

      config.rootSection.playerMessages.commandAdvertiseMultiNoShopsMatchedTargetOther.sendMessage(
        sender,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("name", shopOwner.getName())
          .build()
      );

      return ExitCode.SUCCESS;
    }

    BukkitEvaluable message;

    var modeEnvironment = new EvaluationEnvironmentBuilder()
      .withStaticVariable("advertise_mode", mode.name())
      .build();

    if ((message = config.rootSection.playerMessages.commandAdvertiseMultiAlteredScreenHeader) != null)
      message.sendMessage(sender, modeEnvironment);

    for (var alteredShop : alteredShops)
      config.rootSection.playerMessages.commandAdvertiseMultiAlteredScreenLine.sendMessage(sender, alteredShop.getShopEnvironment().build(modeEnvironment));

    if ((message = config.rootSection.playerMessages.commandAdvertiseMultiAlteredScreenFooter) != null)
      message.sendMessage(sender, modeEnvironment);

    return ExitCode.SUCCESS;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, String[] args) {
    if (args.length == 1)
      return suggestEnumConstants(MultiTarget.class, args[0]);

    if (args.length == 2)
      return suggestEnumConstants(AdvertiseMode.class, args[1]);

    if (args.length == 3 && PluginPermission.SUB_COMMAND_ADVERTISE_MANY_OTHER.has(sender))
      return offlinePlayerCache.createSuggestions(args[2]);

    return List.of();
  }

  @Override
  public String getUsage(CommandSender sender) {
    var usageEnvironment = config.rootSection.getBaseEnvironment()
      .withStaticVariable("sub_label", label)
      .withStaticVariable("multi_target_names", suggestEnumConstants(MultiTarget.class, null))
      .withStaticVariable("advertise_mode_names", suggestEnumConstants(AdvertiseMode.class, null))
      .build();

    BukkitEvaluable usage;

    if (PluginPermission.SUB_COMMAND_ADVERTISE_MANY_OTHER.has(sender))
      usage = config.rootSection.playerMessages.commandAdvertiseMultiUsageOther;
    else
      usage = config.rootSection.playerMessages.commandAdvertiseMultiUsageSelf;

    return usage.asScalar(ScalarType.STRING, usageEnvironment);
  }

  @Override
  public String getDescription(CommandSender sender) {
    BukkitEvaluable description;

    if (PluginPermission.SUB_COMMAND_ADVERTISE_MANY_OTHER.has(sender))
      description = config.rootSection.playerMessages.commandAdvertiseMultiDescriptionOther;
    else
      description = config.rootSection.playerMessages.commandAdvertiseMultiDescriptionSelf;

    return description.asScalar(ScalarType.STRING, config.rootSection.builtBaseEnvironment);
  }
}
