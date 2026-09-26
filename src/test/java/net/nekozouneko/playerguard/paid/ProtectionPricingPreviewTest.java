package net.nekozouneko.playerguard.paid;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtectionPricingPreviewTest {
    @Test
    void splitsFreeAndPaidVolumeAndBands() {
        ProtectionPricingPreview.Result result = ProtectionPricingPreview.calculate(100000, 120000, 120000, rates());

        assertEquals(20000, result.freeAddedVolume());
        assertEquals(100000, result.paidAddedVolume());
        assertEquals(new BigDecimal("2400.00"), result.total());
        assertEquals(2, result.bands().size());
    }

    private static NavigableMap<Long, BigDecimal> rates() {
        NavigableMap<Long, BigDecimal> rates = new TreeMap<>();
        rates.put(120000L, new BigDecimal("0.02"));
        rates.put(200000L, new BigDecimal("0.04"));
        return rates;
    }
}