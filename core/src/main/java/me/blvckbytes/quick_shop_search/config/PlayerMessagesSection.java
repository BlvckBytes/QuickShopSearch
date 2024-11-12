package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import javax.annotation.Nullable;

public class PlayerMessagesSection extends AConfigSection {

  public BukkitEvaluable emptyPredicate;
  public BukkitEvaluable noMatches;
  public BukkitEvaluable beforeQueryingGlobal;
  public BukkitEvaluable beforeQueryingNear;
  public BukkitEvaluable beforeQueryingPlayer;
  public BukkitEvaluable beforeQueryingGlobalNoPredicate;
  public BukkitEvaluable beforeQueryingNearNoPredicate;
  public BukkitEvaluable beforeQueryingPlayerNoPredicate;
  public BukkitEvaluable beforeTeleporting;

  public BukkitEvaluable pluginReloadedSuccess;
  public BukkitEvaluable pluginReloadedError;
  public BukkitEvaluable predicateParseError;
  public BukkitEvaluable shopInteractPromptBuying;
  public BukkitEvaluable shopInteractPromptSelling;
  public BukkitEvaluable shopInteractPromptTimeout;
  public BukkitEvaluable shopInteractPromptDispatch;
  public BukkitEvaluable shopInteractPromptInvalidInput;
  public BukkitEvaluable shopInteractPromptCancelPrevious;
  public BukkitEvaluable shopInteractPromptCancelCurrent;

  public BukkitEvaluable missingPermissionMainCommand;
  public BukkitEvaluable missingPermissionSubCommand;
  public BukkitEvaluable missingPermissionFeatureSort;
  public BukkitEvaluable missingPermissionFeatureFilter;
  public BukkitEvaluable missingPermissionFeatureTeleport;
  public BukkitEvaluable missingPermissionFeatureTeleportOtherWorld;
  public BukkitEvaluable missingPermissionFeatureInteract;
  public BukkitEvaluable missingPermissionFeatureInteractOtherWorld;
  public BukkitEvaluable missingPermissionAdvertiseManyOther;

  public BukkitEvaluable commandAdvertiseNotLookingAtShop;
  public BukkitEvaluable commandAdvertiseNotTheOwner;
  public BukkitEvaluable commandAdvertiseInternalError;
  public BukkitEvaluable commandAdvertiseUnchangedSelf;
  public BukkitEvaluable commandAdvertiseUnchangedOther;
  public BukkitEvaluable commandAdvertiseChangedSelf;
  public BukkitEvaluable commandAdvertiseChangedOther;
  public BukkitEvaluable commandAdvertiseReadSelf;
  public BukkitEvaluable commandAdvertiseReadOther;

  public BukkitEvaluable commandAdvertiseManyOwnsNoShopsSelf;
  public BukkitEvaluable commandAdvertiseManyOwnsNoShopsOther;
  public BukkitEvaluable commandAdvertiseManyNoShopsMatchedTargetSelf;
  public BukkitEvaluable commandAdvertiseManyNoShopsMatchedTargetOther;
  public @Nullable BukkitEvaluable commandAdvertiseManyAlteredScreenHeader;
  public @Nullable BukkitEvaluable commandAdvertiseManyAlteredScreenFooter;

  public BukkitEvaluable commandArgumentUnknownPlayerName;
  public BukkitEvaluable commandArgumentMalformedInteger;
  public BukkitEvaluable commandArgumentNegativeOrZeroInteger;
  public @Nullable BukkitEvaluable commandHelpScreenHeader;
  public BukkitEvaluable commandHelpScreenLine;
  public @Nullable BukkitEvaluable commandHelpScreenFooter;
  public BukkitEvaluable unknownSubCommand;
  public BukkitEvaluable subCommandUsage;
  public BukkitEvaluable subCommandPlayerOnly;

  public BukkitEvaluable commandAdvertiseManyUsageSelf;
  public BukkitEvaluable commandAdvertiseManyUsageOther;
  public BukkitEvaluable commandAdvertiseManyDescriptionSelf;
  public BukkitEvaluable commandAdvertiseManyDescriptionOther;
  public BukkitEvaluable commandAdvertiseUsage;
  public BukkitEvaluable commandAdvertiseDescription;
  public BukkitEvaluable commandReloadUsage;
  public BukkitEvaluable commandReloadDescription;
  public BukkitEvaluable commandPlayerUsage;
  public BukkitEvaluable commandPlayerDescription;
  public BukkitEvaluable commandNearUsage;
  public BukkitEvaluable commandNearDescription;
  public BukkitEvaluable commandGlobalUsage;
  public BukkitEvaluable commandGlobalDescription;
  public BukkitEvaluable commandAboutUsage;
  public BukkitEvaluable commandAboutDescription;
  public @Nullable  BukkitEvaluable commandAboutScreenHeader;
  public BukkitEvaluable commandAboutScreen;
  public @Nullable  BukkitEvaluable commandAboutScreenFooter;

  public PlayerMessagesSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
