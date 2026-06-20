package me.blvckbytes.quick_shop_search.textures;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.UUID;

public record CachedTextures(
  String ownerName,
  UUID ownerId,
  String textures,
  long fetchedAt
) {

  public static final long CACHE_TIME_MS = 1000 * 60 * 60 * 24;

  public boolean shouldBeUpdated() {
    return System.currentTimeMillis() - fetchedAt > CACHE_TIME_MS;
  }

  public static CachedTextures fromJson(JsonObject object) {
    if (!(object.get("ownerName") instanceof JsonPrimitive ownerNamePrimitive) || !ownerNamePrimitive.isString())
      throw new IllegalStateException("Expected \"ownerName\" to be a string");

    if (!(object.get("textures") instanceof JsonPrimitive texturesPrimitive) || !texturesPrimitive.isString())
      throw new IllegalStateException("Expected \"textures\" to be a string");

    if (!(object.get("ownerId") instanceof JsonPrimitive ownerIdPrimitive) || !ownerIdPrimitive.isString())
      throw new IllegalStateException("Expected \"ownerId\" to be a string");

    if (!(object.get("fetchedAt") instanceof JsonPrimitive fetchedAtPrimitive) || !fetchedAtPrimitive.isNumber())
      throw new IllegalStateException("Expected \"fetchedAt\" to be a string");

    return new CachedTextures(
      ownerNamePrimitive.getAsString(),
      UUID.fromString(ownerIdPrimitive.getAsString()),
      texturesPrimitive.getAsString(),
      fetchedAtPrimitive.getAsLong()
    );
  }

  public JsonObject toJson() {
    var result = new JsonObject();

    result.addProperty("ownerName", ownerName);
    result.addProperty("ownerId", ownerId.toString());
    result.addProperty("textures", textures);
    result.addProperty("fetchedAt", fetchedAt);

    return result;
  }
}
