package me.blvckbytes.quick_shop_search.config;

import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;

public class PredicatesSection extends ConfigSection {

  public TranslationLanguage mainLanguage;

  public PredicatesSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this.mainLanguage = TranslationLanguage.ENGLISH_US;
  }
}
