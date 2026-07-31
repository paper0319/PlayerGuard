package net.nekozouneko.playerguard.paid;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.math.BigDecimal;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Logger;

public record PaidExtensionConfig(boolean enabled, NavigableMap<Long, BigDecimal> rates,
                                 boolean allowRefundOnDelete, BigDecimal refundRate) {
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
        BigDecimal refundRate;
        try { refundRate = new BigDecimal(config.getString("protection.paid-extension.refund-rate", "0.5")); }
        catch (RuntimeException ex) { refundRate = BigDecimal.ZERO; }
        if (refundRate.compareTo(BigDecimal.ZERO) < 0 || refundRate.compareTo(BigDecimal.ONE) > 0) {
            logger.warning("Invalid paid-extension refund-rate; clamping to 0.0 through 1.0.");
            refundRate = refundRate.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        }
        return new PaidExtensionConfig(enabled, rates, config.getBoolean("protection.paid-extension.allow-refund-on-delete", true), refundRate);
    }
}
