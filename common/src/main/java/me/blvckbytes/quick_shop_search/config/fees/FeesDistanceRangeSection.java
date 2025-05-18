package me.blvckbytes.quick_shop_search.config.fees;

import me.blvckbytes.bbconfigmapper.MappingError;
import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CSAlways
public class FeesDistanceRangeSection extends AConfigSection {

  public long minDistance;
  public long maxDistance;

  public FeesValuesSection permissionNamesFallback;

  public Map<String, FeesValuesSection> permissionNames;

  public FeesDistanceRangeSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.permissionNames = new LinkedHashMap<>();
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    if (minDistance < 0)
      throw new MappingError("The value of \"minDistance\" cannot be less than zero");

    if (maxDistance < 0)
      throw new MappingError("The value of \"maxDistance\" cannot be less than zero");

    if (maxDistance < minDistance)
      throw new MappingError("The value of \"maxDistance\" cannot be less than \"minDistance\"");
  }
}
