package me.blvckbytes.quick_shop_search.config.integration;

import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class EssentialsWarpsIntegrationSection extends ConfigSection {

  public int nearestWarpBlockRadius;
  public boolean enabled;
  public boolean displayNearestInIcon;
  public boolean withinSameRegion;

  public EssentialsWarpsIntegrationSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this.enabled = false;
    this.nearestWarpBlockRadius = 15;
    this.displayNearestInIcon = false;
    this.withinSameRegion = false;
  }
}
