package me.blvckbytes.quick_shop_search.config.access_lists;

import me.blvckbytes.bbconfigmapper.MappingError;
import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSIgnore;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShopAccessListSection extends AConfigSection {

  public boolean isAllowTypes;
  public List<BukkitEvaluable> types;

  @CSIgnore
  private final Set<Material> typeMaterials;

  public ShopAccessListSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    // It's very important that this property defaults to false, as an empty types-list
    // thereby results in all shops passing, which is desired behavior.
    this.isAllowTypes = false;
    this.types = new ArrayList<>();
    this.typeMaterials = new HashSet<>();
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var type : types) {
      var interpretedMaterial = type.asXMaterial(builtBaseEnvironment);

      if (interpretedMaterial == null)
        throw new MappingError("Could not interpret access-list type \"" + type.value + "\" as a material");

      typeMaterials.add(interpretedMaterial.parseMaterial());
    }
  }

  public boolean allowsShop(CachedShop shop) {
    // This section could be extended by more criteria in the future - thus, I'll keep this layer of abstraction
    return allowsMaterialOfItem(shop.handle.getItem());
  }

  private boolean allowsMaterialOfItem(ItemStack item) {
    var isTypeContained = typeMaterials.contains(item.getType());

    if (isAllowTypes)
      return isTypeContained;

    return !isTypeContained;
  }
}
