package net.nekozouneko.playerguard.gui;

import net.nekozouneko.playerguard.PlayerGuard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ProtectionGuiText {
    private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance(Locale.US);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").withZone(ZoneId.systemDefault());

    private ProtectionGuiText() {}

    public static String blocks(long value) { return NUMBER.format(value) + "ブロック"; }
    public static String money(BigDecimal value) { return NUMBER.format(value.setScale(0, RoundingMode.HALF_UP)) + "円"; }
    public static String rate(BigDecimal value) { return value.stripTrailingZeros().toPlainString() + "円"; }
    public static String date(Instant value) { return value == null ? "不明" : DATE.format(value); }

    public static BigDecimal refund(BigDecimal paid) {
        PlayerGuard plugin = PlayerGuard.getInstance();
        if (plugin == null || !plugin.getPaidExtensionConfig().allowRefundOnDelete()) return BigDecimal.ZERO.setScale(2);
        return paid.multiply(plugin.getPaidExtensionConfig().refundRate()).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal refund(BigDecimal paid, Instant createdAt, Instant now) {
        PlayerGuard plugin = PlayerGuard.getInstance();
        if (plugin == null || !plugin.getPaidExtensionConfig().allowRefundOnDelete()) return BigDecimal.ZERO.setScale(2);
        return plugin.getPaidExtensionConfig().refundPolicy().refundAmount(paid, createdAt, now);
    }

    public static BigDecimal refundRate(Instant createdAt, Instant now) {
        PlayerGuard plugin = PlayerGuard.getInstance();
        if (plugin == null || !plugin.getPaidExtensionConfig().allowRefundOnDelete()) return BigDecimal.ZERO;
        return plugin.getPaidExtensionConfig().refundPolicy().rateFor(createdAt, now);
    }

    public static String percent(BigDecimal rate) { return rate.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString() + "%"; }

    public static String elapsed(Instant createdAt, Instant now) {
        if (createdAt == null || now == null) return "不明";
        Duration d = Duration.between(createdAt, now);
        long days = d.toDays();
        if (days > 0) return days + "日";
        long hours = d.toHours();
        if (hours > 0) return hours + "時間";
        return Math.max(0, d.toMinutes()) + "分";
    }
}