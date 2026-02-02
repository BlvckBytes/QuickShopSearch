package me.blvckbytes.quick_shop_search.config.cooldowns;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSNamed;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.util.Map;

@CSAlways
public class TeleportToShopSection extends ConfigSection {

  @CSNamed(name="default")
  public ShopTeleportCooldowns _default;

  public Map<String, ShopTeleportCooldowns> groups;

  public TeleportToShopSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
