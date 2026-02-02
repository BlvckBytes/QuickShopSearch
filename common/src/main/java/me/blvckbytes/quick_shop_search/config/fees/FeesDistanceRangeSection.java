package me.blvckbytes.quick_shop_search.config.fees;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CSAlways
public class FeesDistanceRangeSection extends ConfigSection {

  public long minDistance;
  public long maxDistance;

  public FeesValuesSection permissionNamesFallback;

  public Map<String, FeesValuesSection> permissionNames;

  public FeesDistanceRangeSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this.permissionNames = new LinkedHashMap<>();
  }

  @Override
  public void afterParsing(List<Field> fields) {
    if (minDistance < 0)
      throw new MappingError("The value of \"minDistance\" cannot be less than zero");

    if (maxDistance < 0)
      throw new MappingError("The value of \"maxDistance\" cannot be less than zero");

    if (maxDistance < minDistance)
      throw new MappingError("The value of \"maxDistance\" cannot be less than \"minDistance\"");
  }
}
