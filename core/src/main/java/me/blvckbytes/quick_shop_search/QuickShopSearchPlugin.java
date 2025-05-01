package me.blvckbytes.quick_shop_search;

import com.cryptomorin.xseries.XMaterial;
import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.command.CommandContainer;
import com.tcoded.folialib.FoliaLib;
import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.CommandUpdater;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.bukkitevaluable.ConfigManager;
import me.blvckbytes.item_predicate_parser.ItemPredicateParserPlugin;
import me.blvckbytes.quick_shop_search.cache.CachedShopRegistry;
import me.blvckbytes.quick_shop_search.cache.QuickShopVersionDependentFactory;
import me.blvckbytes.quick_shop_search.command.QuickShopSearchCommand;
import me.blvckbytes.quick_shop_search.command.ReloadCommand;
import me.blvckbytes.quick_shop_search.config.*;
import me.blvckbytes.quick_shop_search.display.ResultDisplayHandler;
import me.blvckbytes.quick_shop_search.display.SelectionStateStore;
import me.blvckbytes.quick_shop_search.integration.player_warps.IPlayerWarpsIntegration;
import me.blvckbytes.quick_shop_search.integration.player_warps.PlayerWarpsIntegration;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

public class QuickShopSearchPlugin extends JavaPlugin {

  private ResultDisplayHandler displayHandler;
  private SelectionStateStore stateStore;
  private UidScopedNamedStampStore stampStore;

  @Override
  public void onEnable() {
    var logger = getLogger();

    try {
      // First invocation is quite heavy - warm up cache
      XMaterial.matchXMaterial(Material.AIR);

      var foliaLib = new FoliaLib(this);
      var scheduler = foliaLib.getScheduler();

      var configManager = new ConfigManager(this, "config");
      var config = new ConfigKeeper<>(configManager, "config.yml", MainSection.class);

      var parserPlugin = ItemPredicateParserPlugin.getInstance();

      if (parserPlugin == null)
        throw new IllegalStateException("Depending on ItemPredicateParser to be successfully loaded");

      logger.info("Using language " + config.rootSection.predicates.mainLanguage.assetFileNameWithoutExtension + " for predicate parsing");

      var chatPromptManager = new ChatPromptManager(scheduler);
      Bukkit.getServer().getPluginManager().registerEvents(chatPromptManager, this);

      var versionDependentFactory = new QuickShopVersionDependentFactory(logger);
      var remoteInteractionApi = versionDependentFactory.createInteractionApi();

      stateStore = new SelectionStateStore(this, logger);
      stampStore = new UidScopedNamedStampStore(this, logger);

      var slowTeleportManager = new SlowTeleportManager(scheduler, config);
      Bukkit.getServer().getPluginManager().registerEvents(slowTeleportManager, this);

      IPlayerWarpsIntegration playerWarpsIntegration = null;

      if (Bukkit.getPluginManager().isPluginEnabled("PlayerWarps")) {
        try {
          playerWarpsIntegration = new PlayerWarpsIntegration(logger, scheduler);
          Bukkit.getPluginManager().registerEvents(playerWarpsIntegration, this);
          logger.info("Successfully loaded the PlayerWarps-integration!");
        } catch (Exception e) {
          logger.log(Level.SEVERE, "Could not load PlayerWarps-integration!", e);
        }
      }

      displayHandler = new ResultDisplayHandler(
        scheduler,
        remoteInteractionApi,
        config,
        stateStore,
        stampStore,
        chatPromptManager,
        slowTeleportManager,
        playerWarpsIntegration
      );

      var shopRegistry = new CachedShopRegistry(this, scheduler, displayHandler, config, logger);
      var shopEventHandler = versionDependentFactory.createListener(shopRegistry);

      Bukkit.getPluginManager().registerEvents(shopRegistry, this);
      Bukkit.getPluginManager().registerEvents(shopEventHandler, this);
      Bukkit.getPluginManager().registerEvents(displayHandler, this);

      var offlinePlayerRegistry = new OfflinePlayerRegistry();
      Bukkit.getPluginManager().registerEvents(offlinePlayerRegistry, this);

      var commandUpdater = new CommandUpdater(this);
      var commandExecutor = new QuickShopSearchCommand(scheduler, parserPlugin.getPredicateHelper(), shopRegistry, config, displayHandler, offlinePlayerRegistry);

      var mainCommand = Objects.requireNonNull(getCommand(QuickShopSearchCommandSection.INITIAL_NAME));
      var languageCommand = Objects.requireNonNull(getCommand(QuickShopSearchLanguageCommandSection.INITIAL_NAME));
      var reloadCommand = Objects.requireNonNull(getCommand(QuickShopSearchReloadCommandSection.INITIAL_NAME));

      mainCommand.setExecutor(commandExecutor);
      languageCommand.setExecutor(commandExecutor);
      reloadCommand.setExecutor(new ReloadCommand(logger, config));

      Runnable updateCommands = () -> {
        config.rootSection.commands.quickShopSearch.apply(mainCommand, commandUpdater);
        config.rootSection.commands.quickShopSearchLanguage.apply(languageCommand, commandUpdater);
        config.rootSection.commands.quickShopSearchReload.apply(reloadCommand, commandUpdater);

        commandUpdater.trySyncCommands();
      };

      updateCommands.run();
      config.registerReloadListener(updateCommands);

      Bukkit.getPluginManager().registerEvents(new CommandSendListener(this, config), this);

      var advertiseCommandContainer = CommandContainer.builder()
        .prefix("advertise")
        .permission(PluginPermission.ADVERTISE_COMMAND.node)
        .executor(new SubCommand_Advertise(shopRegistry, config))
        .build();

      QuickShop.getInstance().getCommandManager().registerCmd(advertiseCommandContainer);

      // Set description afterward, because at the time of writing this, the current version of
      // QuickShop-Hikari annihilates the description when registering the container.
      advertiseCommandContainer.setDescription(locale -> {
        BukkitEvaluable description;

        if ((description = config.rootSection.playerMessages.commandAdvertiseDescription) != null)
          return Component.text(description.asScalar(ScalarType.STRING, config.rootSection.builtBaseEnvironment));

        return Component.text("Missing corresponding config-key");
      });
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not initialize plugin", e);
      Bukkit.getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    if (displayHandler != null)
      displayHandler.onShutdown();

    if (stateStore != null)
      stateStore.onShutdown();

    if (stampStore != null)
      stampStore.onShutdown();
  }
}