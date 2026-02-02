package me.blvckbytes.quick_shop_search.config.result_display;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.item.ItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

@CSAlways
public class ResultDisplayItemsSection extends ConfigSection {

  public GuiItemStackSection previousPage;
  public GuiItemStackSection nextPage;
  public GuiItemStackSection sorting;
  public GuiItemStackSection filtering;
  public GuiItemStackSection activeSearch;
  public GuiItemStackSection filler;
  public ItemStackSection representativePatch;

  public ResultDisplayItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
