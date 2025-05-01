package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import javax.annotation.Nullable;
import java.util.List;

public class SearchFlagSection extends AConfigSection {

  public @Nullable String name;
  public @Nullable List<String> suggestions;

  public SearchFlagSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
