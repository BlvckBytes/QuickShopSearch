package me.blvckbytes.quick_shop_search.config.integration;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class PlayerWarpsTeleportCommandsSection extends ConfigSection {

  public ComponentMarkup revivalo;
  public ComponentMarkup olzieDev;
  public ComponentMarkup ax;

  public PlayerWarpsTeleportCommandsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
