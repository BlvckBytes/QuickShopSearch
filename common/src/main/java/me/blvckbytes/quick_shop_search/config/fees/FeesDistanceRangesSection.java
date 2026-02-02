package me.blvckbytes.quick_shop_search.config.fees;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.util.List;

@CSAlways
public class FeesDistanceRangesSection extends ConfigSection {

  public List<FeesDistanceRangeSection> distanceRanges;

  public FeesDistanceRangeSection distanceRangesFallback;

  public FeesDistanceRangesSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
