package me.blvckbytes.quick_shop_search.display;

import com.tcoded.folialib.impl.PlatformScheduler;

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

  private final PlatformScheduler scheduler;
  private final List<QueuedTask> taskQueue;

  public AsyncTaskQueue(PlatformScheduler scheduler) {
    this.scheduler = scheduler;
    this.taskQueue = new ArrayList<>();
  }

  public void enqueue(Runnable runnable) {
    synchronized (taskQueue) {
      taskQueue.add(new QueuedTask(runnable));
      processQueue();
    }
  }

  private void processQueue() {
    synchronized (taskQueue) {
      if (taskQueue.isEmpty())
        return;

      var task = taskQueue.get(0);

      // Task has already been dispatched and is still executing
      if (task.dispatched)
        return;

      task.dispatched = true;

      scheduler.runAsync(scheduleTask -> {
        task.runnable.run();

        synchronized (taskQueue) {
          taskQueue.remove(0);
        }

        processQueue();
      });
    }
  }
}
