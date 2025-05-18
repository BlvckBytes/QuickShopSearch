package me.blvckbytes.quick_shop_search.config.display_common;

import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bbconfigmapper.sections.CSIgnore;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.IItemBuildable;
import me.blvckbytes.bukkitevaluable.section.ItemStackSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class GuiItemStackSection extends ItemStackSection {

  private @Nullable BukkitEvaluable slots;

  @CSIgnore
  private IItemBuildable buildable;

  @CSIgnore
  private @Nullable Set<Integer> displaySlots;

  public GuiItemStackSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    this.buildable = asItem();
  }

  public IItemBuildable getBuildable() {
    return buildable;
  }

  public void initializeDisplaySlots(IEvaluationEnvironment inventoryEnvironment) {
    this.displaySlots = Collections.unmodifiableSet(
      slots == null
        ? Set.of()
        : slots.asSet(ScalarType.INT, inventoryEnvironment)
    );
  }

  public Set<Integer> getDisplaySlots() {
    return displaySlots == null ? Set.of() : displaySlots;
  }

  public void renderInto(Inventory inventory, IEvaluationEnvironment environment) {
    if (displaySlots == null)
      return;

    var item = buildable.build(environment);
    var inventorySize = inventory.getSize();

    for (var slot : displaySlots) {
      if (slot < inventorySize)
        inventory.setItem(slot, item);
    }
  }
}
