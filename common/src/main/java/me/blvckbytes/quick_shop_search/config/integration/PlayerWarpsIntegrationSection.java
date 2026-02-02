package me.blvckbytes.quick_shop_search.config.integration;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class PlayerWarpsIntegrationSection extends ConfigSection {

  public int nearestWarpBlockRadius;
  public boolean enabled;
  public boolean displayNearestInIcon;
  public int updatePeriodSeconds;
  public boolean withinSameRegion;
  public boolean doNotUseSlowTeleport;

  @CSAlways
  public PlayerWarpsTeleportCommandsSection teleportCommand;

  public PlayerWarpsIntegrationSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this.enabled = false;
    this.nearestWarpBlockRadius = 15;
    this.displayNearestInIcon = false;
    this.updatePeriodSeconds = 30;
    this.withinSameRegion = false;
    this.doNotUseSlowTeleport = false;
  }
}
