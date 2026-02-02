package me.blvckbytes.quick_shop_search.config.result_display;

import at.blvckbytes.cm_mapper.section.gui.PaginatedGuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class ResultDisplaySection extends PaginatedGuiSection<ResultDisplayItemsSection> {

  public String chatPromptAllSentinel;
  public String chatPromptCancelSentinel;

  public ResultDisplaySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(ResultDisplayItemsSection.class, baseEnvironment, interpreterLogger);

    this.chatPromptAllSentinel = "all";
    this.chatPromptCancelSentinel = "cancel";
  }
}
