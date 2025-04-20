package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class ShopTeleportCooldowns extends AConfigSection {

  public long sameShop;
  public long anyShop;
  public long otherWorldSameShop;
  public long otherWorldAnyShop;

  public ShopTeleportCooldowns(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
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
