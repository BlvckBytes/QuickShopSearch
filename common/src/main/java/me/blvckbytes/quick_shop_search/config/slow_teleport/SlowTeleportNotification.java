package me.blvckbytes.quick_shop_search.config.slow_teleport;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import com.cryptomorin.xseries.XSound;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;

public class SlowTeleportNotification extends ConfigSection {

  @CSAlways
  public SlotsMessagesSection messages;

  public @Nullable ComponentMarkup sound;

  @CSIgnore
  public @Nullable XSound _sound;

  public float soundVolume;
  public float soundPitch;

  public SlowTeleportNotification(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this.soundVolume = 1;
    this.soundPitch = 1;
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (sound != null) {
      var soundString = sound.asPlainString(null);
      var xSound = XSound.of(soundString);

      if (xSound.isEmpty())
        throw new MappingError("Property \"sound\" could not be corresponded to any valid sound");

      _sound = xSound.get();
    }
  }
}
