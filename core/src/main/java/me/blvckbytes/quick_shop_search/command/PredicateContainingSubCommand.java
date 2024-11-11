package me.blvckbytes.quick_shop_search.command;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.PredicateState;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.cache.CachedShopRegistry;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.config.ShopAccessListSection;
import me.blvckbytes.quick_shop_search.display.DisplayData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class PredicateContainingSubCommand extends SubCommand {

  protected final PredicateHelper predicateHelper;
  protected final CachedShopRegistry shopRegistry;
  protected final ConfigKeeper<MainSection> config;

  public PredicateContainingSubCommand(
    String label, String permission,
    PredicateHelper predicateHelper,
    CachedShopRegistry shopRegistry,
    ConfigKeeper<MainSection> config
  ) {
    super(label, permission);

    this.predicateHelper = predicateHelper;
    this.shopRegistry = shopRegistry;
    this.config = config;
  }

  protected @Nullable ItemPredicate parsePredicateOrSendErrorMessage(Player player, TranslationLanguage language, String[] args, int argsOffset) {
    try {
      var tokens = predicateHelper.parseTokens(args, argsOffset);
      return predicateHelper.parsePredicate(language, tokens);
    } catch (ItemPredicateParseException e) {
      config.rootSection.playerMessages.predicateParseError.sendMessage(
        player,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("error_message", predicateHelper.createExceptionMessage(e))
          .build()
      );
    }

    return null;
  }

  protected List<String> handlePredicateCompletion(Player player, TranslationLanguage language, String[] args, int argsOffset) {
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

  protected DisplayData createFilteredDisplayData(
    Player player,
    @Nullable OfflinePlayer shopOwner,
    @Nullable Integer distance,
    @Nullable ItemPredicate predicate
  ) {
    var matchingShops = new ArrayList<CachedShop>();
    var matchingShopIds = new LongOpenHashSet();
    var playerWorld = player.getWorld();
    var playerLocation = player.getLocation();
    var accessList = decideAccessListFor(player);
    var distanceSquared = distance == null ? null : distance * distance;

    var canViewOtherWorld = PluginPermission.OTHER_WORLD.has(player);
    var canViewNonAdvertising = PluginPermission.NON_ADVERTISE_BYPASS.has(player);

    for (var cachedShop : shopRegistry.getExistingShops()) {
      if (!cachedShop.isAdvertising() && !canViewNonAdvertising)
        continue;

      if (shopOwner != null) {
        var targetOwner = cachedShop.handle.getOwner().getBukkitPlayer().orElse(null);

        if (!shopOwner.equals(targetOwner))
          continue;
      }

      var isOtherWorld = cachedShop.handle.getLocation().getWorld() != playerWorld;

      if (!canViewOtherWorld && isOtherWorld)
        continue;

      if (
        distanceSquared != null && (
          isOtherWorld ||
          blockDistanceSquared(playerLocation, cachedShop.handle.getLocation()) > distanceSquared
        )
      ) {
        continue;
      }

      if (accessList != null && !accessList.allowsShop(cachedShop))
        continue;

      if (predicate != null && !predicate.test(new PredicateState(cachedShop.handle.getItem())))
        continue;

      matchingShops.add(cachedShop);
      matchingShopIds.add(cachedShop.handle.getShopId());
    }

    return new DisplayData(matchingShops, matchingShopIds, predicate);
  }

  protected @Nullable ShopAccessListSection decideAccessListFor(Player player) {
    if (PluginPermission.ACCESS_LISTS_BYPASS.has(player))
      return null;

    for (var permissionEntry : config.rootSection.shopAccessLists.permissions.entrySet()) {
      if (player.hasPermission(PluginPermission.ACCESS_LIST_BASE.nodeWithSuffix(permissionEntry.getKey())))
        return permissionEntry.getValue();
    }

    return config.rootSection.shopAccessLists._default;
  }

  protected void showActionBarMessage(Player player, String message) {
    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
  }

  private int blockDistanceSquared(Location a, Location b) {
    int deltaX = (a.getBlockX() - b.getBlockX());
    int deltaY = (a.getBlockY() - b.getBlockY());
    int deltaZ = (a.getBlockZ() - b.getBlockZ());

    return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
  }
}
