package me.blvckbytes.quick_shop_search;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UidScopedNamedStampStore {

  private static final Gson GSON_INSTANCE = new GsonBuilder().setPrettyPrinting().create();

  private final File dataFile;
  private final Logger logger;

  private final Map<UUID, Map<String, Long>> namedStampsByUid;
  private boolean isDataDirty;

  public UidScopedNamedStampStore(Plugin plugin, Logger logger) {
    this.dataFile = new File(plugin.getDataFolder(), "cooldown_persistence.json");
    this.logger = logger;
    this.namedStampsByUid = new HashMap<>();

    loadFromDisk();

    Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveToDisk, 0L, 20L * 5);
  }

  public void write(UUID id, String name, long value) {
    isDataDirty = true;

    namedStampsByUid
      .computeIfAbsent(id, k -> new HashMap<>())
      .put(name.toLowerCase(), value);
  }

  /**
   * @return -1 if non-existent; value >= 0 otherwise
   */
  public long read(UUID id, String name) {
    var stampByName = namedStampsByUid.get(id);

    if (stampByName == null)
      return -1;

    return stampByName.getOrDefault(name.toLowerCase(), -1L);
  }

  public void onShutdown() {
    saveToDisk();
  }

  private void loadFromDisk() {
    if (!dataFile.isFile())
      return;

    try (
      var reader = new FileReader(dataFile, Charsets.UTF_8);
    ) {
      var jsonData = GSON_INSTANCE.fromJson(reader, JsonObject.class);

      if (jsonData == null)
        return;

      for (var jsonEntry : jsonData.entrySet()) {
        var idString = jsonEntry.getKey();

        UUID id;

        try {
          id = UUID.fromString(idString);
        } catch (Exception e) {
          logger.log(Level.WARNING, "Could not parse ID \"" + idString + "\" of state-file at " + dataFile, e);
          continue;
        }

        if (!(jsonEntry.getValue() instanceof JsonObject stampByNameObject)) {
          logger.warning("Value at ID \"" + idString + "\" of state-file at " + dataFile + " was not a map");
          continue;
        }

        var stampByName = new HashMap<String, Long>();

        for (var stampEntry : stampByNameObject.entrySet()) {
          var name = stampEntry.getKey();

          if (!(stampEntry.getValue() instanceof JsonPrimitive valuePrimitive)) {
            logger.warning("Value at ID \"" + idString + "\" and name \"" + name + "\" of state-file at " + dataFile + " was not a primitive");
            continue;
          }

          long value;

          try {
            value = valuePrimitive.getAsLong();
          } catch (Exception e) {
            logger.warning("Value at ID \"" + idString + "\" and name \"" + name + "\" of state-file at " + dataFile + " was malformed");
            continue;
          }

          stampByName.put(name, value);
        }

        namedStampsByUid.put(id, stampByName);
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not read state-file at " + dataFile, e);
    }
  }

  private void saveToDisk() {
    if (!isDataDirty)
      return;

    isDataDirty = false;

    try (
      var fileWriter = new FileWriter(dataFile, Charsets.UTF_8);
      var jsonWriter = new JsonWriter(fileWriter);
    ) {
      GSON_INSTANCE.toJson(this.namedStampsByUid, Map.class, jsonWriter);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not write state-file to " + dataFile, e);
    }
  }
}
