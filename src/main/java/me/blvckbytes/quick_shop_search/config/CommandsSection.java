package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

@CSAlways
public class CommandsSection extends AConfigSection {

  public QuickShopSearchCommandSection quickShopSearch;
  public QuickShopSearchLanguageCommandSection quickShopSearchLanguage;
  public QuickShopSearchReloadCommandSection quickShopSearchReload;

  public CommandsSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
