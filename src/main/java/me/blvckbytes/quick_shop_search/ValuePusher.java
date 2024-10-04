package me.blvckbytes.quick_shop_search;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ValuePusher<T> {

  private T value;
  private final List<Consumer<T>> updateConsumers;

  public ValuePusher(T initialValue) {
    this.updateConsumers = new ArrayList<>();
    this.value = initialValue;
  }

  public ValuePusher<T> subscribeToUpdates(Consumer<T> consumer) {
    this.updateConsumers.add(consumer);
    return this;
  }

  public void push(T value) {
    this.value = value;

    for (Consumer<T> updateConsumer : updateConsumers)
      updateConsumer.accept(value);
  }

  public T get() {
    return this.value;
  }
}
