package net.nekozouneko.playerguard.paid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

public final class ProtectionPricingPreview {
    private ProtectionPricingPreview() {}

    public static Result calculate(long before, long added, long freeLimit, NavigableMap<Long, BigDecimal> rates) {
        long after = Math.addExact(before, added);
        long freeAdded = Math.max(0L, Math.min(after, freeLimit) - before);
        long paidAdded = Math.max(0L, added - freeAdded);
        List<Band> bands = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        if (paidAdded > 0 && rates != null && !rates.isEmpty()) {
            long chargeFrom = Math.max(before, freeLimit);
            for (Map.Entry<Long, BigDecimal> entry : rates.entrySet()) {
                long start = entry.getKey();
                Long nextKey = rates.higherKey(start);
                long from = Math.max(chargeFrom, start);
                long to = nextKey == null ? after : Math.min(after, nextKey);
                if (to > from) {
                    BigDecimal amount = entry.getValue().multiply(BigDecimal.valueOf(to - from)).setScale(2, RoundingMode.HALF_UP);
                    bands.add(new Band(from, to, to - from, entry.getValue(), amount));
                    total = total.add(amount);
                }
            }
        }
        return new Result(before, after, freeLimit, added, freeAdded, paidAdded, total.setScale(2, RoundingMode.HALF_UP), bands);
    }

    public record Result(long beforeVolume, long afterVolume, long freeLimit, long addedVolume,
                         long freeAddedVolume, long paidAddedVolume, BigDecimal total, List<Band> bands) {}

    public record Band(long from, long to, long volume, BigDecimal rate, BigDecimal amount) {}
}