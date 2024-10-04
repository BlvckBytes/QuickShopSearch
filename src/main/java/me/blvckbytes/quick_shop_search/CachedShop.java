package me.blvckbytes.quick_shop_search;

import com.ghostchu.quickshop.api.shop.Shop;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.bukkitevaluable.IItemBuildable;
import me.blvckbytes.bukkitevaluable.ItemBuilder;
import me.blvckbytes.bukkitevaluable.section.ItemStackSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.inventory.ItemStack;

public class CachedShop {

  private final Shop shop;
  private ItemStackSection representativePatch;
  private final EvaluationEnvironmentBuilder shopEnvironment;
  private final ConfigKeeper<MainSection> config;

  private IItemBuildable representativeBuildable;
  private int cachedStock;
  private int cachedSpace;

  public CachedShop(Shop shop, ConfigKeeper<MainSection> config) {
    this.shop = shop;
    this.config = config;

    onConfigReload();

    this.representativeBuildable = makeBuildable(shop.getItem());
    this.cachedStock = shop.getRemainingStock();
    this.cachedSpace = shop.getRemainingSpace();

    var shopLocation = shop.getLocation();
    var shopWorld = shopLocation.getWorld();

    this.shopEnvironment = new EvaluationEnvironmentBuilder()
      .withLiveVariable("owner", shop.getOwner()::getDisplay)
      .withLiveVariable("price", shop::getPrice)
      .withLiveVariable("currency", shop::getCurrency)
      .withLiveVariable("remaining_stock", this::getCachedStock)
      .withLiveVariable("remaining_space", this::getCachedSpace)
      .withLiveVariable("is_buying", shop::isBuying)
      .withLiveVariable("is_selling", shop::isSelling)
      .withLiveVariable("is_unlimited", shop::isUnlimited)
      .withLiveVariable("loc_world", () -> shopWorld == null ? null : shopWorld.getName())
      .withLiveVariable("loc_x", shopLocation::getBlockX)
      .withLiveVariable("loc_y", shopLocation::getBlockY)
      .withLiveVariable("loc_z", shopLocation::getBlockZ);
  }

  public void onConfigReload() {
    this.representativePatch = config.rootSection.resultDisplay.representativePatch;
    this.representativeBuildable = makeBuildable(this.shop.getItem());
  }

  public Shop getShop() {
    return shop;
  }

  public int getCachedStock() {
    return cachedStock;
  }

  public int getCachedSpace() {
    return cachedSpace;
  }

  public IItemBuildable getRepresentativeBuildable() {
    return representativeBuildable;
  }

  public EvaluationEnvironmentBuilder getShopEnvironment() {
    return shopEnvironment;
  }

  public void onInventoryCalculate(int newStock, int newSpace) {
    if (newStock >= 0)
      this.cachedStock = newStock;

    if (newSpace >= 0)
      this.cachedSpace = newSpace;
  }

  public void onItemChange(ItemStack newItem) {
    this.representativeBuildable = makeBuildable(newItem);
  }

  private IItemBuildable makeBuildable(ItemStack item) {
    return new ItemBuilder(item, item.getAmount())
      .patch(representativePatch);
  }
}
