package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.region.ProtectionAccess;
import net.nekozouneko.playerguard.region.RegionRentals;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

/**
 * 建築権の貸出期間を「+/-」ボタンで設定するシンプルGUI（36スロット / 4行）。
 *
 * レイアウト:
 *   Row 0: [G][G][+1日][+1時][+10分][+1分][+1秒][G][G]  ← 緑 増加
 *   Row 1: [G][G][◆1日][◆1時][◆10分][◆1分][◆1秒][G][G] ← 単位ラベル
 *   Row 2: [G][G][-1日][-1時][-10分][-1分][-1秒][G][G]  ← 赤 減少
 *   Row 3: [←戻る][G][G][G][リセット][G][G][G][✔貸出確定]
 */
public class RentalDurationGUI extends AbstractGUI {

    private static final long SECOND = 1_000L;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR   = 60 * MINUTE;
    private static final long DAY    = 24 * HOUR;
    private static final long MAX    = 30 * DAY;

    // 5列（中央配置: col 2〜6）
    private static final long[]   INCREMENTS = { DAY, HOUR, 10 * MINUTE, MINUTE, SECOND };
    private static final String[] COL_LABELS  = { "1日", "1時間", "10分", "1分", "1秒" };
    private static final int      NUM_COLS    = 5;
    private static final int      COL_OFFSET  = 2;   // col 2,3,4,5,6

    // 各行の先頭スロット
    private static final int ROW_PLUS  = 0;
    private static final int ROW_LABEL = 9;
    private static final int ROW_MINUS = 18;

    // ナビゲーション（Row 3）
    private static final int SLOT_BACK    = 35;
    private static final int SLOT_RESET   = 31;
    private static final int SLOT_CONFIRM = 34;

    private static final Material GLASS = Material.BLACK_STAINED_GLASS_PANE;

    private final ProtectedRegion region;
    private final UUID target;
    private final String targetName;
    private long totalMillis = 0;

    public RentalDurationGUI(Player player, AbstractGUI parent,
                             ProtectedRegion region, UUID target, String targetName) {
        super(player, parent);
        this.region = region;
        this.target = target;
        this.targetName = targetName;
    }

    @Override
    public void init() {
        if (inventory == null)
            inventory = Bukkit.createInventory(this, 36,
                ChatColor.AQUA + "⏱ " + ChatColor.WHITE + "貸出期間: " + targetName);
        inventory.clear();

        // 全スロットを黒ガラスで初期化
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, ItemStackBuilder.of(GLASS).name(" ").build());
        }

        // ─── Rows 0-2: +/ラベル/- ───
        for (int i = 0; i < NUM_COLS; i++) {
            int col       = COL_OFFSET + i;
            int plusSlot  = ROW_PLUS  + col;   // 2, 3, 4, 5, 6
            int labelSlot = ROW_LABEL + col;   // 11,12,13,14,15
            int minusSlot = ROW_MINUS + col;   // 20,21,22,23,24

            inventory.setItem(plusSlot, ItemStackBuilder.of(Material.LIME_DYE)
                    .name(ChatColor.GREEN + "" + ChatColor.BOLD + "+" + COL_LABELS[i])
                    .lore(
                        ChatColor.DARK_GRAY + "クリックで " + ChatColor.GREEN + "+" + COL_LABELS[i]
                    ).build());

            inventory.setItem(labelSlot, ItemStackBuilder.of(Material.CYAN_STAINED_GLASS_PANE)
                    .name(ChatColor.AQUA + "" + ChatColor.BOLD + COL_LABELS[i])
                    .build());

            inventory.setItem(minusSlot, ItemStackBuilder.of(Material.RED_DYE)
                    .name(ChatColor.RED + "" + ChatColor.BOLD + "-" + COL_LABELS[i])
                    .lore(
                        ChatColor.DARK_GRAY + "クリックで " + ChatColor.RED + "-" + COL_LABELS[i]
                    ).build());
        }

        // ─── Row 3: ナビゲーション ───
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW)
                .name(ChatColor.WHITE + "← 戻る")
                .build());

        inventory.setItem(SLOT_RESET, ItemStackBuilder.of(Material.BARRIER)
                .name(ChatColor.RED + "リセット")
                .lore(ChatColor.DARK_GRAY + "設定をゼロに戻す")
                .build());

        boolean hasTime = totalMillis > 0;
        String totalStr = hasTime ? formatDuration(totalMillis) : null;

        if (hasTime) {
            inventory.setItem(SLOT_CONFIRM, ItemStackBuilder.of(Material.EMERALD)
                    .name(ChatColor.GREEN + "" + ChatColor.BOLD + "✔ 貸し出す")
                    .lore(
                        ChatColor.GRAY + "期間: " + ChatColor.WHITE + totalStr,
                        ChatColor.DARK_GRAY + "クリックで確定"
                    ).build());
        } else {
            inventory.setItem(SLOT_CONFIRM, ItemStackBuilder.of(Material.GRAY_DYE)
                    .name(ChatColor.GRAY + "貸し出す")
                    .lore(ChatColor.RED + "時間を設定してください")
                    .build());
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == SLOT_BACK)  { back(); return; }
        if (slot == SLOT_RESET) { totalMillis = 0; playTick(); init(); return; }
        if (slot == SLOT_CONFIRM && totalMillis > 0) { rent(totalMillis, formatDuration(totalMillis)); return; }

        for (int i = 0; i < NUM_COLS; i++) {
            int col = COL_OFFSET + i;
            if (slot == ROW_PLUS  + col) { totalMillis = Math.min(totalMillis + INCREMENTS[i], MAX); playTick(); init(); return; }
            if (slot == ROW_MINUS + col) { totalMillis = Math.max(0, totalMillis - INCREMENTS[i]);  playTick(); init(); return; }
        }
    }

    private void playTick() {
        getPlayer().playSound(getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 1, 2);
    }

    private void rent(long duration, String label) {
        if (!ProtectionAccess.canManageMembers(getPlayer(), region)) {
            getPlayer().sendMessage(PGMessages.error("建築権を貸し出す権限がありません。"));
            back();
            return;
        }
        RegionRentals.RentResult result =
                RegionRentals.rent(region, target, duration, System.currentTimeMillis());
        switch (result) {
            case RENTED:
            case EXTENDED:
                getPlayer().sendMessage(PGMessages.success(
                        "%s に領域 %s の建築権を %s 貸し出しました。",
                        PGMessages.highlight(targetName),
                        PGMessages.highlight(region.getId()),
                        PGMessages.highlight(label)
                ));
                getPlayer().playSound(getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 10, 2);
                Player t = Bukkit.getPlayer(target);
                if (t != null) {
                    t.sendMessage(PGMessages.info(
                            "%s から領域 %s の建築権を %s 貸与されました。",
                            PGMessages.highlight(getPlayer().getName()),
                            PGMessages.highlight(region.getId()),
                            PGMessages.highlight(label)
                    ));
                }
                break;
            case ALREADY_MEMBER:
                getPlayer().sendMessage(PGMessages.warn("そのプレイヤーはすでにメンバーです。"));
                break;
            default:
                getPlayer().sendMessage(PGMessages.error("建築権の貸出に失敗しました。"));
                break;
        }
        back();
    }

    private String formatDuration(long millis) {
        long s = millis / SECOND;
        long m = s / 60; s %= 60;
        long h = m / 60; m %= 60;
        long d = h / 24; h %= 24;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("日");
        if (h > 0) sb.append(h).append("時間");
        if (m > 0) sb.append(m).append("分");
        if (s > 0 || sb.length() == 0) sb.append(s).append("秒");
        return sb.toString();
    }
}
