package me.blvckbytes.quick_shop_search;

import com.cryptomorin.xseries.XMaterial;
import com.ghostchu.quickshop.api.QuickShopAPI;
import me.blvckbytes.bukkitevaluable.CommandUpdater;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.bukkitevaluable.ConfigManager;
import me.blvckbytes.item_predicate_parser.ItemPredicateParserPlugin;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.config.QuickShopSearchCommandSection;
import me.blvckbytes.quick_shop_search.config.QuickShopSearchLanguageCommandSection;
import me.blvckbytes.quick_shop_search.config.QuickShopSearchReloadCommandSection;
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

    // First invocation is quite heavy - warm up cache
    XMaterial.matchXMaterial(Material.AIR);

    try {
      var configManager = new ConfigManager(this);
      var config = new ConfigKeeper<>(configManager, "config.yml", MainSection.class);

      var parserPlugin = ItemPredicateParserPlugin.getInstance();

      if (parserPlugin == null)
        throw new IllegalStateException("Depending on ItemPredicateParser to be successfully loaded");

      logger.info("Using language " + config.rootSection.predicates.mainLanguage.assetFileNameWithoutExtension + " for predicate parsing");

      stateStore = new SelectionStateStore(this, logger);
      displayHandler = new ResultDisplayHandler(this, config, stateStore);

      var quickShopApi = QuickShopAPI.getInstance();
      var shopRegistry = new CachedShopRegistry(this, quickShopApi.getShopManager(), displayHandler, config);

      Bukkit.getPluginManager().registerEvents(shopRegistry, this);
      Bukkit.getPluginManager().registerEvents(displayHandler, this);

      var commandUpdater = new CommandUpdater(this);
      var commandExecutor = new QuickShopSearchCommand(this, parserPlugin.getPredicateHelper(), shopRegistry, config, displayHandler);

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