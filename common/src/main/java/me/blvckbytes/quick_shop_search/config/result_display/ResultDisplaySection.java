package me.blvckbytes.quick_shop_search.config.result_display;

import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.config.display_common.PaginatedGuiSection;

public class ResultDisplaySection extends PaginatedGuiSection<ResultDisplayItemsSection> {

  public String chatPromptAllSentinel;
  public String chatPromptCancelSentinel;

  public ResultDisplaySection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(ResultDisplayItemsSection.class, baseEnvironment);

    this.chatPromptAllSentinel = "all";
    this.chatPromptCancelSentinel = "cancel";
  }
}
