package me.blvckbytes.quick_shop_search.config.integration;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashSet;
import java.util.Set;

public class WorldGuardIntegrationSection extends AConfigSection {

  public boolean enabled;
  public Set<Integer> ignoredPriorities;
  public Set<String> ignoredIds;
  public Set<String> advertiseIdsAllowList;
  public boolean autoAdvertiseIfInAllowList;
  public boolean disableAdvertiseIfNotAllowed;

  public WorldGuardIntegrationSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.enabled = false;
    this.ignoredPriorities = new HashSet<>();
    this.ignoredIds = new HashSet<>();
    this.advertiseIdsAllowList = new HashSet<>();
    this.autoAdvertiseIfInAllowList = false;
    this.disableAdvertiseIfNotAllowed = false;
  }
}
