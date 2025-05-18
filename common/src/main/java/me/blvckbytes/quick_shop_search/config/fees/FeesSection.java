package me.blvckbytes.quick_shop_search.config.fees;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

@CSAlways
public class FeesSection extends AConfigSection {

  public boolean enabled;

  public long feesPayBackTimeoutTicks;

  public FeesDistanceRangesSection worldsFallback;

  public FeesOtherWorldSection otherWorld;

  public Map<String, FeesDistanceRangesSection> worlds;

  public FeesSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.enabled = false;
    this.worlds = new LinkedHashMap<>();
    this.feesPayBackTimeoutTicks = 40;
  }
}
