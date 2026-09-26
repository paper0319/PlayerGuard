package net.nekozouneko.playerguard;

import org.bukkit.ChatColor;

public final class PGMessages {

    private static final String PREFIX = ChatColor.DARK_AQUA + "[PG] ";

    private PGMessages() {
    }

    public static String success(String format, Object... args) {
        return compose(ChatColor.GREEN, "OK", format, args);
    }

    public static String error(String format, Object... args) {
        return compose(ChatColor.RED, "ERR", format, args);
    }

    public static String warn(String format, Object... args) {
        return compose(ChatColor.GOLD, "WARN", format, args);
    }

    public static String info(String format, Object... args) {
        return compose(ChatColor.AQUA, "INFO", format, args);
    }

    public static String title(String format, Object... args) {
        return PREFIX + ChatColor.YELLOW + String.format(format, args);
    }

    public static String detail(String label, Object value) {
        return ChatColor.GRAY + label + ": " + ChatColor.WHITE + value;
    }

    public static String highlight(Object value) {
        return ChatColor.AQUA + String.valueOf(value) + ChatColor.WHITE;
    }

    public static String deniedEntryTitle() {
        return ChatColor.RED + "" + ChatColor.BOLD + "その土地には入れません";
    }

    public static String deniedEntrySubtitle() {
        return ChatColor.GRAY + "メンバー以外は入場できません";
    }

    private static String compose(ChatColor accent, String label, String format, Object... args) {
        return PREFIX + accent + "[" + label + "] " + ChatColor.WHITE + String.format(format, args);
    }
}
