package me.blvckbytes.quick_shop_search.config.teleport_display;

import at.blvckbytes.cm_mapper.section.gui.GuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class TeleportDisplaySection extends GuiSection<TeleportDisplayItemsSection> {

  public TeleportDisplaySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(TeleportDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}
