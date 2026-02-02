package me.blvckbytes.quick_shop_search.config.fees;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.util.LinkedHashMap;
import java.util.Map;

@CSAlways
public class FeesOtherWorldSection extends ConfigSection {

  public FeesValuesSection worldsFallback;

  public Map<String, FeesValuesSection> worlds;

  public FeesOtherWorldSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this.worlds = new LinkedHashMap<>();
  }
}
