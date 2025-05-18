package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import javax.annotation.Nullable;

public class PlayerMessagesSection extends AConfigSection {

  public @Nullable BukkitEvaluable emptyPredicate;
  public @Nullable BukkitEvaluable noMatches;
  public @Nullable BukkitEvaluable beforeQuerying;
  public @Nullable BukkitEvaluable beforeTeleporting;
  public @Nullable BukkitEvaluable beforeTeleportingNearestPlayerWarp;
  public @Nullable BukkitEvaluable beforeTeleportingNearestEssentialsWarp;
  public @Nullable BukkitEvaluable queryingAllShops;
  public @Nullable BukkitEvaluable usageQsslCommandLanguage;
  public @Nullable BukkitEvaluable unknownLanguageActionBar;
  public @Nullable BukkitEvaluable pluginReloadedSuccess;
  public @Nullable BukkitEvaluable pluginReloadedError;
  public @Nullable BukkitEvaluable predicateParseError;
  public @Nullable BukkitEvaluable searchFlagParseError;
  public @Nullable BukkitEvaluable shopInteractPromptBuying;
  public @Nullable BukkitEvaluable shopInteractPromptSelling;
  public @Nullable BukkitEvaluable shopInteractPromptTimeout;
  public @Nullable BukkitEvaluable shopInteractPromptDispatch;
  public @Nullable BukkitEvaluable shopInteractPromptInvalidInput;
  public @Nullable BukkitEvaluable shopInteractPromptCancelPrevious;
  public @Nullable BukkitEvaluable shopInteractPromptCancelCurrent;
  public @Nullable BukkitEvaluable shopInteractPromptLimitingFactorPlayerSpace;
  public @Nullable BukkitEvaluable shopInteractPromptLimitingFactorPlayerStock;
  public @Nullable BukkitEvaluable shopInteractPromptLimitingFactorShopSpace;
  public @Nullable BukkitEvaluable shopInteractPromptLimitingFactorShopStock;
  public @Nullable BukkitEvaluable shopInteractPromptLimitingFactorSellerFunds;
  public @Nullable BukkitEvaluable shopInteractPromptLimitingFactorBuyerFunds;
  public @Nullable BukkitEvaluable shopInteractPromptFeesWarning;
  public @Nullable BukkitEvaluable shopInteractPlayerHasNoSpace;
  public @Nullable BukkitEvaluable shopInteractPlayerHasNoStock;
  public @Nullable BukkitEvaluable shopInteractShopHasNoSpace;
  public @Nullable BukkitEvaluable shopInteractShopHasNoStock;
  public @Nullable BukkitEvaluable shopInteractSellerInsufficientFunds;
  public @Nullable BukkitEvaluable shopInteractBuyerInsufficientFunds;
  public @Nullable BukkitEvaluable shopInteractPendingFeesTask;
  public @Nullable BukkitEvaluable shopInteractCouldNotWithdrawFees;
  public @Nullable BukkitEvaluable shopInteractWithdrawnFees;
  public @Nullable BukkitEvaluable shopInteractPayedBackFees;
  public @Nullable BukkitEvaluable shopInteractCouldNotPayBackFees;

  public @Nullable BukkitEvaluable missingPermissionMainCommand;
  public @Nullable BukkitEvaluable missingPermissionLanguageCommand;
  public @Nullable BukkitEvaluable missingPermissionReloadCommand;
  public @Nullable BukkitEvaluable missingPermissionAdvertiseCommand;
  public @Nullable BukkitEvaluable missingPermissionFeatureSort;
  public @Nullable BukkitEvaluable missingPermissionFeatureFilter;
  public @Nullable BukkitEvaluable missingPermissionFeatureTeleport;
  public @Nullable BukkitEvaluable missingPermissionFeatureTeleportOtherWorld;
  public @Nullable BukkitEvaluable missingPermissionFeatureInteract;
  public @Nullable BukkitEvaluable missingPermissionFeatureInteractOtherWorld;

  public @Nullable BukkitEvaluable commandAdvertiseDescription;
  public @Nullable BukkitEvaluable commandAdvertiseNotLookingAtShop;
  public @Nullable BukkitEvaluable commandAdvertiseNotTheOwner;
  public @Nullable BukkitEvaluable commandAdvertiseToggleError;
  public @Nullable BukkitEvaluable commandAdvertiseEnabledSelf;
  public @Nullable BukkitEvaluable commandAdvertiseEnabledOther;
  public @Nullable BukkitEvaluable commandAdvertiseDisabledSelf;
  public @Nullable BukkitEvaluable commandAdvertiseDisabledOther;

  public @Nullable BukkitEvaluable pendingCooldownFeatureTeleportSameShop;
  public @Nullable BukkitEvaluable pendingCooldownFeatureTeleportAnyShop;
  public @Nullable BukkitEvaluable pendingCooldownFeatureTeleportOtherWorldSameShop;
  public @Nullable BukkitEvaluable pendingCooldownFeatureTeleportOtherWorldAnyShop;

  public @Nullable BukkitEvaluable slowTeleportHasMoved;
  public @Nullable BukkitEvaluable slowTeleportHasBeenAttacked;
  public @Nullable BukkitEvaluable slowTeleportAttackedSomebody;
  public @Nullable BukkitEvaluable slowTeleportTookDamageByNonPlayer;

  public @Nullable BukkitEvaluable nearestPlayerWarpBanned;

  public PlayerMessagesSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
