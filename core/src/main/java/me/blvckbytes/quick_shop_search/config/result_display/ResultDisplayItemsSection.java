package me.blvckbytes.quick_shop_search.config.result_display;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.bukkitevaluable.section.ItemStackSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

@CSAlways
public class ResultDisplayItemsSection extends AConfigSection {

  public GuiItemStackSection previousPage;
  public GuiItemStackSection nextPage;
  public GuiItemStackSection sorting;
  public GuiItemStackSection filtering;
  public GuiItemStackSection activeSearch;
  public GuiItemStackSection filler;
  public ItemStackSection representativePatch;

  public ResultDisplayItemsSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
