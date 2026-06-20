package me.blvckbytes.quick_shop_search.textures;

import com.google.gson.*;
import me.blvckbytes.quick_shop_search.OfflinePlayerRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class TexturesResolver implements Listener {

  private static final long CACHE_SAVE_PERIOD_T = 20 * 60 * 15;

  private final Plugin plugin;
  private final OfflinePlayerRegistry offlinePlayerRegistry;
  private final HttpClient httpClient;
  private final Gson gson;
  private final Map<UUID, CachedTextures> texturesByOwnerId;
  private final Set<UUID> currentlyResolvedOwnerIds;

  private final File cacheFile;

  public TexturesResolver(
    Plugin plugin,
    OfflinePlayerRegistry offlinePlayerRegistry
  ) throws Exception {
    this.plugin = plugin;
    this.offlinePlayerRegistry = offlinePlayerRegistry;
    this.httpClient = HttpClient.newHttpClient();
    this.gson = new GsonBuilder().create();
    this.texturesByOwnerId = new ConcurrentHashMap<>();
    this.currentlyResolvedOwnerIds = new HashSet<>();

    this.cacheFile = new File(plugin.getDataFolder(), "textures-cache.json");

    if (!cacheFile.exists()) {
      if (!cacheFile.createNewFile())
        throw new IllegalStateException("Could not create file " + cacheFile);
    }
    else if (!cacheFile.isFile())
      throw new IllegalStateException("Expected file at " + cacheFile);

    loadCache();

    Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveCache, CACHE_SAVE_PERIOD_T, CACHE_SAVE_PERIOD_T);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Bukkit.getScheduler().runTaskLater(plugin, () -> tryUpdateSkinFromOnlinePlayer(event.getPlayer()), 20);
  }

  private void updateCacheEntry(CachedTextures newEntry) {
    var existingEntry = texturesByOwnerId.get(newEntry.ownerId());

    texturesByOwnerId.put(newEntry.ownerId(), newEntry);

    if (existingEntry != null && existingEntry.doPropertiesEqual(newEntry))
      return;

    Bukkit.getPluginManager().callEvent(new CachedTexturesUpdateEvent(newEntry));
  }

  private void tryUpdateSkinFromOnlinePlayer(Player player) {
    if (!player.isOnline())
      return;

    var skinUrl = player.getPlayerProfile().getTextures().getSkin();

    if (skinUrl == null)
      return;

    updateCacheEntry(new CachedTextures(player.getName(), player.getUniqueId(), skinUrlToBase64(skinUrl), System.currentTimeMillis()));
  }

  private String skinUrlToBase64(URL skinUrl) {
    return new String(Base64.getEncoder().encode(("{\"textures\":{\"SKIN\":{\"url\": \"" + skinUrl + "\"}}}").getBytes()));
  }

  public @Nullable CachedTextures tryResolveCachedTextures(String ownerName) {
    if (ownerName == null)
      return null;

    var ownerPlayer = offlinePlayerRegistry.getByName(ownerName);

    if (ownerPlayer == null)
      return null;

    var ownerId = ownerPlayer.getUniqueId();

    var result = texturesByOwnerId.get(ownerId);

    if (result != null) {
      if (result.shouldBeUpdated())
        tryUpdateCachedTexturesAsync(ownerName, ownerId);

      return result;
    }

    tryUpdateCachedTexturesAsync(ownerName, ownerId);
    return null;
  }

  private void tryUpdateCachedTexturesAsync(String ownerName, UUID ownerId) {
    if (!currentlyResolvedOwnerIds.add(ownerId))
      return;

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      var skinUrl = tryResolveSkinUrl(ownerId);

      synchronized (currentlyResolvedOwnerIds) {
        currentlyResolvedOwnerIds.remove(ownerId);
      }

      if (skinUrl == null)
        return;

      var cachedTextures = new CachedTextures(ownerName, ownerId, skinUrlToBase64(skinUrl), System.currentTimeMillis());

      Bukkit.getScheduler().runTask(plugin, () -> updateCacheEntry(cachedTextures));
    });
  }

  private @Nullable URL tryResolveSkinUrl(UUID ownerId) {
    try {
      var ownerIdString = ownerId.toString().replace("-", "");

      var httpRequest = HttpRequest
        .newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + ownerIdString))
        .GET()
        .build();

      var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

      // Let's not spam the console with full stack-traces when rate-limited or when encountering outages.
      // All other exceptions are fine to be logged in full, as they should never occur, except for when their API changes.
      if (response.statusCode() != 200) {
        plugin.getLogger().warning("Expected status-code 200, but got " + response.statusCode() + " for attempt of fetching textures for " + ownerId);
        return null;
      }

      var responseBody = response.body();
      var responseJson = gson.fromJson(responseBody, JsonObject.class);

      if (!(responseJson.get("properties") instanceof JsonArray propertiesArray))
        throw new IllegalStateException("Expected key \"properties\" to be of type array");

      String texturesValue = null;

      for (var property : propertiesArray) {
        if (!(property instanceof JsonObject propertyObject))
          throw new IllegalStateException("Expected item of \"properties\"-array to be an object");

        if (!(propertyObject.get("name") instanceof JsonPrimitive namePrimitive))
          throw new IllegalStateException("Expected \"name\" of property within \"properties\"-array to be a string");

        if (!"textures".equals(namePrimitive.getAsString()))
          continue;

        if (!(propertyObject.get("value") instanceof JsonPrimitive valuePrimitive))
          throw new IllegalStateException("Expected \"value\" of property within \"properties\"-array to be a string");

        texturesValue = valuePrimitive.getAsString();
        break;
      }

      if (texturesValue == null)
        throw new IllegalStateException("Could not locate \"textures\"-property within \"properties\"-array");

      String decodedValue;

      try {
        decodedValue = new String(Base64.getDecoder().decode(texturesValue));
      } catch (Throwable e) {
        throw new IllegalStateException("Received invalid base64: " + texturesValue, e);
      }

      if (!(gson.fromJson(decodedValue, JsonElement.class) instanceof JsonObject valueObject))
        throw new IllegalStateException("Expected decoded base64 \"" + texturesValue + "\" to be a json-object");

      if (!(valueObject.get("textures") instanceof JsonObject texturesObject))
        throw new IllegalStateException("Expected decoded base64 \"" + texturesValue + "\" to contain a \"textures\" json-object");

      if (!(texturesObject.get("SKIN") instanceof JsonObject skinObject))
        throw new IllegalStateException("Expected decoded base64 \"" + texturesValue + "\" to contain a \"textures\".\"SKIN\" json-object");

      if (!(skinObject.get("url") instanceof JsonPrimitive urlPrimitive))
        throw new IllegalStateException("Expected decoded base64 \"" + texturesValue + "\" to contain a \"textures\".\"SKIN\".\"url\" json-primitive");

      return URI.create(urlPrimitive.getAsString()).toURL();
    } catch (Throwable e) {
      plugin.getLogger().log(Level.SEVERE, "Could not dispatch a request to Mojang's profile-API in order to retrieve skin-textures", e);
    }

    return null;
  }

  public void saveCache() {
    try (var writer = new FileWriter(cacheFile)) {
      var cacheEntries = new JsonArray();

      for (var cachedTexture : texturesByOwnerId.values())
        cacheEntries.add(cachedTexture.toJson());

      gson.toJson(cacheEntries, writer);
    } catch (Throwable e) {
      plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to save the textures-cache to disk", e);
    }
  }

  private void loadCache() throws Exception {
    if (cacheFile.length() == 0)
      return;

    var loadCounter = 0;

    try (var reader = new FileReader(cacheFile)) {
      for (var arrayItem : gson.fromJson(reader, JsonArray.class)) {
        if (!(arrayItem instanceof JsonObject jsonObject))
          continue;

        CachedTextures textures;

        try {
          textures = CachedTextures.fromJson(jsonObject);
        } catch (Throwable e) {
          plugin.getLogger().log(Level.WARNING, "An error occurred while trying to load a textures cache-entry", e);
          continue;
        }

        texturesByOwnerId.put(textures.ownerId(), textures);
        ++loadCounter;
      }
    }

    plugin.getLogger().info("Loaded " + loadCounter + " cached textures from disk");
  }
}