package me.blvckbytes.quick_shop_search.config.display_common;

import me.blvckbytes.bbconfigmapper.MappingError;
import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSIgnore;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public class PaginatedGuiSection<T extends AConfigSection> extends GuiSection<T> {

  protected @Nullable BukkitEvaluable paginationSlots;

  @CSIgnore
  private Set<Integer> _paginationSlots;

  public PaginatedGuiSection(Class<T> itemsSectionClass, EvaluationEnvironmentBuilder baseEnvironment) {
    super(itemsSectionClass, baseEnvironment);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    _paginationSlots = paginationSlots == null ? Set.of() : paginationSlots.asSet(ScalarType.INT, inventoryEnvironment);

    for (var paginationSlot : _paginationSlots) {
      if (paginationSlot < 0 || paginationSlot > lastSlot)
        throw new MappingError("Pagination slot " + paginationSlot + " out of range [0;" + lastSlot + "]");
    }

    for (var field : itemsSectionClass.getDeclaredFields()) {
      if (!GuiItemStackSection.class.isAssignableFrom(field.getType()))
        continue;

      field.setAccessible(true);

      GuiItemStackSection itemSection = (GuiItemStackSection) field.get(items);

      if (itemSection == null)
        continue;

      for (var itemSlot : itemSection.getDisplaySlots()) {
        if (itemSlot < 0 || itemSlot > lastSlot)
          throw new MappingError("Slot " + itemSlot + " of item " + field.getName() + " out of range [0;" + lastSlot + "]");

        if (_paginationSlots.contains(itemSlot))
          throw new MappingError("Slot " + itemSlot + " of item " + field.getName() + " conflicts with pagination-slots " + paginationSlots);
      }
    }
  }

  public Set<Integer> getPaginationSlots() {
    return _paginationSlots;
  }
}
