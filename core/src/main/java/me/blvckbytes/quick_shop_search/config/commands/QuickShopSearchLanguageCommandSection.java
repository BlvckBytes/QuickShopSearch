package me.blvckbytes.quick_shop_search.config.commands;

import me.blvckbytes.bukkitevaluable.section.ACommandSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class QuickShopSearchLanguageCommandSection extends ACommandSection {

  public static final String INITIAL_NAME = "quickshopsearchlanguage";

  public QuickShopSearchLanguageCommandSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(INITIAL_NAME, baseEnvironment);
  }
}
