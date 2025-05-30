package me.blvckbytes.quick_shop_search.config.integration;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class EssentialsWarpsIntegrationSection extends AConfigSection {

  public int nearestWarpBlockRadius;
  public boolean enabled;
  public boolean displayNearestInIcon;
  public boolean withinSameRegion;

  public EssentialsWarpsIntegrationSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.enabled = false;
    this.nearestWarpBlockRadius = 15;
    this.displayNearestInIcon = false;
    this.withinSameRegion = false;
  }
}
