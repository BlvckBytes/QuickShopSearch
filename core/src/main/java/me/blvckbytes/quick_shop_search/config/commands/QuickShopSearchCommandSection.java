package me.blvckbytes.quick_shop_search.config.commands;

import me.blvckbytes.bbconfigmapper.MappingError;
import me.blvckbytes.bukkitevaluable.section.ACommandSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.command.SearchFlag;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuickShopSearchCommandSection extends ACommandSection {

  public static final String INITIAL_NAME = "quickshopsearch";

  public Map<String, SearchFlagSection> searchFlags;

  public QuickShopSearchCommandSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(INITIAL_NAME, baseEnvironment);

    this.searchFlags = new HashMap<>();
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var searchFlagEntry : searchFlags.entrySet()) {
      var searchFlagName = searchFlagEntry.getKey();

      var searchFlag = SearchFlag.getByName(searchFlagName);

      if (searchFlag == null)
        throw new MappingError("Unknown search-flag \"searchFlags." + searchFlagName + "\"");

      var searchFlagValue = searchFlagEntry.getValue();

      if (searchFlagValue.name != null)
        SearchFlag.matcher.getNormalizedConstant(searchFlag).setName(searchFlagValue.name);

      searchFlag.setSuggestions(searchFlagValue.suggestions);
    }
  }
}
