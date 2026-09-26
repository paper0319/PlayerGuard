package net.nekozouneko.playerguard.paid;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.math.BigDecimal;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Logger;

public record PaidExtensionConfig(boolean enabled, NavigableMap<Long, BigDecimal> rates,
                                  boolean allowRefundOnDelete, BigDecimal refundRate,
                                  RefundPolicy refundPolicy) {
    public static PaidExtensionConfig load(Configuration config, Logger logger) {
        boolean enabled = config.getBoolean("protection.paid-extension.enabled", false);
        NavigableMap<Long, BigDecimal> rates = new TreeMap<>();
        ConfigurationSection section = config.getConfigurationSection("protection.paid-extension.rates");
        if (section != null) for (String key : section.getKeys(false)) {
            try {
                long start = Long.parseLong(key);
                BigDecimal rate = new BigDecimal(String.valueOf(section.get(key)));
                if (start < 0 || rate.signum() < 0 || !Double.isFinite(rate.doubleValue())) throw new NumberFormatException();
                rates.put(start, rate);
            } catch (RuntimeException ex) {
                logger.warning("Ignoring invalid paid-extension rate: " + key);
            }
        }
        BigDecimal legacyRefundRate = readRate(config, logger, "protection.paid-extension.refund-rate", "0.5");
        boolean allowRefund = config.getBoolean("protection.paid-extension.allow-refund-on-delete", true);
        RefundPolicy policy = new RefundPolicy(
                config.getBoolean("protection.paid-extension.refund-policy.enabled", allowRefund),
                Math.max(0L, config.getLong("protection.paid-extension.refund-policy.cooling-off-minutes", 30L)),
                readRate(config, logger, "protection.paid-extension.refund-policy.cooling-off-rate", "1.0"),
                Math.max(0L, config.getLong("protection.paid-extension.refund-policy.normal-period-days", 7L)),
                readRate(config, logger, "protection.paid-extension.refund-policy.normal-refund-rate", "0.8"),
                readRate(config, logger, "protection.paid-extension.refund-policy.long-term-refund-rate", legacyRefundRate.toPlainString())
        );
        return new PaidExtensionConfig(enabled, rates, allowRefund, legacyRefundRate, policy);
    }

    private static BigDecimal readRate(Configuration config, Logger logger, String path, String fallback) {
        BigDecimal value;
        try { value = new BigDecimal(config.getString(path, fallback)); }
        catch (RuntimeException ex) { value = new BigDecimal(fallback); }
        BigDecimal clamped = value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        if (clamped.compareTo(value) != 0) logger.warning("Invalid rate at " + path + "; clamped to 0.0 through 1.0.");
        return clamped;
    }
}