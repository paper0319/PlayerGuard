package net.nekozouneko.playerguard.paid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

public record RefundPolicy(boolean enabled, long coolingOffMinutes, BigDecimal coolingOffRate,
                           long normalPeriodDays, BigDecimal normalRefundRate,
                           BigDecimal longTermRefundRate) {
    public BigDecimal rateFor(Instant createdAt, Instant now) {
        if (!enabled || createdAt == null || now == null || now.isBefore(createdAt)) return BigDecimal.ZERO;
        Duration elapsed = Duration.between(createdAt, now);
        if (elapsed.compareTo(Duration.ofMinutes(coolingOffMinutes)) <= 0) return coolingOffRate;
        if (elapsed.compareTo(Duration.ofDays(normalPeriodDays)) <= 0) return normalRefundRate;
        return longTermRefundRate;
    }

    public BigDecimal refundAmount(BigDecimal paid, Instant createdAt, Instant now) {
        if (paid == null || paid.signum() <= 0) return BigDecimal.ZERO.setScale(2);
        return paid.multiply(rateFor(createdAt, now)).setScale(2, RoundingMode.HALF_UP);
    }
}