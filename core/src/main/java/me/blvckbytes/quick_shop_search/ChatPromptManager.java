package me.blvckbytes.quick_shop_search;

import com.tcoded.folialib.impl.PlatformScheduler;
import me.blvckbytes.item_predicate_parser.translation.resolver.TranslationResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatPromptManager implements Listener {

  private static final int TIMEOUT_DURATION_S = 15;

  private final Map<UUID, ChatPromptInstance> handlerByUuid;
  private final PlatformScheduler scheduler;

  public ChatPromptManager(PlatformScheduler scheduler) {
    this.handlerByUuid = new HashMap<>();
    this.scheduler = scheduler;
  }

  /**
   * @return True if a previous handler has been overwritten
   */
  public boolean register(Player player, Consumer<String> inputHandler, Runnable timeoutHandler) {
    synchronized (handlerByUuid) {
      var existed = removeAndCancelIfExists(player) != null;
      var playerId = player.getUniqueId();

      var removalTask = scheduler.runLater(() -> {
        synchronized (handlerByUuid) {
          if (handlerByUuid.remove(playerId) != null)
            timeoutHandler.run();
        }
      }, TIMEOUT_DURATION_S * 20L);

      handlerByUuid.put(playerId, new ChatPromptInstance(removalTask, inputHandler));

      return existed;
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onChat(AsyncPlayerChatEvent event) {
    ChatPromptInstance handler;

    synchronized (handlerByUuid) {
      handler = removeAndCancelIfExists(event.getPlayer());
    }

    // Might not be the most decoupled solution, but since QSS depends on IPP, we're fine.
    var sanitizedMessage = TranslationResolver.sanitize(event.getMessage());

    if (handler != null) {
      event.setCancelled(true);
      handler.handler().accept(sanitizedMessage);
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    synchronized (handlerByUuid) {
      removeAndCancelIfExists(event.getPlayer());
    }
  }

  private @Nullable ChatPromptInstance removeAndCancelIfExists(Player player) {
    var handler = handlerByUuid.remove(player.getUniqueId());

    if (handler != null)
      handler.timeoutTask().cancel();

    return handler;
  }
}
