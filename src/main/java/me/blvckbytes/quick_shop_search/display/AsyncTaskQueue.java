package me.blvckbytes.quick_shop_search.display;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class AsyncTaskQueue {

  private static class QueuedTask {
    Runnable runnable;
    boolean dispatched;

    public QueuedTask(Runnable runnable) {
      this.runnable = runnable;
      this.dispatched = false;
    }
  }

  private final Plugin plugin;
  private final List<QueuedTask> taskQueue;

  public AsyncTaskQueue(Plugin plugin) {
    this.plugin = plugin;
    this.taskQueue = new ArrayList<>();
  }

  public void enqueue(Runnable runnable) {
    synchronized (taskQueue) {
      if (taskQueue.isEmpty()) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        return;
      }

      taskQueue.add(new QueuedTask(runnable));
      processQueue();
    }
  }

  private void processQueue() {
    synchronized (taskQueue) {
      if (taskQueue.isEmpty())
        return;

      var task = taskQueue.getFirst();

      // Task has already been dispatched and is still executing
      if (task.dispatched)
        return;

      task.dispatched = true;

      Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
        task.runnable.run();

        synchronized (taskQueue) {
          taskQueue.removeFirst();
        }

        processQueue();
      });
    }
  }
}
