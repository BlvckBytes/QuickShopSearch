package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.config.access_lists.ShopAccessListsSection;
import me.blvckbytes.quick_shop_search.config.commands.CommandsSection;
import me.blvckbytes.quick_shop_search.config.cooldowns.CooldownsSection;
import me.blvckbytes.quick_shop_search.config.fees.FeesSection;
import me.blvckbytes.quick_shop_search.config.integration.EssentialsWarpsIntegrationSection;
import me.blvckbytes.quick_shop_search.config.integration.PlayerWarpsIntegrationSection;
import me.blvckbytes.quick_shop_search.config.integration.WorldGuardIntegrationSection;
import me.blvckbytes.quick_shop_search.config.result_display.ResultDisplaySection;
import me.blvckbytes.quick_shop_search.config.slow_teleport.SlowTeleportSection;
import me.blvckbytes.quick_shop_search.config.teleport_display.TeleportDisplaySection;

@CSAlways
public class MainSection extends AConfigSection {

  public ResultDisplaySection resultDisplay;
  public TeleportDisplaySection teleportDisplay;
  public PlayerMessagesSection playerMessages;
  public PredicatesSection predicates;
  public CommandsSection commands;
  public ShopAccessListsSection shopAccessLists;
  public CooldownsSection cooldowns;
  public SlowTeleportSection slowTeleport;
  public PlayerWarpsIntegrationSection playerWarpsIntegration;
  public EssentialsWarpsIntegrationSection essentialsWarpsIntegration;
  public WorldGuardIntegrationSection worldGuardIntegration;
  public FeesSection fees;

  public MainSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
