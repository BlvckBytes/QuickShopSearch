package me.blvckbytes.quick_shop_search.command;

import com.tcoded.folialib.impl.PlatformScheduler;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.PredicateState;
import me.blvckbytes.item_predicate_parser.predicate.StringifyState;
import me.blvckbytes.item_predicate_parser.syllables_matcher.NormalizedConstant;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import me.blvckbytes.quick_shop_search.OfflinePlayerRegistry;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.cache.CachedShopRegistry;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.config.ShopAccessListSection;
import me.blvckbytes.quick_shop_search.display.DisplayData;
import me.blvckbytes.quick_shop_search.display.ResultDisplayHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class QuickShopSearchCommand implements CommandExecutor, TabCompleter {

  private final PlatformScheduler scheduler;
  private final PredicateHelper predicateHelper;
  private final CachedShopRegistry shopRegistry;
  private final ResultDisplayHandler resultDisplay;
  private final OfflinePlayerRegistry offlinePlayerRegistry;
  private final ConfigKeeper<MainSection> config;

  public QuickShopSearchCommand(
    PlatformScheduler scheduler,
    PredicateHelper predicateHelper,
    CachedShopRegistry shopRegistry,
    ConfigKeeper<MainSection> config,
    ResultDisplayHandler resultDisplay,
    OfflinePlayerRegistry offlinePlayerRegistry
  ) {
    this.scheduler = scheduler;
    this.predicateHelper = predicateHelper;
    this.shopRegistry = shopRegistry;
    this.config = config;
    this.resultDisplay = resultDisplay;
    this.offlinePlayerRegistry = offlinePlayerRegistry;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player))
      return false;

    scheduler.runAsync(scheduleTask -> {
      BukkitEvaluable message;

      var language = config.rootSection.predicates.mainLanguage;
      var argsOffset = 0;
      var searchFlagsContainer = new SearchFlagsContainer();
      var isLanguageCommand = command.getName().equals(config.rootSection.commands.quickShopSearchLanguage.evaluatedName);

      if (isLanguageCommand) {
        if (!PluginPermission.LANGUAGE_COMMAND.has(player)) {
          if ((message = config.rootSection.playerMessages.missingPermissionLanguageCommand) != null)
            message.applicator.sendMessage(player, message, config.rootSection.builtBaseEnvironment);

          return;
        }

        NormalizedConstant<TranslationLanguage> matchedLanguage;

        if (args.length == 0 || (matchedLanguage = TranslationLanguage.matcher.matchFirst(args[0])) == null) {
          if ((message = config.rootSection.playerMessages.usageQsslCommandLanguage) != null) {
            message.sendMessage(
              player,
              config.rootSection.getBaseEnvironment()
                .withStaticVariable("label", label)
                .withStaticVariable("languages", TranslationLanguage.matcher.createCompletions(null))
                .build()
            );
          }

          return;
        }

        language = matchedLanguage.constant;
        argsOffset = 1;
      }

      if (!PluginPermission.MAIN_COMMAND.has(player)) {
        if ((message = config.rootSection.playerMessages.missingPermissionMainCommand) != null)
          message.sendMessage(player, config.rootSection.builtBaseEnvironment);

        return;
      }

      while (argsOffset < args.length) {
        var currentArg = args[argsOffset];

        if (currentArg.isEmpty() || currentArg.charAt(0) != '-')
          break;

        var searchFlag = SearchFlag.matcher.matchFirst(currentArg.substring(1), SearchFlag.permissionPredicate(player));

        if (searchFlag == null)
          break;

        if (argsOffset + 1 == args.length)
          break;

        var nextArg = args[argsOffset + 1];

        if (!searchFlagsContainer.parseAndSet(searchFlag.constant, nextArg, offlinePlayerRegistry)) {
          if ((message = config.rootSection.playerMessages.searchFlagParseError) != null) {
            message.sendMessage(
              player,
              config.rootSection.getBaseEnvironment()
                .withStaticVariable("flag_name", searchFlag.getNormalizedName())
                .withStaticVariable("flag_value", nextArg)
                .build()
            );
          }

          return;
        }

        argsOffset += 2;
      }

      ItemPredicate predicate;

      try {
        var tokens = predicateHelper.parseTokens(args, argsOffset);
        predicate = predicateHelper.parsePredicate(language, tokens);
      } catch (ItemPredicateParseException e) {
        if ((message = config.rootSection.playerMessages.predicateParseError) != null) {
          message.sendMessage(
            player,
            config.rootSection.getBaseEnvironment()
              .withStaticVariable("error_message", predicateHelper.createExceptionMessage(e))
              .build()
          );
        }
        return;
      }

      if (predicate == null) {
        // Empty predicates on a command which requires specifying the desired language explicitly seem odd...
        if (!isLanguageCommand && PluginPermission.EMPTY_PREDICATE.has(player)) {
          var displayData = createFilteredDisplayData(player, null, searchFlagsContainer);

          if (displayData.hasAnyConstraints() && displayData.shops().isEmpty()) {
            if ((message = config.rootSection.playerMessages.noMatches) != null)
              message.sendMessage(player, config.rootSection.builtBaseEnvironment);
            return;
          }

          if ((message = config.rootSection.playerMessages.queryingAllShops) != null) {
            message.sendMessage(
              player,
              config.rootSection.getBaseEnvironment()
                .withStaticVariable("number_shops", displayData.shops().size())
                .build()
            );
          }

          resultDisplay.show(player, displayData);
          return;
        }

        if ((message = config.rootSection.playerMessages.emptyPredicate) != null)
          message.sendMessage(player, config.rootSection.builtBaseEnvironment);

        return;
      }

      var displayData = createFilteredDisplayData(player, predicate, searchFlagsContainer);

      if ((message = config.rootSection.playerMessages.beforeQuerying) != null) {
        message.sendMessage(
          player,
          config.rootSection.getBaseEnvironment()
            .withStaticVariable("number_shops", displayData.shops().size())
            .withStaticVariable("predicate", new StringifyState(true).appendPredicate(predicate).toString())
            .build()
        );
      }

      if (displayData.hasAnyConstraints() && displayData.shops().isEmpty()) {
        if ((message = config.rootSection.playerMessages.noMatches) != null)
          message.sendMessage(player, config.rootSection.builtBaseEnvironment);
        return;
      }

      resultDisplay.show(player, displayData);
    });

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player))
      return List.of();

    BukkitEvaluable message;

    var language = config.rootSection.predicates.mainLanguage;
    var argsOffset = 0;

    if (command.getName().equals(config.rootSection.commands.quickShopSearchLanguage.evaluatedName)) {
      if (!PluginPermission.LANGUAGE_COMMAND.has(player))
        return List.of();

      argsOffset = 1;

      if (args.length == 1)
        return TranslationLanguage.matcher.createCompletions(args[0]);

      var matchedLanguage = TranslationLanguage.matcher.matchFirst(args[0]);

      if (matchedLanguage == null) {
        if ((message = config.rootSection.playerMessages.unknownLanguageActionBar) != null) {
          showActionBarMessage(player, message.asScalar(
            ScalarType.STRING,
            config.rootSection.getBaseEnvironment()
              .withStaticVariable("user_input", args[0])
              .build()
          ));
        }

        return List.of();
      }

      language = matchedLanguage.constant;
    }

    if (!PluginPermission.MAIN_COMMAND.has(player))
      return List.of();

    var searchFlagPredicate = SearchFlag.permissionPredicate(player);
    var encounteredSearchFlags = new HashSet<SearchFlag<?>>();

    while (argsOffset < args.length) {
      var currentArg = args[argsOffset];

      if (currentArg.isEmpty() || currentArg.charAt(0) != '-')
        break;

      var searchFlagName = currentArg.substring(1);

      if (argsOffset + 1 == args.length) {
        return SearchFlag.matcher.createCompletions(
          searchFlagName,
          searchFlag -> {
            if (encounteredSearchFlags.contains(searchFlag.constant))
              return false;

            return searchFlagPredicate.test(searchFlag);
          },
          "-", null
        );
      }

      var searchFlag = SearchFlag.matcher.matchFirst(searchFlagName, searchFlagPredicate);

      if (searchFlag == null)
        break;

      encounteredSearchFlags.add(searchFlag.constant);

      var searchFlagValue = args[argsOffset + 1];

      argsOffset += 2;

      if (argsOffset == args.length) {
        if (searchFlag.constant == SearchFlag.OWNER)
          return this.offlinePlayerRegistry.createSuggestions(searchFlagValue, 10);

        return searchFlag.constant.getSuggestions(searchFlagValue);
      }
    }

    try {
      var tokens = predicateHelper.parseTokens(args, argsOffset);
      var completion = predicateHelper.createCompletion(language, tokens);

      if (completion.expandedPreviewOrError() != null)
        showActionBarMessage(player, completion.expandedPreviewOrError());

      return completion.suggestions();

    } catch (ItemPredicateParseException e) {
      showActionBarMessage(player, predicateHelper.createExceptionMessage(e));
      return List.of();
    }
  }

  private DisplayData createFilteredDisplayData(Player player, @Nullable ItemPredicate predicate, SearchFlagsContainer searchFlagsContainer) {
    var matchingShops = new ArrayList<CachedShop>();
    var matchingShopIds = new LongOpenHashSet();
    var playerWorld = player.getWorld();
    var accessList = decideAccessListFor(player);

    var canViewOtherWorld = PluginPermission.OTHER_WORLD.has(player);
    var canViewNonAdvertising = PluginPermission.NON_ADVERTISE_BYPASS.has(player);

    for (var cachedShop : shopRegistry.getExistingShops()) {
      if (!cachedShop.isAdvertising() && !canViewNonAdvertising)
        continue;

      if (!canViewOtherWorld && cachedShop.handle.getLocation().getWorld() != playerWorld)
        continue;

      if (accessList != null && !accessList.allowsShop(cachedShop))
        continue;

      if (predicate != null && !predicate.test(new PredicateState(cachedShop.handle.getItem())))
        continue;

      if (!searchFlagsContainer.test(cachedShop, player))
        continue;

      matchingShops.add(cachedShop);
      matchingShopIds.add(cachedShop.handle.getShopId());
    }

    return new DisplayData(matchingShops, matchingShopIds, predicate, searchFlagsContainer);
  }

  private @Nullable ShopAccessListSection decideAccessListFor(Player player) {
    if (PluginPermission.ACCESS_LISTS_BYPASS.has(player))
      return null;

    for (var permissionEntry : config.rootSection.shopAccessLists.permissions.entrySet()) {
      if (player.hasPermission(PluginPermission.ACCESS_LIST_BASE.nodeWithSuffix(permissionEntry.getKey())))
        return permissionEntry.getValue();
    }

    return config.rootSection.shopAccessLists._default;
  }

  private void showActionBarMessage(Player player, String message) {
    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
  }
}
