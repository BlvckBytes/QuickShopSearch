package me.blvckbytes.quick_shop_search.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import javax.annotation.Nullable;

public class PlayerMessagesSection extends ConfigSection {

  public @Nullable ComponentMarkup emptyPredicate;
  public @Nullable ComponentMarkup noMatches;
  public @Nullable ComponentMarkup beforeQuerying;
  public @Nullable ComponentMarkup beforeTeleporting;
  public @Nullable ComponentMarkup beforeTeleportingNearestPlayerWarp;
  public @Nullable ComponentMarkup beforeTeleportingNearestEssentialsWarp;
  public @Nullable ComponentMarkup queryingAllShops;
  public @Nullable ComponentMarkup usageQsslCommandLanguage;
  public @Nullable ComponentMarkup unknownLanguageActionBar;
  public @Nullable ComponentMarkup pluginReloadedSuccess;
  public @Nullable ComponentMarkup pluginReloadedError;
  public @Nullable ComponentMarkup predicateParseError;
  public @Nullable ComponentMarkup searchFlagParseError;
  public @Nullable ComponentMarkup shopInteractPromptBuying;
  public @Nullable ComponentMarkup shopInteractPromptSelling;
  public @Nullable ComponentMarkup shopInteractPromptTimeout;
  public @Nullable ComponentMarkup shopInteractPromptDispatch;
  public @Nullable ComponentMarkup shopInteractPromptInvalidInput;
  public @Nullable ComponentMarkup shopInteractPromptCancelPrevious;
  public @Nullable ComponentMarkup shopInteractPromptCancelCurrent;
  public @Nullable ComponentMarkup shopInteractPromptFeesWarning;
  public @Nullable ComponentMarkup shopInteractPlayerHasNoSpace;
  public @Nullable ComponentMarkup shopInteractPlayerHasNoStock;
  public @Nullable ComponentMarkup shopInteractShopHasNoSpace;
  public @Nullable ComponentMarkup shopInteractShopHasNoStock;
  public @Nullable ComponentMarkup shopInteractSellerInsufficientFunds;
  public @Nullable ComponentMarkup shopInteractBuyerInsufficientFunds;
  public @Nullable ComponentMarkup shopInteractPendingFeesTask;
  public @Nullable ComponentMarkup shopInteractCouldNotWithdrawFees;
  public @Nullable ComponentMarkup shopInteractWithdrawnFees;
  public @Nullable ComponentMarkup shopInteractPayedBackFees;
  public @Nullable ComponentMarkup shopInteractCouldNotPayBackFees;

  public @Nullable ComponentMarkup missingPermissionMainCommand;
  public @Nullable ComponentMarkup missingPermissionLanguageCommand;
  public @Nullable ComponentMarkup missingPermissionReloadCommand;
  public @Nullable ComponentMarkup missingPermissionAdvertiseCommand;
  public @Nullable ComponentMarkup missingPermissionFeatureSort;
  public @Nullable ComponentMarkup missingPermissionFeatureFilter;
  public @Nullable ComponentMarkup missingPermissionFeatureTeleport;
  public @Nullable ComponentMarkup missingPermissionFeatureTeleportOtherWorld;
  public @Nullable ComponentMarkup missingPermissionFeatureInteract;
  public @Nullable ComponentMarkup missingPermissionFeatureInteractOtherWorld;

  public @Nullable ComponentMarkup commandAdvertiseDescription;
  public @Nullable ComponentMarkup commandAdvertiseNotLookingAtShop;
  public @Nullable ComponentMarkup commandAdvertiseNotTheOwner;
  public @Nullable ComponentMarkup commandAdvertiseToggleError;
  public @Nullable ComponentMarkup commandAdvertiseEnabledSelf;
  public @Nullable ComponentMarkup commandAdvertiseEnabledOther;
  public @Nullable ComponentMarkup commandAdvertiseDisabledSelf;
  public @Nullable ComponentMarkup commandAdvertiseDisabledOther;
  public @Nullable ComponentMarkup commandAdvertiseDisallowedSelf;
  public @Nullable ComponentMarkup commandAdvertiseDisallowedOther;

  public @Nullable ComponentMarkup pendingCooldownFeatureTeleportSameShop;
  public @Nullable ComponentMarkup pendingCooldownFeatureTeleportAnyShop;
  public @Nullable ComponentMarkup pendingCooldownFeatureTeleportOtherWorldSameShop;
  public @Nullable ComponentMarkup pendingCooldownFeatureTeleportOtherWorldAnyShop;

  public @Nullable ComponentMarkup slowTeleportHasMoved;
  public @Nullable ComponentMarkup slowTeleportHasBeenAttacked;
  public @Nullable ComponentMarkup slowTeleportAttackedSomebody;
  public @Nullable ComponentMarkup slowTeleportTookDamageByNonPlayer;

  public @Nullable ComponentMarkup nearestPlayerWarpBanned;

  public PlayerMessagesSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
