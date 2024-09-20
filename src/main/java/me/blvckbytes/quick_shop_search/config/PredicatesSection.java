package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PredicatesSection extends AConfigSection {

  public TranslationLanguage language;
  public @Nullable BukkitEvaluable expandedPreview;
  public @Nullable BukkitEvaluable maxCompletionsExceeded;
  public @Nullable BukkitEvaluable inputNonHighlightPrefix;
  public @Nullable BukkitEvaluable inputHighlightPrefix;
  public Map<String, BukkitEvaluable> parseConflicts;

  public PredicatesSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.language = TranslationLanguage.ENGLISH_US;
    this.parseConflicts = new HashMap<>();
  }
}
