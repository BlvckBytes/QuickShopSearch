package me.blvckbytes.quick_shop_search;

import com.tcoded.folialib.wrapper.task.WrappedTask;

import java.util.function.Consumer;

public record ChatPromptInstance(
  WrappedTask timeoutTask,
  Consumer<String> handler
) {}
