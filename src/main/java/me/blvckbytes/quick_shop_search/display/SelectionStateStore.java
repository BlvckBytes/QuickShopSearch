package me.blvckbytes.quick_shop_search.display;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionStateStore {

  // JSON seems more than good enough for a state-store this simple and concise
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  private final Map<UUID, SelectionState> states;
  private final File stateFile;

  public SelectionStateStore(Plugin plugin) throws Exception {
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
    return this.states.computeIfAbsent(player.getUniqueId(), k -> new SelectionState());
  }

  public void onShutdown() throws Exception {
    var stateData = new JsonObject();

    for (var state : this.states.entrySet())
      stateData.add(state.getKey().toString(), state.getValue().toJson());

    FileUtils.writeStringToFile(stateFile, gson.toJson(stateData), StandardCharsets.UTF_8);
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
        state = new SelectionState();
      }

      this.states.put(uuid, state);
    }
  }
}
