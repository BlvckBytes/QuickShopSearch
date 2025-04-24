package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

@CSAlways
public class SlowTeleportSection extends AConfigSection {

  public boolean cancelIfDamagedByPlayer;
  public boolean cancelIfDamagedByNonPlayer;
  public int combatLogCoolOffDurationSeconds;

  public SlowTeleportParametersSection whenInCombat;
  public SlowTeleportParametersSection whenNotInCombat;

  public SlowTeleportSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.cancelIfDamagedByPlayer = true;
    this.cancelIfDamagedByNonPlayer = false;
    this.combatLogCoolOffDurationSeconds = 10;
  }
}
