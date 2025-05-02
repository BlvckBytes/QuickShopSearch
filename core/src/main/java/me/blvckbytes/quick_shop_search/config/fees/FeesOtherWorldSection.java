package me.blvckbytes.quick_shop_search.config.fees;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

@CSAlways
public class FeesOtherWorldSection extends AConfigSection {

  public FeesValuesSection worldsFallback;

  public Map<String, FeesValuesSection> worlds;

  public FeesOtherWorldSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.worlds = new LinkedHashMap<>();
  }
}
