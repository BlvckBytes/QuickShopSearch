package me.blvckbytes.quick_shop_search;

import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.PredicateState;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.display.ResultDisplayHandler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class QuickShopSearchCommand implements CommandExecutor, TabCompleter {

  public static final String MAIN_COMMAND_NAME = "quickshopsearch";
  public static final String LANGUAGE_COMMAND_NAME = "quickshopsearchlanguage";

  private final Plugin plugin;
  private final PredicateHelper predicateHelper;
  private final CachedShopRegistry shopRegistry;
  private final ResultDisplayHandler resultDisplay;

  private MainSection mainSection;

  public QuickShopSearchCommand(
    Plugin plugin,
    PredicateHelper predicateHelper,
    CachedShopRegistry shopRegistry,
    ValuePusher<MainSection> configPusher,
    ResultDisplayHandler resultDisplay
  ) {
    this.plugin = plugin;
    this.predicateHelper = predicateHelper;
    this.shopRegistry = shopRegistry;
    this.resultDisplay = resultDisplay;

    this.mainSection = configPusher
      .subscribeToUpdates(value -> this.mainSection = value)
      .get();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player))
      return false;

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      BukkitEvaluable message;

      var language = mainSection.predicates.mainLanguage;
      var argsOffset = 0;

      if (command.getName().equals(LANGUAGE_COMMAND_NAME)) {
        if (!PluginPermission.LANGUAGE_COMMAND.has(player)) {
          if ((message = mainSection.playerMessages.missingPermissionLanguageCommand) != null)
            player.sendMessage(message.stringify(mainSection.getBaseEnvironment().build()));

          return;
        }

        if (args.length == 0) {
          if ((message = mainSection.playerMessages.missingLanguage) != null)
            player.sendMessage(message.stringify(mainSection.getBaseEnvironment().build()));

          return;
        }

        language = TranslationLanguage.matchFirst(args[0]);

        if (language == null) {
          if ((message = mainSection.playerMessages.unknownLanguageChat) != null) {
            player.sendMessage(message.stringify(
              mainSection.getBaseEnvironment()
                .withStaticVariable("user_input", args[0])
                .build()
            ));
          }

          return;
        }

        argsOffset = 1;
      }

      if (!PluginPermission.MAIN_COMMAND.has(player)) {
        if ((message = mainSection.playerMessages.missingPermissionMainCommand) != null)
          player.sendMessage(message.stringify(mainSection.getBaseEnvironment().build()));

        return;
      }

      ItemPredicate predicate;

      try {
        var tokens = predicateHelper.parseTokens(args, argsOffset);
        predicate = predicateHelper.parsePredicate(language, tokens);
      } catch (ItemPredicateParseException e) {
        player.sendMessage(predicateHelper.createExceptionMessage(e));
        return;
      }

      if (predicate == null) {
        if (PluginPermission.EMPTY_PREDICATE.has(player)) {
          if ((message = mainSection.playerMessages.queryingAllShops) != null) {
            player.sendMessage(message.stringify(
              mainSection.getBaseEnvironment()
                .withLiveVariable("number_shops", shopRegistry::getNumberOfExistingShops)
                .build()
            ));
          }

          resultDisplay.show(player, shopRegistry.getExistingShops());
          return;
        }

        if ((message = mainSection.playerMessages.emptyPredicate) != null)
          player.sendMessage(message.stringify(mainSection.getBaseEnvironment().build()));

        return;
      }

      if ((message = mainSection.playerMessages.beforeQuerying) != null) {
        player.sendMessage(message.stringify(
          mainSection.getBaseEnvironment()
            .withLiveVariable("number_shops", shopRegistry::getNumberOfExistingShops)
            .withStaticVariable("predicate", predicate.stringify(true))
            .build()
        ));
      }

      var matchingShops = new ArrayList<CachedShop>();

      for (var shop : shopRegistry.getExistingShops()) {
        if (predicate.test(new PredicateState(shop.getShop().getItem())))
          matchingShops.add(shop);
      }

      if (matchingShops.isEmpty()) {
        if ((message = mainSection.playerMessages.noMatches) != null)
          player.sendMessage(message.stringify(mainSection.getBaseEnvironment().build()));
        return;
      }

      resultDisplay.show(player, matchingShops);
    });

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player))
      return List.of();

    BukkitEvaluable message;

    var language = mainSection.predicates.mainLanguage;
    var argsOffset = 0;

    if (command.getName().equals(LANGUAGE_COMMAND_NAME)) {
      if (!PluginPermission.LANGUAGE_COMMAND.has(player))
        return List.of();

      argsOffset = 1;

      if (args.length == 1)
        return TranslationLanguage.createCompletions(args[0]);

      language = TranslationLanguage.matchFirst(args[0]);

      if (language == null) {
        if ((message = mainSection.playerMessages.unknownLanguageActionBar) != null) {
          showActionBarMessage(player, message.stringify(
            mainSection.getBaseEnvironment()
              .withStaticVariable("user_input", args[0])
              .build()
          ));
        }

        return List.of();
      }
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
