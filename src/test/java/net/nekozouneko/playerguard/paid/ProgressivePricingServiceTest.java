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

    @Test
    void calculatesRequestedProgressiveExample() {
        ProgressivePricingService service = new ProgressivePricingService(defaultRates());

        assertEquals(new BigDecimal("3600.00"), service.calculate(120000, 250000, 120000));
    }

    @Test
    void chargesOnlyVolumeAboveFreeLimit() {
        ProgressivePricingService service = new ProgressivePricingService(defaultRates());

        assertEquals(new BigDecimal("600.00"), service.calculate(80000, 150000, 120000));
    }

    @Test
    void reportsCurrentRateAndNextBandForGui() {
        ProgressivePricingService service = new ProgressivePricingService(defaultRates());

        assertEquals(new BigDecimal("0.02"), service.currentRate(150000, 120000));
        assertEquals(200000L, service.nextBandStart(150000, 120000));
    }

    @Test
    void reportsNoCurrentRateInsideFreeLimit() {
        ProgressivePricingService service = new ProgressivePricingService(defaultRates());

        assertEquals(BigDecimal.ZERO, service.currentRate(80000, 120000));
        assertEquals(120000L, service.nextBandStart(80000, 120000));
    }

    private static NavigableMap<Long, BigDecimal> rates() {
        NavigableMap<Long, BigDecimal> rates = new TreeMap<>();
        rates.put(100000L, new BigDecimal("0.1"));
        rates.put(150000L, new BigDecimal("0.2"));
        rates.put(200000L, new BigDecimal("0.5"));
        return rates;
    }

    private static NavigableMap<Long, BigDecimal> defaultRates() {
        NavigableMap<Long, BigDecimal> rates = new TreeMap<>();
        rates.put(120000L, new BigDecimal("0.02"));
        rates.put(200000L, new BigDecimal("0.04"));
        rates.put(300000L, new BigDecimal("0.08"));
        rates.put(500000L, new BigDecimal("0.15"));
        return rates;
    }
}
