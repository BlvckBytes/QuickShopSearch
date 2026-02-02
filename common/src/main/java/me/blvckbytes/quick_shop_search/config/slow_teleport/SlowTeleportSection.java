package me.blvckbytes.quick_shop_search.config.slow_teleport;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

@CSAlways
public class SlowTeleportSection extends ConfigSection {

  public boolean cancelIfDamagedByPlayer;
  public boolean cancelIfDamagedByNonPlayer;
  public int combatLogCoolOffDurationSeconds;

  public SlowTeleportParametersSection whenInCombat;
  public SlowTeleportParametersSection whenNotInCombat;

  public SlowTeleportSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this.cancelIfDamagedByPlayer = true;
    this.cancelIfDamagedByNonPlayer = false;
    this.combatLogCoolOffDurationSeconds = 10;
  }
}
