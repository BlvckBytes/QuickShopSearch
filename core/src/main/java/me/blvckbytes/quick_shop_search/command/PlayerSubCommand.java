package me.blvckbytes.quick_shop_search.command;

import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.StringifyState;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.cache.CachedShopRegistry;
import me.blvckbytes.quick_shop_search.cache.OfflinePlayerCache;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.display.ResultDisplayHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class PlayerSubCommand extends PredicateContainingSubCommand {

  private final OfflinePlayerCache offlinePlayerCache;
  private final ResultDisplayHandler resultDisplayHandler;

  public PlayerSubCommand(
    OfflinePlayerCache offlinePlayerCache,
    ResultDisplayHandler resultDisplayHandler,
    PredicateHelper predicateHelper,
    CachedShopRegistry shopRegistry,
    ConfigKeeper<MainSection> config
  ) {
    super(
      "player", PluginPermission.SUB_COMMAND_PLAYER.node,
      predicateHelper, shopRegistry, config
    );

    this.offlinePlayerCache = offlinePlayerCache;
    this.resultDisplayHandler = resultDisplayHandler;
  }

  // /qss player <name> [predicate]

  @Override
  public ExitCode onCommand(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player))
      return ExitCode.PLAYER_ONLY;

    if (args.length > 2 || args.length == 0)
      return ExitCode.MALFORMED_USAGE;

    var shopOwner = offlinePlayerCache.getByName(args[0]);

    if (shopOwner == null) {
      config.rootSection.playerMessages.commandArgumentUnknownPlayerName.sendMessage(
        sender,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("name", args[0])
          .build()
      );

      return ExitCode.SUCCESS;
    }

    ItemPredicate predicate;

    if (args.length == 1) {
      if (!PluginPermission.EMPTY_PREDICATE.has(player)) {
        config.rootSection.playerMessages.emptyPredicate.sendMessage(
          player, config.rootSection.builtBaseEnvironment
        );

        return ExitCode.SUCCESS;
      }

      predicate = null;
    }

    else {
      predicate = parsePredicateOrSendErrorMessage(player, config.rootSection.predicates.mainLanguage, args, 1);

      if (predicate == null)
        return ExitCode.SUCCESS;
    }

    var displayData = createFilteredDisplayData(player, shopOwner, null, predicate);

    if (displayData.shops().isEmpty()) {
      config.rootSection.playerMessages.noMatches.sendMessage(
        player, config.rootSection.builtBaseEnvironment
      );

      return ExitCode.SUCCESS;
    }

    if (predicate == null) {
      config.rootSection.playerMessages.beforeQueryingPlayerNoPredicate.sendMessage(
        player,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("owner", shopOwner.getName())
          .withStaticVariable("number_shops", displayData.shops().size())
          .build()
      );
    }

    else {
      config.rootSection.playerMessages.beforeQueryingPlayer.sendMessage(
        player,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("owner", shopOwner.getName())
          .withStaticVariable("number_shops", displayData.shops().size())
          .withStaticVariable("predicate", new StringifyState(true).appendPredicate(predicate).toString())
          .build()
      );
    }

    resultDisplayHandler.show(player, displayData);

    return ExitCode.SUCCESS;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player))
      return List.of();

    if (args.length == 1)
      return offlinePlayerCache.createSuggestions(args[0]);

    return handlePredicateCompletion(player, config.rootSection.predicates.mainLanguage, args, 1);
  }

  @Override
  public String getUsage(CommandSender sender) {
    return config.rootSection.playerMessages.commandPlayerUsage.asScalar(
      ScalarType.STRING, config.rootSection.getBaseEnvironment()
        .withStaticVariable("sub_label", label)
        .build()
    );
  }

  @Override
  public String getDescription(CommandSender sender) {
    return config.rootSection.playerMessages.commandPlayerDescription.asScalar(
      ScalarType.STRING, config.rootSection.builtBaseEnvironment
    );
  }
}
