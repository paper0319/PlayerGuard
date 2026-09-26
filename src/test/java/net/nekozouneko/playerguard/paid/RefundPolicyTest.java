package net.nekozouneko.playerguard.paid;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RefundPolicyTest {
    private final RefundPolicy policy = new RefundPolicy(true, 30, new BigDecimal("1.0"),
            7, new BigDecimal("0.8"), new BigDecimal("0.5"));
    private final Instant created = Instant.parse("2026-08-01T00:00:00Z");

    @Test
    void refundsFullyThroughExactlyThirtyMinutes() {
        assertEquals(new BigDecimal("1.0"), policy.rateFor(created, created.plus(30, ChronoUnit.MINUTES)));
        assertEquals(new BigDecimal("5000.00"), policy.refundAmount(new BigDecimal("5000"), created, created.plus(30, ChronoUnit.MINUTES)));
    }

    @Test
    void refundsEightyPercentAfterThirtyMinutesThroughExactlySevenDays() {
        assertEquals(new BigDecimal("0.8"), policy.rateFor(created, created.plus(31, ChronoUnit.MINUTES)));
        assertEquals(new BigDecimal("0.8"), policy.rateFor(created, created.plus(7, ChronoUnit.DAYS)));
    }

    @Test
    void refundsHalfAfterSevenDays() {
        assertEquals(new BigDecimal("0.5"), policy.rateFor(created, created.plus(7, ChronoUnit.DAYS).plus(1, ChronoUnit.SECONDS)));
        assertEquals(new BigDecimal("2500.00"), policy.refundAmount(new BigDecimal("5000"), created, created.plus(8, ChronoUnit.DAYS)));
    }
}