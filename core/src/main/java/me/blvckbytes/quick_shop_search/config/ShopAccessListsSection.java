package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSNamed;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ShopAccessListsSection extends AConfigSection {

  @CSNamed(name="default")
  public @Nullable ShopAccessListSection _default;

  public Map<String, ShopAccessListSection> permissions;

  public ShopAccessListsSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.permissions = new HashMap<>();
  }
}
