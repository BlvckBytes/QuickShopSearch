package me.blvckbytes.quick_shop_search.config.access_lists;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Material;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShopAccessListSection extends ConfigSection {

  public boolean isAllowTypes;
  public List<ComponentMarkup> types;

  @CSIgnore
  public final Set<Material> typeMaterials;

  public ShopAccessListSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
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
      var materialString = type.asPlainString(null);
      var xMaterial = XMaterial.matchXMaterial(materialString);

      if (xMaterial.isEmpty())
        throw new MappingError("Could not interpret access-list type \"" + materialString + "\" as a XMaterial");

      typeMaterials.add(xMaterial.get().get());
    }
  }
}
