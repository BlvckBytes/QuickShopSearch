package me.blvckbytes.quick_shop_search.cache;

import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopType;
import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.bukkitevaluable.IItemBuildable;
import me.blvckbytes.bukkitevaluable.ItemBuilder;
import me.blvckbytes.bukkitevaluable.section.ItemStackSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.integration.IntegrationRegistry;
import me.blvckbytes.quick_shop_search.integration.worldguard.IWorldGuardIntegration;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.logging.Logger;

public class CachedShop {

  private enum AdvertiseAllow {
    ALLOWED,
    DISALLOWED,
    UNSET
  }

  private final Logger logger;
  public final Shop handle;
  public final String shopWorldName;
  public final ShopScalarDiff diff;

  private ItemStackSection representativePatch;
  private final EvaluationEnvironmentBuilder shopEnvironment;
  private final ConfigKeeper<MainSection> config;
  private final NamespacedKey keyIsAdvertising;
  private final IntegrationRegistry integrationRegistry;

  private IItemBuildable representativeBuildable;
  public int cachedStock;
  public int cachedSpace;
  public ShopType cachedType;
  public double cachedPrice;
  public @Nullable String cachedName;
  private boolean advertising;

  public CachedShop(
    Plugin plugin,
    PlatformScheduler scheduler,
    Shop handle,
    ConfigKeeper<MainSection> config,
    IntegrationRegistry integrationRegistry
  ) {
    this.logger = plugin.getLogger();
    this.handle = handle;
    this.diff = new ShopScalarDiff(this);
    this.config = config;
    this.keyIsAdvertising = new NamespacedKey(plugin, "is_advertising");
    this.integrationRegistry = integrationRegistry;

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
      onConfigReload();

      this.representativeBuildable = makeBuildable(handle.getItem());
      this.cachedStock = handle.getRemainingStock();
      this.cachedSpace = handle.getRemainingSpace();
      this.cachedType = handle.getShopType();
      this.cachedPrice = handle.getPrice();
      this.cachedName = handle.getShopName();
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
    this.loadAdvertising();
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

  public ToggleResult toggleAdvertising(Player player) {
    if (getAdvertiseAllowState() != AdvertiseAllow.ALLOWED) {
      var bypassPermission = PluginPermission.ADVERTISE_COMMAND_ALLOWLIST_BYPASS;

      if (!player.getUniqueId().equals(handle.getOwner().getUniqueId()))
        bypassPermission = PluginPermission.ADVERTISE_COMMAND_ALLOWLIST_BYPASS_OTHERS;

      if (!bypassPermission.has(player))
        return ToggleResult.NOT_ALLOWED;
    }

    var previousState = this.advertising;
    this.advertising ^= true;

    if (!this.storeAdvertising()) {
      this.advertising = previousState;
      return ToggleResult.ERROR;
    }

    return this.advertising ? ToggleResult.NOW_ON : ToggleResult.NOW_OFF;
  }

  public boolean isAdvertising() {
    return advertising;
  }

  private boolean storeAdvertising() {
    return tryAccessDataContainer(container -> {
      container.set(keyIsAdvertising, PersistentDataType.BYTE, (byte) (this.advertising ? 1 : 0));
    });
  }

  private void loadAdvertising() {
    var allowState = getAdvertiseAllowState();

    if (allowState == AdvertiseAllow.DISALLOWED) {
      if (config.rootSection.worldGuardIntegration.disableAdvertiseIfNotAllowed) {
        this.advertising = false;
        return;
      }
    }

    tryAccessDataContainer(container -> {
      var value = container.get(keyIsAdvertising, PersistentDataType.BYTE);

      // No state has yet been manually chosen - consider auto-advertise.
      // If a user has chosen to hide their shop, it shall stay hidden.
      if (value == null) {
        if (allowState == AdvertiseAllow.ALLOWED) {
          if (config.rootSection.worldGuardIntegration.autoAdvertiseIfInAllowList) {
            this.advertising = true;
            return;
          }
        }
      }

      this.advertising = value != null && value == 1;
    });
  }

  private boolean tryAccessDataContainer(Consumer<PersistentDataContainer> handler) {
    var signs = handle.getSigns();

    if (signs.isEmpty()) {
      var location = handle.getLocation();
      var locationWorld = location.getWorld();

      logger.severe(
        "Encountered a shop without its corresponding sign at (" +
          location.getBlockX() + ", " +
          location.getBlockY() + ", " +
          location.getBlockZ() + ") in world \"" +
          (locationWorld == null ? "null" : locationWorld.getName()) + "\""
      );
      return false;
    }

    handler.accept(signs.get(0).getPersistentDataContainer());
    return true;
  }

  private AdvertiseAllow getAdvertiseAllowState() {
    var worldGuardIntegration = integrationRegistry.getWorldGuardIntegration();

    if (worldGuardIntegration == null)
      return AdvertiseAllow.UNSET;

    var regionsAllowList = config.rootSection.worldGuardIntegration.advertiseIdsAllowList;

    if (regionsAllowList.isEmpty())
      return AdvertiseAllow.UNSET;

    var shopRegions = worldGuardIntegration.getRegionsContainingLocation(handle.getLocation());

    if (IWorldGuardIntegration.isRegionAllowed(regionsAllowList, shopRegions))
      return AdvertiseAllow.ALLOWED;

    return AdvertiseAllow.DISALLOWED;
  }
}
