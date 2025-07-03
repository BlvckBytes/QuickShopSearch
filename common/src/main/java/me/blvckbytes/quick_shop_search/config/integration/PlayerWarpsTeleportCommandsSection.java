package me.blvckbytes.quick_shop_search.config.integration;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class PlayerWarpsTeleportCommandsSection extends AConfigSection {

  public BukkitEvaluable revivalo;
  public BukkitEvaluable olzieDev;
  public BukkitEvaluable ax;

  public PlayerWarpsTeleportCommandsSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
