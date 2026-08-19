package net.nekozouneko.playerguard;

import net.nekozouneko.playerguard.flag.GuardFlags;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

public class PGConfig {

    private static Configuration config;

    public static void setConfig(Configuration config) {
        PGConfig.config = config;
    }

    public static long getLimit(int day) {
        ConfigurationSection section = config.getConfigurationSection("protection.limit");
        // protection.limit セクションが無い壊れた config でも NPE せず、
        // 同梱デフォルト（getConfig() の defaults）へフォールバックさせる。
        if (section == null) return config.getLong("protection.limit." + day);

        int nearest = 0;
        for (String key : section.getKeys(false)) {
            try {
                int parsed = Integer.parseInt(key);

                if (parsed == day) {
                    nearest = day;
                    break;
                }

                if (parsed > day || Math.abs(day - parsed) > Math.abs(day - nearest)) continue;

                nearest = parsed;
            }
            catch (NumberFormatException nfe) { continue; }
        }

        // 明示的な既定値を渡さないことで、キー欠落時に Bukkit が
        // 同梱 config.yml のデフォルト値を参照できる（黙って 0 にならない）。
        return config.getLong("protection.limit." + nearest);
    }

    public static boolean isFlagDisabled(GuardFlags flag) {
        return !config.getBoolean("protection.flags." + flag.getConfigId());
    }

    public static long getMinSpacingBetweenRegions() {
        return config.getLong("protection.spacing.min-spacing-between-regions");
    }

    public static boolean doApplyToSamePlayerSRegion() {
        return config.getBoolean("protection.spacing.apply-to-same-player-s-region");
    }

    public static boolean isVisitorLogEnabled() {
        return config.getBoolean("visitor-log.enabled", true);
    }

    public static int getVisitorLogMaxEntriesPerRegion() {
        int v = config.getInt("visitor-log.max-entries-per-region", 100);
        return clamp(v, 10, 200);
    }

    public static long getVisitorLogFlushIntervalTicks() {
        int seconds = config.getInt("visitor-log.flush-interval-seconds", 60);
        seconds = clamp(seconds, 10, 3600);
        return seconds * 20L;
    }

    public static String getVisitorLogTarget() {
        String v = config.getString("visitor-log.target", "visitors-only");
        if (v == null) return "visitors-only";
        v = v.trim().toLowerCase();
        if ("visitors-only".equals(v)) return v;
        if ("include-builders".equals(v)) return v;
        if ("include-all".equals(v)) return v;
        return "visitors-only";
    }

    public static boolean isVisitorLogEnterExitEnabled() {
        return config.getBoolean("visitor-log.events.enter-exit", true);
    }

    public static boolean isVisitorLogBreakEnabled() {
        return config.getBoolean("visitor-log.events.break", true);
    }

    public static boolean isVisitorLogPlaceEnabled() {
        return config.getBoolean("visitor-log.events.place", true);
    }

    public static boolean isVisitorLogInteractEnabled() {
        return config.getBoolean("visitor-log.events.interact", true);
    }

    public static boolean allowSubownerViewVisitorLog() {
        return config.getBoolean("visitor-log.permissions.allow-subowner-view", true);
    }

    public static boolean allowBuilderViewVisitorLog() {
        return config.getBoolean("visitor-log.permissions.allow-builder-view", false);
    }

    public static boolean allowSubownerTransfer() {
        return config.getBoolean("permissions.allow-subowner-transfer", false);
    }

    public static boolean allowSubownerDisclaim() {
        return config.getBoolean("permissions.allow-subowner-disclaim", false);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

}
