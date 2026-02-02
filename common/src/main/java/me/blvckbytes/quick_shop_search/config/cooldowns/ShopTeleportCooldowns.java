package me.blvckbytes.quick_shop_search.config.cooldowns;

import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class ShopTeleportCooldowns extends ConfigSection {

  public long sameShop;
  public long anyShop;
  public long otherWorldSameShop;
  public long otherWorldAnyShop;

  public ShopTeleportCooldowns(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  public long getCooldownMillis(CooldownType type) {
    return switch (type) {
      case SAME_SHOP -> sameShop;
      case ANY_SHOP -> anyShop;
      case OTHER_WORLD_SAME_SHOP -> otherWorldSameShop;
      case OTHER_WORLD_ANY_SHOP -> otherWorldAnyShop;
    } * 1000;
  }
}
