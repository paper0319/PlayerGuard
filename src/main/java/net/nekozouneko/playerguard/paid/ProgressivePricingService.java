package net.nekozouneko.playerguard.paid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;

/** Side-effect-free progressive price calculator. */
public final class ProgressivePricingService {
    private final NavigableMap<Long, BigDecimal> rates;

    public ProgressivePricingService(NavigableMap<Long, BigDecimal> rates) {
        this.rates = Objects.requireNonNull(rates);
    }

    public BigDecimal calculate(long before, long after, long freeLimit) {
        if (before < 0 || after <= before || freeLimit < 0 || rates.isEmpty()) return BigDecimal.ZERO.setScale(2);
        long chargeFrom = Math.max(before, freeLimit);
        if (after <= chargeFrom) return BigDecimal.ZERO.setScale(2);
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry : rates.entrySet()) {
            long start = entry.getKey();
            Long next = rates.higherKey(start);
            long from = Math.max(chargeFrom, start);
            long to = next == null ? after : Math.min(after, next);
            if (to > from) total = total.add(entry.getValue().multiply(BigDecimal.valueOf(to - from)));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
