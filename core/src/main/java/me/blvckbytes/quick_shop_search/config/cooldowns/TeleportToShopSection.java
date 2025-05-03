package me.blvckbytes.quick_shop_search.config.cooldowns;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.bbconfigmapper.sections.CSNamed;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.PluginPermission;
import org.bukkit.entity.Player;

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

  public long getCooldownMillis(Player player, CooldownType type) {
    for (var groupEntry : groups.entrySet()) {
      var groupPermission = PluginPermission.TELEPORT_COOLDOWN_GROUP_BASE.nodeWithSuffix(groupEntry.getKey());

      if (player.hasPermission(groupPermission))
        return groupEntry.getValue().getCooldownMillis(type);
    }

    return _default.getCooldownMillis(type);
  }
}
