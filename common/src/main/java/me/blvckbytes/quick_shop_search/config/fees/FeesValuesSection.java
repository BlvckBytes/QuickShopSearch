package me.blvckbytes.quick_shop_search.config.fees;

import me.blvckbytes.bbconfigmapper.MappingError;
import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.List;

public class FeesValuesSection extends AConfigSection {

  public double absoluteBuy;
  public double relativeBuy;
  public double absoluteSell;
  public double relativeSell;

  public int priority;

  public FeesValuesSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (relativeBuy < 0 || relativeBuy > 100)
      throw new MappingError("Property \"relativeBuy\" is not within the range of 0 to 100 (percent)");

    if (relativeSell < 0 || relativeSell > 100)
      throw new MappingError("Property \"relativeSell\" is not within the range of 0 to 100 (percent)");

    if (absoluteBuy < 0)
      throw new MappingError("Property \"absoluteBuy\" cannot be less than zero");

    if (absoluteSell < 0)
      throw new MappingError("Property \"absoluteSell\" cannot be less than zero");
  }
}
