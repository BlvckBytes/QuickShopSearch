package me.blvckbytes.quick_shop_search.display;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.blvckbytes.quick_shop_search.PluginPermission;
import org.apache.commons.io.FileUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SelectionStateStore {

  // JSON seems more than good enough for a state-store this simple and concise
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  private final Map<UUID, SelectionState> states;
  private final File stateFile;
  private final Logger logger;

  public SelectionStateStore(Plugin plugin, Logger logger) throws Exception {
    this.logger = logger;
    this.states = new HashMap<>();
    this.stateFile = new File(plugin.getDataFolder(), "selection_states.json");

    if (!this.stateFile.exists()) {
      if (!this.stateFile.createNewFile())
        throw new IllegalStateException("Could not create file " + stateFile);
    } else if (!this.stateFile.isFile())
      throw new IllegalStateException("Expected file at location " + stateFile);

    this.load();
  }

  public SelectionState loadState(Player player) {
    var result = this.states.get(player.getUniqueId());

    if (result == null) {
      result = new SelectionState();
      this.states.put(player.getUniqueId(), result);
      return result;
    }

    // Make sure that the player's not stuck with any unchangeable sorting- or filtering-setup

    if (!PluginPermission.FEATURE_SORT.has(player))
      result.resetSorting();

    if (!PluginPermission.FEATURE_FILTER.has(player))
      result.resetFiltering();

    return result;
  }

  public void onShutdown() {
    try {
      var stateData = new JsonObject();

      for (var state : this.states.entrySet())
        stateData.add(state.getKey().toString(), state.getValue().toJson());

      FileUtils.writeStringToFile(stateFile, gson.toJson(stateData), StandardCharsets.UTF_8);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not write state-file to " + stateFile, e);
    }
  }

  private void load() throws Exception {
    var stateData = gson.fromJson(new FileReader(stateFile), JsonObject.class);

    if (stateData == null)
      return;

    for (var entry : stateData.entrySet()) {
      var uuid = UUID.fromString(entry.getKey());

      if (!(entry.getValue() instanceof JsonObject object))
        continue;

      SelectionState state;

      try {
        state = SelectionState.fromJson(object);
      } catch (Exception e) {
        logger.warning("Could not load selection state: " + e.getMessage() + "; falling back to defaults");
        state = new SelectionState();
      }

      this.states.put(uuid, state);
    }
  }
}
