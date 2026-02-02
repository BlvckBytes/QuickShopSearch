package me.blvckbytes.quick_shop_search.config.slow_teleport;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import javax.annotation.Nullable;

public class SlotsMessagesSection extends ConfigSection {

  public @Nullable ComponentMarkup messageTitle;
  public @Nullable ComponentMarkup messageSubTitle;
  public @Nullable ComponentMarkup messageActionBar;
  public @Nullable ComponentMarkup messageChat;

  public SlotsMessagesSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
