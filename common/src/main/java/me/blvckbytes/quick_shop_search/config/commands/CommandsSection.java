package me.blvckbytes.quick_shop_search.config.commands;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

@CSAlways
public class CommandsSection extends ConfigSection {

  public QuickShopSearchCommandSection quickShopSearch;
  public QuickShopSearchLanguageCommandSection quickShopSearchLanguage;
  public QuickShopSearchReloadCommandSection quickShopSearchReload;

  public CommandsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
