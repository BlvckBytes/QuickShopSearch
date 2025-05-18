package me.blvckbytes.quick_shop_search.config.access_lists;

import me.blvckbytes.bbconfigmapper.MappingError;
import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSIgnore;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.Material;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShopAccessListSection extends AConfigSection {

  public boolean isAllowTypes;
  public List<BukkitEvaluable> types;

  @CSIgnore
  public final Set<Material> typeMaterials;

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
}
