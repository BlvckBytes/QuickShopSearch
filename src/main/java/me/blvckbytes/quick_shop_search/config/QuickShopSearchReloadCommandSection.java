package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bukkitevaluable.section.ACommandSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class QuickShopSearchReloadCommandSection extends ACommandSection {

  public static final String INITIAL_NAME = "quickshopsearchreload";

  public QuickShopSearchReloadCommandSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(INITIAL_NAME, baseEnvironment);
  }
}
