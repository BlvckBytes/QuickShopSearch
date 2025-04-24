package me.blvckbytes.quick_shop_search.config;

import com.cryptomorin.xseries.XSound;
import me.blvckbytes.bbconfigmapper.MappingError;
import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.bbconfigmapper.sections.CSIgnore;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;

public class SlowTeleportNotification extends AConfigSection {

  @CSAlways
  public SlotsMessagesSection messages;

  public @Nullable BukkitEvaluable sound;

  @CSIgnore
  public @Nullable XSound _sound;

  public float soundVolume;
  public float soundPitch;

  public SlowTeleportNotification(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.soundVolume = 1;
    this.soundPitch = 1;
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (sound != null) {
      if ((_sound = sound.asXSound(builtBaseEnvironment)) == null)
        throw new MappingError("Property \"sound\" could not be corresponded to any valid sound");
    }
  }
}
