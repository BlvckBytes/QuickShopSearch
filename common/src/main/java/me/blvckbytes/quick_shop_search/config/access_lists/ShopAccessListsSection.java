package me.blvckbytes.quick_shop_search.config.access_lists;

import at.blvckbytes.cm_mapper.mapper.section.CSNamed;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ShopAccessListsSection extends ConfigSection {

  @CSNamed(name="default")
  public @Nullable ShopAccessListSection _default;

  public Map<String, ShopAccessListSection> permissions;

  public ShopAccessListsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this.permissions = new HashMap<>();
  }
}
