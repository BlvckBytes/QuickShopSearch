package me.blvckbytes.quick_shop_search.config.commands;

import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import javax.annotation.Nullable;
import java.util.List;

public class SearchFlagSection extends ConfigSection {

  public @Nullable String name;
  public @Nullable List<String> suggestions;

  public SearchFlagSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
