package me.blvckbytes.quick_shop_search.config.commands;

import me.blvckbytes.bukkitevaluable.section.ACommandSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashMap;
import java.util.Map;

public class QuickShopSearchCommandSection extends ACommandSection {

  public static final String INITIAL_NAME = "quickshopsearch";

  public Map<String, SearchFlagSection> searchFlags;

  public QuickShopSearchCommandSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(INITIAL_NAME, baseEnvironment);

    this.searchFlags = new HashMap<>();
  }
}
