package me.blvckbytes.quick_shop_search.display;

import com.ghostchu.quickshop.api.shop.ShopType;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.config.fees.FeesValuesSection;
import org.jetbrains.annotations.Nullable;

public record CalculatedFees(
  double absoluteFees,
  double relativeFees,
  double relativeFeesValue,
  double finalPrice
) {
  public boolean isNotZero() {
    return relativeFees() != 0 || absoluteFees() != 0;
  }

  public static CalculatedFees calculateFor(@Nullable FeesValuesSection feesValues, CachedShop cachedShop) {
    if (feesValues == null)
      return new CalculatedFees(0, 0, cachedShop.cachedPrice, cachedShop.cachedPrice);

    double absoluteFees;
    double relativeFees;

    if (cachedShop.cachedType == ShopType.BUYING) {
      absoluteFees = feesValues.absoluteSell;
      relativeFees = feesValues.relativeSell;
    } else {
      absoluteFees = feesValues.absoluteBuy;
      relativeFees = feesValues.relativeBuy;
    }

    var relativeFeesValue = cachedShop.cachedPrice * (relativeFees / 100);

    double finalPrice;

    if (cachedShop.cachedType == ShopType.BUYING) {
      finalPrice = cachedShop.cachedPrice - relativeFeesValue;

      if (absoluteFees > finalPrice) {
        absoluteFees = finalPrice;
        finalPrice = 0;
      } else {
        finalPrice -= absoluteFees;
      }
    } else {
      finalPrice = cachedShop.cachedPrice + relativeFeesValue + absoluteFees;
    }

    return new CalculatedFees(absoluteFees, relativeFees, relativeFeesValue, finalPrice);
  }
}
