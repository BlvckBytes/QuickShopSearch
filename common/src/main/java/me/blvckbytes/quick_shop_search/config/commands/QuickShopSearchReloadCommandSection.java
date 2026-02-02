package me.blvckbytes.quick_shop_search.config.commands;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class QuickShopSearchReloadCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "quickshopsearchreload";

  public QuickShopSearchReloadCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}
