package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.config.access_lists.ShopAccessListsSection;
import me.blvckbytes.quick_shop_search.config.commands.CommandsSection;
import me.blvckbytes.quick_shop_search.config.cooldowns.CooldownsSection;
import me.blvckbytes.quick_shop_search.config.fees.FeesSection;
import me.blvckbytes.quick_shop_search.config.result_display.ResultDisplaySection;
import me.blvckbytes.quick_shop_search.config.slow_teleport.SlowTeleportSection;

@CSAlways
public class MainSection extends AConfigSection {

  public ResultDisplaySection resultDisplay;
  public PlayerMessagesSection playerMessages;
  public PredicatesSection predicates;
  public CommandsSection commands;
  public ShopAccessListsSection shopAccessLists;
  public CooldownsSection cooldowns;
  public SlowTeleportSection slowTeleport;
  public PlayerWarpsIntegrationSection playerWarpsIntegration;
  public FeesSection fees;

  public MainSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
