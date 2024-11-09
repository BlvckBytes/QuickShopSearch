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
  public @Nullable BukkitEvaluable shopInteractPromptBuying;
  public @Nullable BukkitEvaluable shopInteractPromptSelling;
  public @Nullable BukkitEvaluable shopInteractPromptLimitingFactorPlayerSpace;
  public @Nullable BukkitEvaluable shopInteractPromptLimitingFactorPlayerStock;
  public @Nullable BukkitEvaluable shopInteractPromptLimitingFactorShopSpace;
  public @Nullable BukkitEvaluable shopInteractPromptLimitingFactorShopStock;
  public @Nullable BukkitEvaluable shopInteractPromptLimitingFactorSellerFunds;
  public @Nullable BukkitEvaluable shopInteractPromptLimitingFactorBuyerFunds;
  public @Nullable BukkitEvaluable shopInteractPromptTimeout;
  public @Nullable BukkitEvaluable shopInteractPromptDispatch;
  public @Nullable BukkitEvaluable shopInteractPromptInvalidInput;
  public @Nullable BukkitEvaluable shopInteractPromptCancelPrevious;
  public @Nullable BukkitEvaluable shopInteractPromptCancelCurrent;
  public @Nullable BukkitEvaluable shopInteractPlayerHasNoSpace;
  public @Nullable BukkitEvaluable shopInteractPlayerHasNoStock;
  public @Nullable BukkitEvaluable shopInteractShopHasNoSpace;
  public @Nullable BukkitEvaluable shopInteractShopHasNoStock;
  public @Nullable BukkitEvaluable shopInteractSellerInsufficientFunds;
  public @Nullable BukkitEvaluable shopInteractBuyerInsufficientFunds;

  public @Nullable BukkitEvaluable missingPermissionMainCommand;
  public @Nullable BukkitEvaluable missingPermissionLanguageCommand;
  public @Nullable BukkitEvaluable missingPermissionReloadCommand;
  public @Nullable BukkitEvaluable missingPermissionFeatureSort;
  public @Nullable BukkitEvaluable missingPermissionFeatureFilter;
  public @Nullable BukkitEvaluable missingPermissionFeatureTeleport;
  public @Nullable BukkitEvaluable missingPermissionFeatureTeleportOtherWorld;
  public @Nullable BukkitEvaluable missingPermissionFeatureInteract;
  public @Nullable BukkitEvaluable missingPermissionFeatureInteractOtherWorld;

  public PlayerMessagesSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
