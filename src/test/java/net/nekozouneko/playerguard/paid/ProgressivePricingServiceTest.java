package net.nekozouneko.playerguard.paid;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressivePricingServiceTest {

    private final ProgressivePricingService pricing = new ProgressivePricingService(rates());

    @Test
    void keepsVolumeInsideFreeLimitFree() {
        assertEquals(new BigDecimal("0.00"), pricing.calculate(0, 100000, 100000));
    }

    @Test
    void chargesFirstRateBand() {
        assertEquals(new BigDecimal("5000.00"), pricing.calculate(100000, 150000, 0));
    }

    @Test
    void chargesAcrossTwoRateBands() {
        assertEquals(new BigDecimal("9000.00"), pricing.calculate(100000, 170000, 0));
    }

    @Test
    void chargesOnlyChangedRangeAcrossBoundary() {
        assertEquals(new BigDecimal("3000.00"), pricing.calculate(140000, 160000, 0));
    }

    @Test
    void chargesEachConfiguredBand() {
        assertEquals(new BigDecimal("10000.00"), pricing.calculate(150000, 200000, 0));
        assertEquals(new BigDecimal("5000.00"), pricing.calculate(200000, 210000, 0));
    }

    @Test
    void returnsZeroForUnchangedOrShrunkVolume() {
        assertEquals(new BigDecimal("0.00"), pricing.calculate(170000, 170000, 0));
        assertEquals(new BigDecimal("0.00"), pricing.calculate(170000, 100000, 0));
    }

    @Test
    void honorsPermissionFreeLimitBeforeRates() {
        assertEquals(new BigDecimal("5000.00"), pricing.calculate(90000, 150000, 100000));
    }

    private static NavigableMap<Long, BigDecimal> rates() {
        NavigableMap<Long, BigDecimal> rates = new TreeMap<>();
        rates.put(100000L, new BigDecimal("0.1"));
        rates.put(150000L, new BigDecimal("0.2"));
        rates.put(200000L, new BigDecimal("0.5"));
        return rates;
    }
}
