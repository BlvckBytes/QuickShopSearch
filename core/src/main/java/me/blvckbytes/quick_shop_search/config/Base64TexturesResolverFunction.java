package me.blvckbytes.quick_shop_search.config;

import com.google.gson.*;
import me.blvckbytes.gpeee.functions.AExpressionFunction;
import me.blvckbytes.gpeee.functions.ExpressionFunctionArgument;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import me.blvckbytes.quick_shop_search.OfflinePlayerRegistry;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Base64TexturesResolverFunction extends AExpressionFunction {

  private final Logger logger;
  private final OfflinePlayerRegistry offlinePlayerRegistry;
  private final HttpClient httpClient;
  private final Gson gson;
  private final Map<UUID, String> texturesByOwnerId;

  public Base64TexturesResolverFunction(Logger logger, OfflinePlayerRegistry offlinePlayerRegistry) {
    this.logger = logger;
    this.offlinePlayerRegistry = offlinePlayerRegistry;
    this.httpClient = HttpClient.newHttpClient();
    this.gson = new GsonBuilder().create();
    this.texturesByOwnerId = new HashMap<>();
  }

  @Override
  public Object apply(IEvaluationEnvironment iEvaluationEnvironment, List<@Nullable Object> args) {
    String ownerName = nullable(args, 0);

    if (ownerName == null)
      return null;

    var ownerPlayer = offlinePlayerRegistry.getByName(ownerName);

    if (ownerPlayer == null)
      return null;

    return texturesByOwnerId.computeIfAbsent(ownerPlayer.getUniqueId(), this::tryResolveTextures);
  }

  private @Nullable String tryResolveTextures(UUID ownerId) {
    try {
      var ownerIdString = ownerId.toString().replace("-", "");

      var httpRequest = HttpRequest
        .newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + ownerIdString))
        .GET()
        .build();

      var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200)
        throw new IllegalStateException("Expected status-code 200, but got " + response.statusCode());

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

      return texturesValue;
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "Could not dispatch a request to Mojang's profile-API in order to retrieve skin-textures", e);
    }

    return null;
  }

  @Override
  public @Nullable List<ExpressionFunctionArgument> getArguments() {
    return List.of(
      new ExpressionFunctionArgument("owner_name", "Name of the skull-owner", false, String.class)
    );
  }

  public void registerSelf(EvaluationEnvironmentBuilder environmentBuilder) {
    environmentBuilder.withFunction("resolve_b64_textures", this);
  }
}
