package me.blvckbytes.quick_shop_search.config.integration;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class WorldGuardIntegrationSection extends AConfigSection {

  public boolean enabled;

  public WorldGuardIntegrationSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.enabled = false;
  }
}
