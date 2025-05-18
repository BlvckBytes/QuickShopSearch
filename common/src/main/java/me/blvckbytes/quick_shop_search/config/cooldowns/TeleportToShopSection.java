package me.blvckbytes.quick_shop_search.config.cooldowns;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.bbconfigmapper.sections.CSNamed;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

@CSAlways
public class TeleportToShopSection extends AConfigSection {

  @CSNamed(name="default")
  public ShopTeleportCooldowns _default;

  public Map<String, ShopTeleportCooldowns> groups;

  public TeleportToShopSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.groups = new LinkedHashMap<>();
  }
}
