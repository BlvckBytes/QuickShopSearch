package me.blvckbytes.quick_shop_search.config.teleport_display;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

@CSAlways
public class TeleportDisplayItemsSection extends ConfigSection {

  public GuiItemStackSection back;
  public GuiItemStackSection shopLocation;
  public GuiItemStackSection nearestPlayerWarpLocation;
  public GuiItemStackSection nearestEssentialsWarpLocation;
  public GuiItemStackSection filler;

  public TeleportDisplayItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
