package me.blvckbytes.quick_shop_search;

import com.cryptomorin.xseries.XMaterial;
import com.tcoded.folialib.FoliaLib;
import me.blvckbytes.bukkitevaluable.CommandUpdater;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.bukkitevaluable.ConfigManager;
import me.blvckbytes.item_predicate_parser.ItemPredicateParserPlugin;
import me.blvckbytes.quick_shop_search.cache.CachedShopRegistry;
import me.blvckbytes.quick_shop_search.cache.OfflinePlayerCache;
import me.blvckbytes.quick_shop_search.cache.QuickShopListenerFactory;
import me.blvckbytes.quick_shop_search.command.*;
import me.blvckbytes.quick_shop_search.command.QuickShopSearchCommand;
import me.blvckbytes.quick_shop_search.config.*;
import me.blvckbytes.quick_shop_search.display.ResultDisplayHandler;
import me.blvckbytes.quick_shop_search.display.SelectionStateStore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

public class QuickShopSearchPlugin extends JavaPlugin {

  private ResultDisplayHandler displayHandler;
  private SelectionStateStore stateStore;

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

      stateStore = new SelectionStateStore(this, logger);
      displayHandler = new ResultDisplayHandler(scheduler, config, stateStore, chatPromptManager);

      var shopRegistry = new CachedShopRegistry(this, scheduler, displayHandler, config, logger);

      var shopEventHandler = QuickShopListenerFactory.create(logger, shopRegistry);

      Bukkit.getPluginManager().registerEvents(shopEventHandler, this);
      Bukkit.getPluginManager().registerEvents(displayHandler, this);

      var offlinePlayerCache = new OfflinePlayerCache();
      Bukkit.getPluginManager().registerEvents(offlinePlayerCache, this);

      var commandUpdater = new CommandUpdater(this);
      var commandExecutor = new QuickShopSearchCommand(config);

      commandExecutor.registerSubCommand(new AdvertiseMultiSubCommand(shopRegistry, offlinePlayerCache, config));
      commandExecutor.registerSubCommand(new AdvertiseSubCommand(shopRegistry, config));
      commandExecutor.registerSubCommand(new GlobalSubCommand(displayHandler, parserPlugin.getPredicateHelper(), shopRegistry, config));
      commandExecutor.registerSubCommand(new NearSubCommand(displayHandler, parserPlugin.getPredicateHelper(), shopRegistry, config));
      commandExecutor.registerSubCommand(new PlayerSubCommand(offlinePlayerCache, displayHandler, parserPlugin.getPredicateHelper(), shopRegistry, config));
      commandExecutor.registerSubCommand(new ReloadSubCommand(logger, config));
      commandExecutor.registerSubCommand(new AboutSubCommand(this, config));

      var mainCommand = Objects.requireNonNull(getCommand(QuickShopSearchCommandSection.INITIAL_NAME));

      mainCommand.setExecutor(commandExecutor);

      Runnable updateCommands = () -> {
        config.rootSection.commands.quickShopSearch.apply(mainCommand, commandUpdater);

        commandUpdater.trySyncCommands();
      };

      updateCommands.run();
      config.registerReloadListener(updateCommands);

      Bukkit.getPluginManager().registerEvents(new CommandSendListener(this, config), this);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not initialize plugin", e);
      Bukkit.getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    if (displayHandler != null)
      displayHandler.onShutdown();

    if (stateStore != null) {
      try {
        stateStore.onShutdown();
      } catch (Exception e) {
        getLogger().log(Level.SEVERE, "Could not save state-store", e);
      }
    }
  }
}