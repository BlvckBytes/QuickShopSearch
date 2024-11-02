package me.blvckbytes.quick_shop_search.display;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.IntFunction;

public class ClickTranslator {

  private static class SlotState {
    private final TranslatedClick[] supportedClicks;
    private int currentClick;

    public SlotState(EnumSet<TranslatedClick> supportedClicks) {
      this.supportedClicks = supportedClicks.toArray(TranslatedClick[]::new);
      Arrays.sort(this.supportedClicks, Comparator.comparingInt(Enum::ordinal));
    }

    void next() {
      if (++currentClick >= supportedClicks.length)
        currentClick = 0;
    }

    TranslatedClick current() {
      return supportedClicks[currentClick];
    }
  }

  private @Nullable SlotState @Nullable [] slotStates;

  public void setup(
    int rows,
    Collection<Set<Integer>> slotGroups,
    IntFunction<EnumSet<TranslatedClick>> supportedClicksMapper
  ) {
    var slotCount = rows * 9;

    this.slotStates = new SlotState[slotCount];

    for (var slotId = 0 ; slotId < slotCount; ++slotId) {
      var supportedClicks = supportedClicksMapper.apply(slotId);

      // TODO: For slot-groups, have array-slots point at the same object

      if (supportedClicks == null || supportedClicks.isEmpty()) {
        this.slotStates[slotId] = null;
        continue;
      }

      this.slotStates[slotId] = new SlotState(supportedClicks);
    }
  }

  public @Nullable TranslatedClick updateOrTranslate(InventoryClickEvent event) {
    SlotState slotState;
    var slotId = event.getRawSlot();

    if (slotStates == null || slotId < 0 || slotId >= slotStates.length || (slotState = slotStates[slotId]) == null)
      return TranslatedClick.fromClickEvent(event);

    var clickType = event.getClick();

    if (clickType == ClickType.DROP || clickType == ClickType.CONTROL_DROP) {
      slotState.next();
      return null;
    }

    return slotState.current();
  }

  public @Nullable TranslatedClick getCurrent(int slot) {
    if (slotStates == null || slot < 0 || slot >= slotStates.length)
      return null;

    var slotState = slotStates[slot];

    if (slotState == null)
      return null;

    return slotState.current();
  }
}
