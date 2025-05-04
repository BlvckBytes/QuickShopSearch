package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class PlayerWarpsIntegrationSection extends AConfigSection {

  public int nearestWarpBlockRadius;
  public boolean displayNearestInIcon;

  public PlayerWarpsIntegrationSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.nearestWarpBlockRadius = 15;
    this.displayNearestInIcon = false;
  }
}
