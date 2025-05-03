package me.blvckbytes.quick_shop_search.config.slow_teleport;

import me.blvckbytes.bbconfigmapper.MappingError;
import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSIgnore;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlowTeleportParametersSection extends AConfigSection {

  public int durationSeconds;

  public @Nullable SlowTeleportNotification fallbackNotification;

  public Map<String, @Nullable SlowTeleportNotification> notificationAtSeconds;

  @CSIgnore
  public Map<Integer, @Nullable SlowTeleportNotification> _notificationAtSeconds;

  public SlowTeleportParametersSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.notificationAtSeconds = new HashMap<>();
    this._notificationAtSeconds = new HashMap<>();
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (durationSeconds < 0)
      throw new MappingError("Property \"durationSeconds\" is less than zero!");

    for (var entry : notificationAtSeconds.entrySet()) {
      String key = entry.getKey();
      int secondsValue;

      try {
        secondsValue = Integer.parseInt(key);
      } catch (NumberFormatException ignored) {
        throw new MappingError("Property \"notificationAtSeconds." + key + "\" is not a valid integer!");
      }

      if (secondsValue < 0)
        throw new MappingError("Property \"notificationAtSeconds." + key + "\" is less than zero!");

      if (_notificationAtSeconds.put(secondsValue, entry.getValue()) != null)
        throw new MappingError("Property \"notificationAtSeconds." + key + "\" is a duplicate!");
    }
  }
}
