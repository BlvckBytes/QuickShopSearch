package me.blvckbytes.quick_shop_search.config.teleport_display;

import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.config.display_common.GuiSection;

public class TeleportDisplaySection extends GuiSection<TeleportDisplayItemsSection> {

  public TeleportDisplaySection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(TeleportDisplayItemsSection.class, baseEnvironment);
  }
}
