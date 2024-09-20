package me.blvckbytes.quick_shop_search.display;

import com.ghostchu.quickshop.api.shop.Shop;
import me.blvckbytes.bukkitevaluable.ItemBuilder;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ResultDisplay {

  public static final int INVENTORY_N_ROWS = 6;
  public static final int BACKWARDS_SLOT_ID = 45;
  public static final int FORWARDS_SLOT_ID = 53;

  private static final Set<Integer> DISPLAY_SLOTS;

  static {
    DISPLAY_SLOTS = IntStream.range(0, INVENTORY_N_ROWS * 9).boxed().collect(Collectors.toSet());
    DISPLAY_SLOTS.remove(BACKWARDS_SLOT_ID);
    DISPLAY_SLOTS.remove(FORWARDS_SLOT_ID);
  }

  private final MainSection mainSection;

  private final List<Shop> shops;
  private final Player player;
  private final Map<Integer, Shop> slotMap;
  private final int numberOfPages;

  private Inventory inventory;
  private int currentPage = 1;

  public ResultDisplay(MainSection mainSection, Player player, List<Shop> shops) {
    this.mainSection = mainSection;
    this.player = player;
    this.shops = shops;
    this.slotMap = new HashMap<>();
    this.numberOfPages = (int) Math.ceil(shops.size() / (double) DISPLAY_SLOTS.size());
    this.show();
  }

  public @Nullable Shop getShopCorrespondingToSlot(int slot) {
    return slotMap.get(slot);
  }

  public boolean isInventory(Inventory inventory) {
    return inventory == this.inventory;
  }

  public void cleanup(boolean close) {
    if (this.inventory != null)
      this.inventory.clear();
    this.slotMap.clear();

    if (close && player.getOpenInventory().getTopInventory() == inventory)
      player.closeInventory();
  }

  public void nextPage() {
    if (currentPage == numberOfPages)
      return;

    ++currentPage;
    show();
  }

  public void previousPage() {
    if (currentPage == 1)
      return;

    --currentPage;
    show();
  }

  public void firstPage() {
    if (currentPage == 1)
      return;

    currentPage = 1;
    show();
  }

  public void lastPage() {
    if (currentPage == numberOfPages)
      return;

    currentPage = numberOfPages;
    show();
  }

  private void show() {
    // Avoid the case of the client not accepting opening the new inventory
    // and then being able to take items out of there. This way, we're safe.
    this.cleanup(false);

    inventory = makeInventory();
    player.openInventory(inventory);
    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 10, 1);
    renderItems();
  }

  private void renderItems() {
    var itemsIndex = (currentPage - 1) * DISPLAY_SLOTS.size();
    var numberOfItems = shops.size();

    for (var slot : DISPLAY_SLOTS) {
      var currentSlot = itemsIndex++;

      if (currentSlot >= numberOfItems)
        continue;

      var shop = shops.get(currentSlot);
      var shopLocation = shop.getLocation();
      var shopWorld = shopLocation.getWorld();

      var shopEnvironment = makePageEnvironment()
        .withLiveVariable("owner", shop.getOwner()::getDisplay)
        .withLiveVariable("price", shop::getPrice)
        .withLiveVariable("currency", shop::getCurrency)
        .withLiveVariable("remaining_stock", shop::getRemainingStock)
        .withLiveVariable("is_buying", shop::isBuying)
        .withLiveVariable("is_selling", shop::isSelling)
        .withLiveVariable("is_unlimited", shop::isUnlimited)
        .withLiveVariable("loc_world", () -> shopWorld == null ? null : shopWorld.getName())
        .withLiveVariable("loc_x", shopLocation::getBlockX)
        .withLiveVariable("loc_y", shopLocation::getBlockY)
        .withLiveVariable("loc_z", shopLocation::getBlockZ)
        .build();

      var shopItem = shop.getItem();

      var representativeItem = new ItemBuilder(shopItem, shopItem.getAmount())
        .patch(mainSection.resultDisplay.representativePatch)
        .build(shopEnvironment);

      inventory.setItem(slot, representativeItem);
      slotMap.put(slot, shop);
    }
  }

  private Inventory makeInventory() {
    var currentPageEnvironment = makePageEnvironment().build();
    var title = mainSection.resultDisplay.title.stringify(currentPageEnvironment);
    var inventory = Bukkit.createInventory(null, 9 * INVENTORY_N_ROWS, title);

    inventory.setItem(
      BACKWARDS_SLOT_ID,
      mainSection.resultDisplay.previousPage.build(currentPageEnvironment)
    );

    inventory.setItem(
      FORWARDS_SLOT_ID,
      mainSection.resultDisplay.nextPage.build(currentPageEnvironment)
    );

    return inventory;
  }

  private EvaluationEnvironmentBuilder makePageEnvironment() {
    return mainSection.getBaseEnvironment()
      .withStaticVariable("current_page", this.currentPage)
      .withStaticVariable("number_pages", this.numberOfPages);
  }
}
