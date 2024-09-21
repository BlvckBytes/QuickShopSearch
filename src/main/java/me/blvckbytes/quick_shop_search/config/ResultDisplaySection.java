package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.IItemBuildable;
import me.blvckbytes.bukkitevaluable.section.ItemStackSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.Nullable;

public class ResultDisplaySection extends AConfigSection {

  public BukkitEvaluable title;
  public IItemBuildable previousPage;
  public IItemBuildable nextPage;
  public IItemBuildable sorting;
  public IItemBuildable filtering;
  public @Nullable IItemBuildable filler;

  @CSAlways
  public ItemStackSection representativePatch;

  public ResultDisplaySection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    registerFieldDefault(BukkitEvaluable.class, () -> BukkitEvaluable.UNDEFINED_STRING);
    registerFieldDefault(IItemBuildable.class, IItemBuildable::makeUndefined, "filler");
  }
}
