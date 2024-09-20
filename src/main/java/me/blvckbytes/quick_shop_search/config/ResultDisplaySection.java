package me.blvckbytes.quick_shop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.IItemBuildable;
import me.blvckbytes.bukkitevaluable.section.ItemStackSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.List;

public class ResultDisplaySection extends AConfigSection {

  public BukkitEvaluable title;
  public IItemBuildable previousPage;
  public IItemBuildable nextPage;

  @CSAlways
  public ItemStackSection representativePatch;

  public ResultDisplaySection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }

  @Override
  public void afterParsing(List<Field> fields) {
    if (title == null)
      title = BukkitEvaluable.UNDEFINED_STRING;

    if (previousPage == null)
      previousPage = IItemBuildable.makeUndefined();

    if (nextPage == null)
      nextPage = IItemBuildable.makeUndefined();
  }
}
