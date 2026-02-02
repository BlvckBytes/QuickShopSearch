package me.blvckbytes.quick_shop_search.config.commands;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.util.HashMap;
import java.util.Map;

public class QuickShopSearchCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "quickshopsearch";

  public Map<String, SearchFlagSection> searchFlags;

  public QuickShopSearchCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);

    this.searchFlags = new HashMap<>();
  }
}
