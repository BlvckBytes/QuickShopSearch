package me.blvckbytes.quick_shop_search.config.fees;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.util.LinkedHashMap;
import java.util.Map;

@CSAlways
public class FeesSection extends ConfigSection {

  public boolean enabled;

  public long feesPayBackTimeoutTicks;

  public FeesDistanceRangesSection worldsFallback;

  public FeesOtherWorldSection otherWorld;

  public Map<String, FeesDistanceRangesSection> worlds;

  public FeesSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this.enabled = false;
    this.worlds = new LinkedHashMap<>();
    this.feesPayBackTimeoutTicks = 40;
  }
}
