package me.blvckbytes.quick_shop_search.config.integration;

import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.util.HashSet;
import java.util.Set;

public class WorldGuardIntegrationSection extends ConfigSection {

  public boolean enabled;
  public Set<Integer> ignoredPriorities;
  public Set<String> ignoredIds;
  public Set<String> advertiseIdsAllowList;
  public boolean autoAdvertiseIfInAllowList;
  public boolean disableAdvertiseIfNotAllowed;

  public WorldGuardIntegrationSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this.enabled = false;
    this.ignoredPriorities = new HashSet<>();
    this.ignoredIds = new HashSet<>();
    this.advertiseIdsAllowList = new HashSet<>();
    this.autoAdvertiseIfInAllowList = false;
    this.disableAdvertiseIfNotAllowed = false;
  }
}
