package me.blvckbytes.quick_shop_search.display;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.Nullable;

public enum TranslatedClick {
  LEFT,
  SHIFT_LEFT,
  RIGHT,
  SHIFT_RIGHT,
  MIDDLE,
  DROP_ONE,
  DROP_ALL,
  SWAP_OFFHAND,
  DOUBLE_CLICK,
  NUMBER_KEY_1,
  NUMBER_KEY_2,
  NUMBER_KEY_3,
  NUMBER_KEY_4,
  NUMBER_KEY_5,
  NUMBER_KEY_6,
  NUMBER_KEY_7,
  NUMBER_KEY_8,
  NUMBER_KEY_9
  ;

  public static @Nullable TranslatedClick fromClickEvent(InventoryClickEvent event) {
    return switch (event.getClick()) {
      case LEFT -> LEFT;
      case SHIFT_LEFT -> SHIFT_LEFT;
      case RIGHT -> RIGHT;
      case SHIFT_RIGHT -> SHIFT_RIGHT;
      case MIDDLE -> MIDDLE;
      case DROP -> DROP_ONE;
      case CONTROL_DROP -> DROP_ALL;
      case SWAP_OFFHAND -> SWAP_OFFHAND;
      case DOUBLE_CLICK -> DOUBLE_CLICK;
      case NUMBER_KEY -> switch (event.getHotbarButton()) {
        case 0 -> NUMBER_KEY_1;
        case 1 -> NUMBER_KEY_2;
        case 2 -> NUMBER_KEY_3;
        case 3 -> NUMBER_KEY_4;
        case 4 -> NUMBER_KEY_5;
        case 5 -> NUMBER_KEY_6;
        case 6 -> NUMBER_KEY_7;
        case 7 -> NUMBER_KEY_8;
        case 8 -> NUMBER_KEY_9;
        default -> null;
      };
      default -> null;
    };
  }
}
