package me.blvckbytes.quick_shop_search;

import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.PredicateState;
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

  private final Plugin plugin;
  private final PredicateHelper predicateHelper;
  private final CachedShopRegistry shopRegistry;
  private final ResultDisplayHandler resultDisplay;

  private final MainSection mainSection;

  public QuickShopSearchCommand(
    Plugin plugin,
    PredicateHelper predicateHelper,
    CachedShopRegistry shopRegistry,
    MainSection mainSection,
    ResultDisplayHandler resultDisplay
  ) {
    this.plugin = plugin;
    this.predicateHelper = predicateHelper;
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
        var tokens = predicateHelper.parseTokens(args, 0);
        predicate = predicateHelper.parsePredicate(mainSection.predicates.language, tokens);
      } catch (ItemPredicateParseException e) {
        player.sendMessage(predicateHelper.createExceptionMessage(e));
        return;
      }

      if (predicate == null) {
        if ((message = mainSection.playerMessages.emptyPredicate) != null)
          player.sendMessage(message.stringify(mainSection.getBaseEnvironment().build()));
        return;
      }

      if ((message = mainSection.playerMessages.beforeQuerying) != null) {
        player.sendMessage(message.stringify(
          mainSection.getBaseEnvironment()
            .withStaticVariable("number_shops", shopRegistry.getNumberOfExistingShops())
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
      return null;

    try {
      var tokens = predicateHelper.parseTokens(args, 0);
      var completion = predicateHelper.createCompletion(mainSection.predicates.language, tokens);

      if (completion.expandedPreviewOrError() != null)
        showActionBarMessage(player, completion.expandedPreviewOrError());

      return completion.suggestions();

    } catch (ItemPredicateParseException e) {
      showActionBarMessage(player, predicateHelper.createExceptionMessage(e));
      return null;
    }
  }

  private void showActionBarMessage(Player player, String message) {
    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
  }
}
