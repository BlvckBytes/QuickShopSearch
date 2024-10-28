package me.blvckbytes.quick_shop_search.cache;

import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.shop.Shop;
import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.bukkitevaluable.IItemBuildable;
import me.blvckbytes.bukkitevaluable.ItemBuilder;
import me.blvckbytes.bukkitevaluable.section.ItemStackSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.inventory.ItemStack;

public class CachedShop {

  public final Shop handle;
  public final String shopWorldName;
  public final ShopScalarDiff diff;

  private ItemStackSection representativePatch;
  private final EvaluationEnvironmentBuilder shopEnvironment;
  private final ConfigKeeper<MainSection> config;

  private IItemBuildable representativeBuildable;
  public int cachedStock;
  public int cachedSpace;

  public CachedShop(
    PlatformScheduler scheduler,
    Shop handle,
    ConfigKeeper<MainSection> config
  ) {
    this.handle = handle;
    this.diff = new ShopScalarDiff(this);
    this.config = config;

    var shopLocation = handle.getLocation();
    var shopWorld = shopLocation.getWorld();

    this.shopWorldName = shopWorld == null ? "?" : shopWorld.getName();

    var shopManager = QuickShopAPI.getInstance().getShopManager();

    this.shopEnvironment = new EvaluationEnvironmentBuilder()
      .withLiveVariable("owner", handle.getOwner()::getDisplay)
      .withLiveVariable("name", handle::getShopName)
      .withLiveVariable("price", () -> shopManager.format(handle.getPrice(), handle))
      .withLiveVariable("item_type", () -> formatItemType(handle.getItem()))
      .withLiveVariable("remaining_stock", () -> this.cachedStock)
      .withLiveVariable("remaining_space", () -> this.cachedSpace)
      .withLiveVariable("is_buying", handle::isBuying)
      .withLiveVariable("is_selling", handle::isSelling)
      .withLiveVariable("is_unlimited", handle::isUnlimited)
      .withLiveVariable("loc_world", () -> shopWorldName)
      .withLiveVariable("loc_x", shopLocation::getBlockX)
      .withLiveVariable("loc_y", shopLocation::getBlockY)
      .withLiveVariable("loc_z", shopLocation::getBlockZ);

    scheduler.runAtLocation(shopLocation, scheduleTask -> {
      this.diff.update();
      onConfigReload();

      this.representativeBuildable = makeBuildable(handle.getItem());
      this.cachedStock = handle.getRemainingStock();
      this.cachedSpace = handle.getRemainingSpace();
      this.diff.update();
    });
  }

  private String formatItemType(ItemStack item) {
    var typeEnumName = item.getType().name();
    var formattedName = new char[typeEnumName.length()];

    for (var i = 0; i < formattedName.length; ++i) {
      var currentChar = typeEnumName.charAt(i);

      if (currentChar == '_') {
        formattedName[i] = ' ';
        continue;
      }

      if (i == 0 || formattedName[i - 1] == ' ') {
        formattedName[i] = Character.toUpperCase(currentChar);
        continue;
      }

      formattedName[i] = Character.toLowerCase(currentChar);
    }

    return new String(formattedName);
  }

  public void onConfigReload() {
    this.representativePatch = config.rootSection.resultDisplay.items.representativePatch;
    this.representativeBuildable = makeBuildable(this.handle.getItem());
  }

  public IItemBuildable getRepresentativeBuildable() {
    return representativeBuildable;
  }

  public EvaluationEnvironmentBuilder getShopEnvironment() {
    return shopEnvironment;
  }

  public void onItemChange(ItemStack newItem) {
    this.representativeBuildable = makeBuildable(newItem);
  }

  private IItemBuildable makeBuildable(ItemStack item) {
    return new ItemBuilder(item, item.getAmount())
      .patch(representativePatch);
  }
}
