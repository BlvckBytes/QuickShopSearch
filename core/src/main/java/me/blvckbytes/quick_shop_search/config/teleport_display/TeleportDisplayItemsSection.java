package me.blvckbytes.quick_shop_search.config.teleport_display;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.config.display_common.GuiItemStackSection;

@CSAlways
public class TeleportDisplayItemsSection extends AConfigSection {

  public GuiItemStackSection back;
  public GuiItemStackSection shopLocation;
  public GuiItemStackSection nearestPlayerWarpLocation;
  public GuiItemStackSection nearestEssentialsWarpLocation;
  public GuiItemStackSection filler;

  public TeleportDisplayItemsSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
