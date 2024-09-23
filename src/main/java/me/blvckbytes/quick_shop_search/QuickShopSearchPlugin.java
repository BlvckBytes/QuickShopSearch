package me.blvckbytes.quick_shop_search;

import com.ghostchu.quickshop.api.QuickShopAPI;
import me.blvckbytes.bukkitevaluable.ConfigManager;
import me.blvckbytes.item_predicate_parser.ItemPredicateParserPlugin;
import me.blvckbytes.item_predicate_parser.parse.PredicateParserFactory;
import me.blvckbytes.quick_shop_search.config.MainSection;
import me.blvckbytes.quick_shop_search.display.ResultDisplayHandler;
import me.blvckbytes.quick_shop_search.display.SelectionStateStore;
import org.bukkit.Bukkit;
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
      var configManager = new ConfigManager(this);
      var configMapper = configManager.loadConfig("config.yml");
      var mainSection = configMapper.mapSection(null, MainSection.class);

      var parserPlugin = ItemPredicateParserPlugin.getInstance();

      if (parserPlugin == null)
        throw new IllegalStateException("Depending on ItemPredicateParser to be successfully loaded");

      var translationLanguage = mainSection.predicates.language;
      logger.info("Using language " + translationLanguage.assetFileNameWithoutExtension + " for predicate parsing");

      var translationRegistry = parserPlugin.getLanguageRegistry().getTranslationRegistry(translationLanguage);
      var parserFactory = new PredicateParserFactory(translationRegistry);

      var quickShopApi = QuickShopAPI.getInstance();
      var shopRegistry = new CachedShopRegistry(logger, quickShopApi.getShopManager(), mainSection);

      Bukkit.getPluginManager().registerEvents(shopRegistry, this);

      stateStore = new SelectionStateStore(this);
      displayHandler = new ResultDisplayHandler(this, mainSection, stateStore);

      Bukkit.getPluginManager().registerEvents(displayHandler, this);

      Objects.requireNonNull(getCommand("quickshopsearch")).setExecutor(
        new QuickShopSearchCommand(this, parserFactory, shopRegistry, mainSection, displayHandler)
      );
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