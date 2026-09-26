package net.nekozouneko.playerguard.gui;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.paid.ProtectionTeleportRepository;
import net.nekozouneko.playerguard.region.ProtectionAccess;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProtectionTeleportGUI extends AbstractGUI {
    private static final int SIZE = 27;
    private static final int SLOT_INFO = 4;
    private static final int SLOT_SET = 11;
    private static final int SLOT_TELEPORT = 10;
    private static final int SLOT_CHANGE = 13;
    private static final int SLOT_DELETE = 15;
    private static final int SLOT_BACK = 26;
    static final Set<String> LOCKL = ConcurrentHashMap.newKeySet();

    public static void clearLocks() {
        LOCKL.clear();
    }

    private final ProtectedRegion region;
    private final World world;

    public ProtectionTeleportGUI(Player player, AbstractGUI parent, ProtectedRegion region, World world) {
        super(player, parent);
        this.region = region;
        this.world = world;
    }

    @Override public void init() {
        if (inventory == null) inventory = Bukkit.createInventory(this, SIZE, ChatColor.BLACK + "テレポート管理");
        inventory.clear();
        ItemStack glass = ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, glass);

        ProtectionTeleportRepository.TeleportPoint point = PlayerGuard.getInstance().getProtectionTeleportRepository().find(region);
        boolean manage = ProtectionAccess.canManageTeleport(getPlayer(), region);
        inventory.setItem(SLOT_INFO, infoItem(point));
        if (point == null) {
            if (manage) inventory.setItem(SLOT_SET, setupItem("テレポート地点を設定", "現在地点をこの保護の移動先に設定します。"));
            else inventory.setItem(SLOT_SET, ItemStackBuilder.of(Material.BARRIER).name(ChatColor.RED + "テレポート地点は未設定です")
                    .lore(ChatColor.GRAY + "地点の作成はオーナーのみです。").build());
        } else {
            BigDecimal cost = getPlayer().hasPermission("playerguard.admin.teleport") ? BigDecimal.ZERO : PGConfig.getTeleportCost();
            BigDecimal balance = balance();
            inventory.setItem(SLOT_TELEPORT, ItemStackBuilder.of(Material.ENDER_PEARL).name(ChatColor.AQUA + "" + ChatColor.BOLD + "保護へテレポート")
                    .lore(ChatColor.DARK_GRAY + "────────────",
                            ChatColor.GRAY + "保護名: " + ChatColor.WHITE + PlayerGuard.getInstance().getProtectionNameRepository().displayName(region),
                            ChatColor.GRAY + "移動先: " + ChatColor.WHITE + point.worldName(),
                            ChatColor.GRAY + "テレポート料金: " + (getPlayer().hasPermission("playerguard.admin.teleport") ? ChatColor.GREEN + ProtectionGuiText.money(BigDecimal.ZERO) : ChatColor.YELLOW + ProtectionGuiText.money(cost)),
                            getPlayer().hasPermission("playerguard.admin.teleport") ? ChatColor.GREEN + "管理者権限により無料です" : ChatColor.GRAY + "",
                            ChatColor.GRAY + "所持金: " + ChatColor.WHITE + ProtectionGuiText.money(balance),
                            ChatColor.GRAY + "移動後残高: " + ChatColor.WHITE + ProtectionGuiText.money(balance.subtract(cost)),
                            ChatColor.DARK_GRAY + "────────────",
                            ChatColor.GREEN + "クリックでテレポート")
                    .build());
            if (manage) {
                inventory.setItem(SLOT_CHANGE, setupItem("テレポート地点を変更", "現在地点へテレポート地点を変更します。"));
                inventory.setItem(SLOT_DELETE, ItemStackBuilder.of(Material.BARRIER).name(ChatColor.RED + "テレポート地点を削除")
                        .lore(ChatColor.DARK_GRAY + "────────────",
                                ChatColor.GRAY + "削除しても返金はありません。",
                                ChatColor.YELLOW + "クリックで確認画面を開く")
                        .build());
            }
        }
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "戻る").build());
    }

    private ItemStack setupItem(String title, String description) {
        return ItemStackBuilder.of(Material.COMPASS).name(ChatColor.AQUA + "" + ChatColor.BOLD + title)
                .lore(ChatColor.DARK_GRAY + "────────────",
                        ChatColor.GRAY + description,
                        ChatColor.GRAY + "設定費用: " + (getPlayer().hasPermission("playerguard.admin.teleport.manage") ? ChatColor.GREEN + ProtectionGuiText.money(BigDecimal.ZERO) : ChatColor.YELLOW + ProtectionGuiText.money(PGConfig.getTeleportSetupCost())),
                        getPlayer().hasPermission("playerguard.admin.teleport.manage") ? ChatColor.GREEN + "管理者権限により無料です" : ChatColor.GRAY + "",
                        ChatColor.DARK_GRAY + "────────────",
                        ChatColor.GREEN + "クリックで設定")
                .build();
    }

    private ItemStack infoItem(ProtectionTeleportRepository.TeleportPoint point) {
        if (point == null) {
            return ItemStackBuilder.of(Material.COMPASS).name(ChatColor.AQUA + "現在のテレポート地点")
                    .lore(ChatColor.DARK_GRAY + "────────────", ChatColor.GRAY + "未設定です。")
                    .build();
        }
        return ItemStackBuilder.of(Material.COMPASS).name(ChatColor.AQUA + "現在のテレポート地点")
                .lore(ChatColor.DARK_GRAY + "────────────",
                        ChatColor.GRAY + "ワールド: " + ChatColor.WHITE + point.worldName(),
                        ChatColor.GRAY + "座標: " + ChatColor.WHITE + xyz(point),
                        ChatColor.GRAY + "設定者: " + ChatColor.WHITE + name(point.setter()),
                        ChatColor.GRAY + "設定日時: " + ChatColor.WHITE + ProtectionGuiText.date(point.createdAt()))
                .build();
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);
        switch (e.getRawSlot()) {
            case SLOT_BACK: back(); break;
            case SLOT_SET: setup(false); break;
            case SLOT_CHANGE: setup(true); break;
            case SLOT_DELETE:
                if (!ProtectionAccess.canManageTeleport(getPlayer(), region)) { getPlayer().sendMessage(ChatColor.RED + "テレポート地点を削除できるのは正式なオーナーのみです。"); return; }
                new ProtectionTeleportDeleteConfirmGUI(getPlayer(), this, region).open();
                break;
            case SLOT_TELEPORT: teleport(); break;
            default: break;
        }
    }

    private void setup(boolean change) {
        Player player = getPlayer();
        if (!PGConfig.isProtectionTeleportEnabled()) { player.sendMessage(ChatColor.RED + "テレポート機能は利用できません。"); return; }
        if (!player.hasPermission("playerguard.teleport.manage") && !player.hasPermission("playerguard.admin.teleport.manage")) { player.sendMessage(ChatColor.RED + "テレポート地点を管理する権限がありません。"); return; }
        if (!ProtectionAccess.canManageTeleport(player, region)) { player.sendMessage(ChatColor.RED + "テレポート地点を設定できるのは正式なオーナーのみです。"); return; }
        if (!player.getWorld().equals(world)) { player.sendMessage(ChatColor.RED + "この保護と同じワールドで設定してください。"); return; }
        ProtectionTeleportRepository repo = PlayerGuard.getInstance().getProtectionTeleportRepository();
        if (!change && repo.find(region) != null) { player.sendMessage(ChatColor.RED + "テレポート地点はすでに設定されています。"); return; }
        Location loc = player.getLocation();
        if (!contains(loc) || !safe(loc)) { player.sendMessage(ChatColor.RED + "現在地点はテレポート地点として設定できません。"); return; }
        String key = player.getUniqueId() + ":" + region.getId() + ":setup";
        if (!LOCKL.add(key)) { player.sendMessage(ChatColor.YELLOW + "処理中です。しばらくお待ちください。"); return; }
        BigDecimal cost = player.hasPermission("playerguard.admin.teleport.manage") ? BigDecimal.ZERO : PGConfig.getTeleportSetupCost();
        try {
            if (!charge(player, cost, "テレポート地点の設定")) return;
            try {
                repo.save(region, loc, player.getUniqueId());
            } catch (RuntimeException ex) {
                refund(player, cost);
                player.sendMessage(ChatColor.RED + "テレポート地点の保存に失敗したため、支払いを返金しました。");
                PlayerGuard.getInstance().getProtectionLogService().log(region, "返金", player.getUniqueId(), null, cost.toPlainString(), cost.toPlainString(), "teleport setup save failed");
                return;
            }
            PlayerGuard.getInstance().getProtectionLogService().log(region, change ? "テレポート地点変更" : "テレポート地点設定", player.getUniqueId(), null, null, xyz(loc), "支払額=" + cost.toPlainString());
            PlayerGuard.getInstance().getProtectionLogService().log(region, "テレポート設定費用支払い", player.getUniqueId(), null, null, cost.toPlainString(), null);
            player.sendMessage(ChatColor.GREEN + "テレポート地点を設定しました。");
            init();
        } finally {
            LOCKL.remove(key);
        }
    }

    void deleteTeleportPoint() {
        Player player = getPlayer();
        if ((!player.hasPermission("playerguard.teleport.manage") && !player.hasPermission("playerguard.admin.teleport.manage")) || !ProtectionAccess.canManageTeleport(player, region)) {
            player.sendMessage(ChatColor.RED + "テレポート地点を削除できるのは正式なオーナーのみです。");
            return;
        }
        String key = player.getUniqueId() + ":" + region.getId() + ":delete";
        if (!LOCKL.add(key)) { player.sendMessage(ChatColor.YELLOW + "処理中です。しばらくお待ちください。"); return; }
        try {
            PlayerGuard.getInstance().getProtectionTeleportRepository().delete(region);
            PlayerGuard.getInstance().getProtectionLogService().log(region, "テレポート地点削除", player.getUniqueId(), null, null, null, null);
            player.sendMessage(ChatColor.YELLOW + "テレポート地点を削除しました。");
            init();
            open();
        } finally {
            LOCKL.remove(key);
        }
    }

    private void teleport() {
        Player player = getPlayer();
        if (!PGConfig.isProtectionTeleportEnabled()) { player.sendMessage(ChatColor.RED + "テレポート機能は利用できません。"); return; }
        if (!player.hasPermission("playerguard.teleport.use") && !player.hasPermission("playerguard.admin.teleport")) { player.sendMessage(ChatColor.RED + "テレポートを利用する権限がありません。"); return; }
        if (!ProtectionAccess.canUseTeleport(player, region) && !player.hasPermission("playerguard.admin.teleport")) { player.sendMessage(ChatColor.RED + "この保護へテレポートする権限がありません。"); return; }
        ProtectionTeleportRepository.TeleportPoint point = PlayerGuard.getInstance().getProtectionTeleportRepository().find(region);
        if (point == null) { player.sendMessage(ChatColor.RED + "テレポート地点が設定されていません。"); return; }
        Location loc = point.location();
        if (loc == null || !contains(loc) || !safe(loc)) {
            player.sendMessage(ChatColor.RED + "保存されたテレポート地点が安全ではありません。");
            player.sendMessage(ChatColor.GRAY + "オーナーに再設定を依頼してください。");
            PlayerGuard.getInstance().getProtectionLogService().log(region, "テレポート失敗", player.getUniqueId(), null, null, point.worldName(), "unsafe");
            return;
        }
        String key = player.getUniqueId() + ":" + region.getId() + ":teleport";
        if (!LOCKL.add(key)) { player.sendMessage(ChatColor.YELLOW + "テレポート処理中です。しばらくお待ちください。"); return; }
        BigDecimal cost = getPlayer().hasPermission("playerguard.admin.teleport") ? BigDecimal.ZERO : PGConfig.getTeleportCost();
        try {
            if (!charge(player, cost, "テレポート")) { LOCKL.remove(key); return; }
            player.teleportAsync(loc).thenAccept(success -> {
                PlayerGuard plugin = PlayerGuard.getInstance();
                if (plugin == null || !plugin.isRuntimeReady() || plugin.getScheduler() == null) {
                    LOCKL.remove(key);
                    return;
                }
                plugin.getScheduler().runOnEntity(player, () -> {
                    LOCKL.remove(key);
                    if (success) {
                        plugin.getProtectionLogService().log(region, "テレポート実行", player.getUniqueId(), null, null, xyz(loc), null);
                        plugin.getProtectionLogService().log(region, "テレポート利用料金支払い", player.getUniqueId(), null, null, cost.toPlainString(), null);
                        player.sendMessage(ChatColor.GREEN + "保護へテレポートしました。");
                    } else {
                        refund(player, cost);
                        plugin.getProtectionLogService().log(region, "テレポート失敗", player.getUniqueId(), null, null, xyz(loc), "refunded=" + cost.toPlainString());
                        plugin.getProtectionLogService().log(region, "返金", player.getUniqueId(), null, cost.toPlainString(), cost.toPlainString(), "teleport failed");
                        player.sendMessage(ChatColor.RED + "テレポートに失敗したため、料金を返金しました。");
                    }
                });
            });
        } catch (RuntimeException ex) {
            refund(player, cost);
            LOCKL.remove(key);
            PlayerGuard.getInstance().getProtectionLogService().log(region, "テレポート失敗", player.getUniqueId(), null, null, null, ex.getClass().getSimpleName());
            player.sendMessage(ChatColor.RED + "テレポートに失敗したため、料金を返金しました。");
        }
    }

    private boolean charge(Player player, BigDecimal cost, String action) {
        if (cost.signum() <= 0 || player.hasPermission("playerguard.admin.teleport.manage") || player.hasPermission("playerguard.admin.teleport")) return true;
        if (!PlayerGuard.getInstance().getEconomyTransactionService().available()) {
            player.sendMessage(ChatColor.RED + "経済プラグインが見つかりません。");
            return false;
        }
        BigDecimal balance = balance();
        if (balance.compareTo(cost) < 0) {
            player.sendMessage(ChatColor.RED + action + "には " + ChatColor.YELLOW + ProtectionGuiText.money(cost) + ChatColor.RED + " 必要です。");
            player.sendMessage(ChatColor.GRAY + "所持金: " + ChatColor.WHITE + ProtectionGuiText.money(balance));
            return false;
        }
        if (!PlayerGuard.getInstance().getEconomyTransactionService().withdraw(player, cost)) {
            player.sendMessage(ChatColor.RED + action + "の支払いに失敗しました。");
            return false;
        }
        return true;
    }

    private void refund(Player player, BigDecimal cost) {
        if (cost.signum() > 0) PlayerGuard.getInstance().getEconomyTransactionService().deposit(player, cost);
    }

    private BigDecimal balance() {
        return PlayerGuard.getInstance().getEconomyTransactionService().available()
                ? BigDecimal.valueOf(PlayerGuard.getInstance().getEconomyTransactionService().balance(getPlayer()))
                : BigDecimal.ZERO;
    }

    private boolean contains(Location loc) {
        return loc.getWorld() != null && loc.getWorld().equals(world) && region.contains(BukkitAdapter.asBlockVector(loc));
    }

    private boolean safe(Location loc) {
        if (loc == null || loc.getWorld() == null || loc.getY() <= loc.getWorld().getMinHeight()) return false;
        Block feet = loc.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block below = feet.getRelative(0, -1, 0);
        return !below.isLiquid() && below.getType().isSolid() && feet.isPassable() && head.isPassable();
    }

    private String xyz(ProtectionTeleportRepository.TeleportPoint p) {
        return String.format("X%d Y%d Z%d", (int) Math.floor(p.x()), (int) Math.floor(p.y()), (int) Math.floor(p.z()));
    }

    private String xyz(Location loc) {
        return String.format("X%d Y%d Z%d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private String name(UUID uuid) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
        return p.getName() == null ? uuid.toString() : p.getName();
    }
}
