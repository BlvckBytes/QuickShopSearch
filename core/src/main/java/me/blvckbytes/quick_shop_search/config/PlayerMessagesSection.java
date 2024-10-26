package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.Nullable;

public class PlayerMessagesSection extends AConfigSection {

  public @Nullable BukkitEvaluable emptyPredicate;
  public @Nullable BukkitEvaluable noMatches;
  public @Nullable BukkitEvaluable beforeQuerying;
  public @Nullable BukkitEvaluable beforeTeleporting;
  public @Nullable BukkitEvaluable queryingAllShops;
  public @Nullable BukkitEvaluable usageQsslCommandLanguage;
  public @Nullable BukkitEvaluable unknownLanguageActionBar;
  public @Nullable BukkitEvaluable pluginReloadedSuccess;
  public @Nullable BukkitEvaluable pluginReloadedError;
  public @Nullable BukkitEvaluable predicateParseError;

  public @Nullable BukkitEvaluable missingPermissionMainCommand;
  public @Nullable BukkitEvaluable missingPermissionLanguageCommand;
  public @Nullable BukkitEvaluable missingPermissionReloadCommand;
  public @Nullable BukkitEvaluable missingPermissionFeatureSort;
  public @Nullable BukkitEvaluable missingPermissionFeatureFilter;
  public @Nullable BukkitEvaluable missingPermissionFeatureTeleport;
  public @Nullable BukkitEvaluable missingPermissionFeatureTeleportOtherWorld;

  public PlayerMessagesSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
