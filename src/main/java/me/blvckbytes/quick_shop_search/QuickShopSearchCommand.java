package me.blvckbytes.quick_shop_search;

import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.parse.PredicateParserFactory;
import me.blvckbytes.item_predicate_parser.parse.TokenParser;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.PredicateState;
import me.blvckbytes.item_predicate_parser.token.TokenUtil;
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

  private static final int MAX_COMPLETER_RESULTS = 30;

  private final Plugin plugin;
  private final PredicateParserFactory parserFactory;
  private final CachedShopRegistry shopRegistry;
  private final ResultDisplayHandler resultDisplay;

  private final MainSection mainSection;

  public QuickShopSearchCommand(
    Plugin plugin,
    PredicateParserFactory parserFactory,
    CachedShopRegistry shopRegistry,
    MainSection mainSection,
    ResultDisplayHandler resultDisplay
  ) {
    this.plugin = plugin;
    this.parserFactory = parserFactory;
    this.shopRegistry = shopRegistry;
    this.resultDisplay = resultDisplay;
    this.mainSection = mainSection;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player))
      return false;

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      ItemPredicate predicate;
      BukkitEvaluable message;

      try {
        predicate = parserFactory.create(TokenParser.parseTokens(args, 0), false).parseAst();
      } catch (ItemPredicateParseException e) {
        player.sendMessage(generateParseExceptionMessage(e));
        return;
      }

      if (predicate == null) {
        if ((message = mainSection.playerMessages.emptyPredicate) != null)
          player.sendMessage(message.stringify(mainSection.getBaseEnvironment().build()));
        return;
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
      return null;

    try {
      var tokens = TokenParser.parseTokens(args, 0);

      try {
        // Allow for missing closing parentheses, as HotBar-previewing would become useless otherwise
        var currentCommandRepresentation = parserFactory.create(new ArrayList<>(tokens), true).parseAst();

        if (mainSection.predicates.expandedPreview != null && currentCommandRepresentation != null) {
          showActionBarMessage(
            player,
            mainSection.predicates.expandedPreview.stringify(
              new EvaluationEnvironmentBuilder()
                .withStaticVariable("command_representation", currentCommandRepresentation.stringify(false))
                .build()
            )
          );
        }
      } catch (ItemPredicateParseException e) {
        showActionBarMessage(player, generateParseExceptionMessage(e));
      }

      if (mainSection.predicates.maxCompletionsExceeded == null)
        return TokenUtil.createSuggestions(parserFactory.registry, MAX_COMPLETER_RESULTS, null, tokens);

      return TokenUtil.createSuggestions(parserFactory.registry, MAX_COMPLETER_RESULTS, excess -> (
        mainSection.predicates.maxCompletionsExceeded.stringify(
          new EvaluationEnvironmentBuilder()
            .withStaticVariable("excess_count", excess)
            .build()
        )
      ), tokens);
    } catch (ItemPredicateParseException e) {
      showActionBarMessage(player, generateParseExceptionMessage(e));
      return null;
    }
  }

  private void showActionBarMessage(Player player, String message) {
    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
  }

  private String generateParseExceptionMessage(ItemPredicateParseException exception) {
    String highlightPrefix = "", nonHighlightPrefix = "";

    if (mainSection.predicates.inputHighlightPrefix != null)
      highlightPrefix = mainSection.predicates.inputHighlightPrefix.stringify();

    if (mainSection.predicates.inputNonHighlightPrefix != null)
      nonHighlightPrefix = mainSection.predicates.inputNonHighlightPrefix.stringify();

    var highlightedInput = exception.highlightedInput(nonHighlightPrefix, highlightPrefix);

    var conflictEvaluable = mainSection.predicates.parseConflicts.get(exception.getConflict().name());

    if (conflictEvaluable == null)
      return highlightedInput;

    return conflictEvaluable.stringify(
      mainSection.getBaseEnvironment()
        .withStaticVariable("highlighted_input", highlightedInput)
        .build()
    );
  }
}
