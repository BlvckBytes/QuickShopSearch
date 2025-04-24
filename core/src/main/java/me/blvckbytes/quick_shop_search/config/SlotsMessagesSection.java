package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import javax.annotation.Nullable;

public class SlotsMessagesSection extends AConfigSection {

  public @Nullable BukkitEvaluable messageTitle;
  public @Nullable BukkitEvaluable messageSubTitle;
  public @Nullable BukkitEvaluable messageActionBar;
  public @Nullable BukkitEvaluable messageChat;

  public SlotsMessagesSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
