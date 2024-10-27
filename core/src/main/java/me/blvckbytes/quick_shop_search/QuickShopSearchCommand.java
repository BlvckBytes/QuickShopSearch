package me.blvckbytes.quick_shop_search;

import com.tcoded.folialib.impl.PlatformScheduler;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.parse.NormalizedConstant;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.PredicateState;
import me.blvckbytes.item_predicate_parser.predicate.StringifyState;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.cache.CachedShopRegistry;
import me.blvckbytes.quick_shop_search.config.MainSection;
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
import java.util.List;

public class QuickShopSearchCommand implements CommandExecutor, TabCompleter {

  private final PlatformScheduler scheduler;
  private final PredicateHelper predicateHelper;
  private final CachedShopRegistry shopRegistry;
  private final ResultDisplayHandler resultDisplay;
  private final ConfigKeeper<MainSection> config;

  public QuickShopSearchCommand(
    PlatformScheduler scheduler,
    PredicateHelper predicateHelper,
    CachedShopRegistry shopRegistry,
    ConfigKeeper<MainSection> config,
    ResultDisplayHandler resultDisplay
  ) {
    this.scheduler = scheduler;
    this.predicateHelper = predicateHelper;
    this.shopRegistry = shopRegistry;
    this.config = config;
    this.resultDisplay = resultDisplay;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player))
      return false;

    scheduler.runAsync(scheduleTask -> {
      BukkitEvaluable message;

      var language = config.rootSection.predicates.mainLanguage;
      var argsOffset = 0;

      if (command.getName().equals(config.rootSection.commands.quickShopSearchLanguage.evaluatedName)) {
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
        if (argsOffset == 0 && PluginPermission.EMPTY_PREDICATE.has(player)) {
          if ((message = config.rootSection.playerMessages.queryingAllShops) != null) {
            message.sendMessage(
              player,
              config.rootSection.getBaseEnvironment()
                .withLiveVariable("number_shops", shopRegistry::getNumberOfExistingShops)
                .build()
            );
          }

          resultDisplay.show(player, shopRegistry.getExistingShops());
          return;
        }

        if ((message = config.rootSection.playerMessages.emptyPredicate) != null)
          message.sendMessage(player, config.rootSection.builtBaseEnvironment);

        return;
      }

      if ((message = config.rootSection.playerMessages.beforeQuerying) != null) {
        message.sendMessage(
          player,
          config.rootSection.getBaseEnvironment()
            .withLiveVariable("number_shops", shopRegistry::getNumberOfExistingShops)
            .withStaticVariable("predicate", new StringifyState(true).appendPredicate(predicate).toString())
            .build()
        );
      }

      var matchingShops = new ArrayList<CachedShop>();
      var matchingShopIds = new LongOpenHashSet();

      for (var shop : shopRegistry.getExistingShops().shops()) {
        if (!predicate.test(new PredicateState(shop.handle.getItem())))
          continue;

        matchingShops.add(shop);
        matchingShopIds.add(shop.handle.getShopId());
      }

      if (matchingShops.isEmpty()) {
        if ((message = config.rootSection.playerMessages.noMatches) != null)
          message.sendMessage(player, config.rootSection.builtBaseEnvironment);
        return;
      }

      resultDisplay.show(player, new DisplayData(matchingShops, matchingShopIds, predicate, false));
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

  private void showActionBarMessage(Player player, String message) {
    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
  }
}
