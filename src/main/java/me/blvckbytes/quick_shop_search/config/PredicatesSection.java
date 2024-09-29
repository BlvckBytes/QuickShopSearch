package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;

public class PredicatesSection extends AConfigSection {

  public TranslationLanguage language;

  public PredicatesSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.language = TranslationLanguage.ENGLISH_US;
  }
}
