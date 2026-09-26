package net.nekozouneko.playerguard.gui;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.paid.ProtectionCustomizationRepository;
import net.nekozouneko.playerguard.paid.ProtectionPaymentRecord;
import net.nekozouneko.playerguard.region.RegionRoles;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ProtectionDetailGUI extends AbstractGUI {
    private static final int SIZE = 27;
    private static final int SLOT_BASIC = 10;
    private static final int SLOT_RANGE = 11;
    private static final int SLOT_PRICE = 12;
    private static final int SLOT_REFUND = 13;
    private static final int SLOT_MEMBERS = 14;
    private static final int SLOT_STATS = 15;
    private static final int SLOT_DATES = 16;
    private static final int SLOT_BACK = 26;
    private final ProtectedRegion region;
    private final World world;

    public ProtectionDetailGUI(Player player, AbstractGUI parent, ProtectedRegion region, World world) {
        super(player, parent);
        this.region = region;
        this.world = world;
    }

    @Override public void init() {
        if (inventory == null) inventory = Bukkit.createInventory(this, SIZE, ChatColor.BLACK + "土地保護情報詳細");
        inventory.clear();
        ItemStack glass = ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, glass);

        inventory.setItem(SLOT_BASIC, basicItem());
        inventory.setItem(SLOT_RANGE, rangeItem());
        inventory.setItem(SLOT_PRICE, priceItem());
        inventory.setItem(SLOT_REFUND, refundItem());
        inventory.setItem(SLOT_MEMBERS, memberItem());
        inventory.setItem(SLOT_STATS, statsItem());
        inventory.setItem(SLOT_DATES, datesItem());
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "戻る").build());
    }

    private ItemStack basicItem() {
        ProtectionCustomizationRepository.Category category = PlayerGuard.getInstance().getProtectionCustomizationRepository().category(region);
        String name = PlayerGuard.getInstance().getProtectionNameRepository().displayName(region);
        return ItemStackBuilder.of(Material.NETHER_STAR).name(ChatColor.AQUA + "基本情報")
                .lore(ChatColor.DARK_GRAY + "────────────",
                        ChatColor.GRAY + "保護名: " + ChatColor.WHITE + name,
                        ChatColor.GRAY + "保護ID: " + ChatColor.WHITE + region.getId(),
                        ChatColor.GRAY + "カテゴリ: " + ChatColor.YELLOW + category.label,
                        ChatColor.GRAY + "ワールド: " + ChatColor.WHITE + world.getName())
                .build();
    }
    private ItemStack rangeItem() {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        return ItemStackBuilder.of(Material.COMPASS).name(ChatColor.GREEN + "範囲情報")
                .lore(ChatColor.DARK_GRAY + "────────────",
                        ChatColor.GRAY + "開始座標:",
                        ChatColor.WHITE + pos(min),
                        ChatColor.GRAY + "終了座標:",
                        ChatColor.WHITE + pos(max),
                        ChatColor.GRAY + "サイズ: " + ChatColor.WHITE + dimensions(),
                        ChatColor.GRAY + "体積: " + ChatColor.YELLOW + ProtectionGuiText.blocks(region.volume()))
                .build();
    }

    private ItemStack priceItem() {
        ProtectionPaymentRecord payment = PlayerGuard.getInstance().getProtectionPaymentRepository().find(region);
        BigDecimal paid = payment == null ? BigDecimal.ZERO : payment.amountPaid();
        long paidVolume = payment == null ? 0L : payment.paidVolume();
        long freeVolume = Math.max(0L, region.volume() - paidVolume);
        return ItemStackBuilder.of(Material.EMERALD).name(ChatColor.GOLD + "料金情報")
                .lore(ChatColor.DARK_GRAY + "────────────",
                        ChatColor.GRAY + "無料体積: " + ChatColor.GREEN + ProtectionGuiText.blocks(freeVolume),
                        ChatColor.GRAY + "有料体積: " + ChatColor.RED + ProtectionGuiText.blocks(paidVolume),
                        ChatColor.GRAY + "支払金額: " + ChatColor.YELLOW + ProtectionGuiText.money(paid),
                        ChatColor.GRAY + "現在の単価: " + ChatColor.YELLOW + currentRate() + "/ブロック")
                .build();
    }

    private ItemStack refundItem() {
        ProtectionPaymentRecord payment = PlayerGuard.getInstance().getProtectionPaymentRepository().find(region);
        Instant now = Instant.now();
        BigDecimal paid = payment == null ? BigDecimal.ZERO : payment.amountPaid();
        BigDecimal rate = payment == null ? BigDecimal.ZERO : ProtectionGuiText.refundRate(payment.createdAt(), now);
        BigDecimal refund = payment == null ? BigDecimal.ZERO : ProtectionGuiText.refund(paid, payment.createdAt(), now);
        return ItemStackBuilder.of(Material.GOLD_INGOT).name(ChatColor.YELLOW + "返金情報")
                .lore(ChatColor.DARK_GRAY + "────────────",
                        ChatColor.GRAY + "現在の返金率: " + ChatColor.GREEN + ProtectionGuiText.percent(rate),
                        ChatColor.GRAY + "返金予定額: " + ChatColor.GREEN + ProtectionGuiText.money(refund),
                        ChatColor.GRAY + "クーリングオフ残り: " + ChatColor.WHITE + coolingOffLeft(payment, now))
                .build();
    }

    private ItemStack memberItem() {
        UUID primary = RegionRoles.getPrimaryOwner(region);
        int subowners = Math.max(0, region.getOwners().getUniqueIds().size() - (primary == null ? 0 : 1));
        int members = region.getMembers().getUniqueIds().size();
        return ItemStackBuilder.of(Material.PLAYER_HEAD).name(ChatColor.LIGHT_PURPLE + "メンバー情報")
                .lore(ChatColor.DARK_GRAY + "────────────",
                        ChatColor.GRAY + "オーナー: " + ChatColor.WHITE + ownerName(primary),
                        ChatColor.GRAY + "サブオーナー: " + ChatColor.WHITE + subowners + "人",
                        ChatColor.GRAY + "メンバー: " + ChatColor.WHITE + members + "人")
                .build();
    }

    private ItemStack statsItem() {
        UUID primary = RegionRoles.getPrimaryOwner(region);
        RegionRoles.Role role = RegionRoles.roleOf(region, getPlayer().getUniqueId());
        boolean admin = getPlayer().hasPermission("playerguard.admin.manage");
        String position = admin ? ChatColor.RED + "管理者" : role == RegionRoles.Role.PRIMARY_OWNER ? ChatColor.GREEN + "オーナー" : role == RegionRoles.Role.SUB_OWNER ? ChatColor.GOLD + "サブオーナー" : role == RegionRoles.Role.BUILDER ? ChatColor.AQUA + "メンバー" : ChatColor.GRAY + "なし";
        int subowners = Math.max(0, region.getOwners().getUniqueIds().size() - (primary == null ? 0 : 1));
        int members = region.getMembers().getUniqueIds().size();
        return ItemStackBuilder.of(Material.PLAYER_HEAD).name(ChatColor.LIGHT_PURPLE + "§l所有・権限情報")
                .lore(ChatColor.DARK_GRAY + "────────────", ChatColor.GRAY + "現在のオーナー: " + ChatColor.WHITE + ownerName(primary), ChatColor.GRAY + "あなたの立場: " + position, ChatColor.GRAY + "サブオーナー数: " + ChatColor.WHITE + subowners + "人", ChatColor.GRAY + "メンバー数: " + ChatColor.WHITE + members + "人", ChatColor.GRAY + "管理可能: " + (admin || role == RegionRoles.Role.PRIMARY_OWNER || role == RegionRoles.Role.SUB_OWNER ? ChatColor.GREEN + "はい" : ChatColor.RED + "いいえ"))
                .build();
    }
    private ItemStack datesItem() {
        ProtectionPaymentRecord payment = PlayerGuard.getInstance().getProtectionPaymentRepository().find(region);
        Instant now = Instant.now();
        java.util.List<net.nekozouneko.playerguard.paid.ProtectionLogEntry> entries = PlayerGuard.getInstance().getProtectionLogService().getEntries(region, true);
        String updated = entries.isEmpty() ? "不明" : ProtectionGuiText.date(entries.get(0).instant());
        return ItemStackBuilder.of(Material.CLOCK).name(ChatColor.GRAY + "日時情報")
                .lore(ChatColor.DARK_GRAY + "────────────",
                        ChatColor.GRAY + "作成日時: " + ChatColor.WHITE + (payment == null ? "不明" : ProtectionGuiText.date(payment.createdAt())),
                        ChatColor.GRAY + "最終更新: " + ChatColor.WHITE + updated,
                        ChatColor.GRAY + "経過時間: " + ChatColor.WHITE + (payment == null ? "不明" : ProtectionGuiText.elapsed(payment.createdAt(), now)))
                .build();
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);
        if (e.getRawSlot() == SLOT_BACK) back();
    }

    private String pos(BlockVector3 p) { return "X" + p.x() + " Y" + p.y() + " Z" + p.z(); }

    private String dimensions() {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        long x = Math.abs(max.x() - min.x()) + 1L;
        long y = Math.abs(max.y() - min.y()) + 1L;
        long z = Math.abs(max.z() - min.z()) + 1L;
        return String.format("%,d × %,d × %,d", x, y, z);
    }

    private String currentRate() {
        java.util.NavigableMap<Long, BigDecimal> rates = PlayerGuard.getInstance().getPaidExtensionConfig().rates();
        if (rates.isEmpty()) return "0円";
        Map.Entry<Long, BigDecimal> entry = rates.floorEntry(Math.max(PlayerGuard.getInstance().getProtectLimit(getPlayer()), region.volume()));
        if (entry == null) entry = rates.firstEntry();
        return ProtectionGuiText.rate(entry.getValue());
    }

    private String coolingOffLeft(ProtectionPaymentRecord payment, Instant now) {
        if (payment == null || payment.createdAt() == null) return "不明";
        long minutes = PlayerGuard.getInstance().getPaidExtensionConfig().refundPolicy().coolingOffMinutes();
        Instant end = payment.createdAt().plusSeconds(minutes * 60L);
        if (!end.isAfter(now)) return "終了";
        long left = java.time.Duration.between(now, end).toMinutes();
        return Math.max(1L, left) + "分";
    }

    private String ownerName(UUID uuid) {
        if (uuid == null) return "不明";
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() == null ? uuid.toString() : player.getName();
    }
}
