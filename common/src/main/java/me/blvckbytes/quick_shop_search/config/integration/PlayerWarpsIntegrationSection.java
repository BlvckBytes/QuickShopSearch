package me.blvckbytes.quick_shop_search.config.integration;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class PlayerWarpsIntegrationSection extends AConfigSection {

  public int nearestWarpBlockRadius;
  public boolean enabled;
  public boolean displayNearestInIcon;
  public int updatePeriodSeconds;
  public boolean withinSameRegion;

  @CSAlways
  public PlayerWarpsTeleportCommandsSection teleportCommand;

  public PlayerWarpsIntegrationSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.enabled = false;
    this.nearestWarpBlockRadius = 15;
    this.displayNearestInIcon = false;
    this.updatePeriodSeconds = 30;
    this.withinSameRegion = false;
  }
}
