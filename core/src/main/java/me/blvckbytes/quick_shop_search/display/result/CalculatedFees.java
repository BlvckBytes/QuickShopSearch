package me.blvckbytes.quick_shop_search.display.result;

import com.ghostchu.quickshop.api.shop.ShopType;
import me.blvckbytes.quick_shop_search.cache.CachedShop;
import me.blvckbytes.quick_shop_search.config.fees.FeesValuesSection;
import org.jetbrains.annotations.Nullable;

public record CalculatedFees(
  double absoluteFees,
  double relativeFees,
  double relativeFeesValue
) {
  public static final CalculatedFees NO_FEES = new CalculatedFees(0, 0, 0);

  public boolean isNotZero() {
    return relativeFees() != 0 || absoluteFees() != 0;
  }

  public static CalculatedFees calculateFor(@Nullable FeesValuesSection feesValues, CachedShop cachedShop) {
    if (feesValues == null)
      return NO_FEES;

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

    if (cachedShop.cachedType == ShopType.BUYING) {
      var remainingPrice = cachedShop.cachedPrice - relativeFeesValue;

      if (absoluteFees > remainingPrice) {
        absoluteFees = remainingPrice;
      }
    }

    return new CalculatedFees(absoluteFees, relativeFees, relativeFeesValue);
  }
}
