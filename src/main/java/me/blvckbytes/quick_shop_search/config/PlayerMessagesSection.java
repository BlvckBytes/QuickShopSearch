package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.Nullable;

public class PlayerMessagesSection extends AConfigSection {

  public @Nullable BukkitEvaluable emptyPredicate;
  public @Nullable BukkitEvaluable noMatches;
  public @Nullable BukkitEvaluable beforeQuerying;
  public @Nullable BukkitEvaluable unknownLanguageChat;
  public @Nullable BukkitEvaluable unknownLanguageActionBar;
  public @Nullable BukkitEvaluable missingLanguage;

  public PlayerMessagesSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
