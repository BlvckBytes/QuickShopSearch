package me.blvckbytes.quick_shop_search.config.fees;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.List;

@CSAlways
public class FeesDistanceRangesSection extends AConfigSection {

  public List<FeesDistanceRangeSection> distanceRanges;

  public FeesDistanceRangeSection distanceRangesFallback;

  public FeesDistanceRangesSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.distanceRanges = new ArrayList<>();
  }
}
