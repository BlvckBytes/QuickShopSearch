package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.MappingError;
import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.bbconfigmapper.sections.CSDecide;
import me.blvckbytes.bbconfigmapper.sections.CSIgnore;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.GPEEE;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

public class GuiSection<T extends AConfigSection> extends AConfigSection {

  private static final int DEFAULT_ROWS = 3;

  protected @Nullable BukkitEvaluable title;
  protected @Nullable BukkitEvaluable rows;

  @CSAlways
  @CSDecide
  public T items;

  @CSIgnore
  protected final Class<T> itemsSectionClass;

  @CSIgnore
  protected int _rows, lastSlot;

  @CSIgnore
  public IEvaluationEnvironment inventoryEnvironment;

  public GuiSection(Class<T> itemsSectionClass, EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
    this.itemsSectionClass = itemsSectionClass;
  }

  @Override
  public @Nullable Class<?> runtimeDecide(String field) {
    if (field.equals("items"))
      return itemsSectionClass;

    return super.runtimeDecide(field);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    _rows = rows == null ? DEFAULT_ROWS : rows.asScalar(ScalarType.INT, GPEEE.EMPTY_ENVIRONMENT);

    if (_rows < 1 || _rows > 6)
      throw new MappingError("Rows out of range [1;6]");

    lastSlot = _rows * 9 - 1;

    inventoryEnvironment = new EvaluationEnvironmentBuilder()
      .withStaticVariable("number_of_rows", _rows)
      .withStaticVariable("last_slot", lastSlot)
      .build(builtBaseEnvironment);

    for (var field : itemsSectionClass.getDeclaredFields()) {
      if (!GuiItemStackSection.class.isAssignableFrom(field.getType()))
        continue;

      field.setAccessible(true);
      ((GuiItemStackSection) field.get(items)).initializeDisplaySlots(inventoryEnvironment);
    }
  }

  public Inventory createInventory(IEvaluationEnvironment environment) {
    if (title == null)
      return Bukkit.createInventory(null, _rows * 9);

    return Bukkit.createInventory(null, _rows * 9, title.stringify(environment));
  }

  public int getRows() {
    return _rows;
  }
}
