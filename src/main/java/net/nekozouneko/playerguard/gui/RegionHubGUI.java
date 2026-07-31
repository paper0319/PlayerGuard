package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.region.RegionRoles;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

/**
 * /pg で開く領域管理ハブ。フラグ設定/メンバー管理/領域情報への入口。
 *
 * レイアウト（1×9）:
 *   [BACK/G][FLAGS][G][MEMBERS][G][INFO][G][LOG/G][CLOSE]
 *      0       1    2     3     4    5   6    7       8
 *   ※ BACK は親GUIがある場合のみ表示（slot 0）
 */
public class RegionHubGUI extends AbstractGUI {

    private static final int SLOT_BACK        = 0;
    private static final int SLOT_FLAGS       = 1;
    private static final int SLOT_MEMBERS     = 3;
    private static final int SLOT_INFO        = 5;
    private static final int SLOT_VISITOR_LOG = 7;
    private static final int SLOT_CLOSE       = 8;

    private static final Material GLASS = Material.BLACK_STAINED_GLASS_PANE;

    private final ProtectedRegion region;

    public RegionHubGUI(Player player, ProtectedRegion region) {
        this(player, region, null);
    }

    public RegionHubGUI(Player player, ProtectedRegion region, AbstractGUI parent) {
        super(player, parent);
        this.region = region;
    }

    @Override
    public void init() {
        if (inventory == null)
            inventory = Bukkit.createInventory(this, 9, ChatColor.AQUA + "■ " + ChatColor.WHITE + "領域の管理");
        inventory.clear();

        // Even-index slots filled with glass (0,2,4,6)
        for (int i = 0; i <= 8; i += 2) {
            inventory.setItem(i, ItemStackBuilder.of(GLASS).name(" ").build());
        }

        // Back button at slot 0 (leftmost) when parent exists
        if (getParent() != null) {
            inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW)
                    .name(ChatColor.WHITE + "← 戻る")
                    .lore(ChatColor.DARK_GRAY + "前の画面へ")
                    .build());
        }

        UUID primary = RegionRoles.getPrimaryOwner(region);
        String primaryName  = primary != null ? Bukkit.getOfflinePlayer(primary).getName() : null;
        String primaryLabel = primary == null ? "(未設定)"
                : primaryName != null ? primaryName : primary.toString();

        inventory.setItem(SLOT_FLAGS, ItemStackBuilder.of(Material.REDSTONE_TORCH)
                .name(ChatColor.YELLOW + "" + ChatColor.BOLD + "フラグ設定")
                .lore(
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.GRAY + "領域の権限フラグを管理",
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.DARK_GRAY + "クリックで開く"
                ).build());

        inventory.setItem(SLOT_MEMBERS, ItemStackBuilder.of(Material.PLAYER_HEAD)
                .name(ChatColor.YELLOW + "" + ChatColor.BOLD + "メンバー管理")
                .lore(
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.GRAY + "オーナー: " + ChatColor.WHITE + primaryLabel,
                    ChatColor.GRAY + "メンバー数: " + ChatColor.WHITE + region.getMembers().size(),
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.DARK_GRAY + "クリックで開く"
                ).build());

        inventory.setItem(SLOT_INFO, ItemStackBuilder.of(Material.PAPER)
                .name(ChatColor.YELLOW + "" + ChatColor.BOLD + "領域情報")
                .lore(
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.GRAY + "ID: " + ChatColor.WHITE + region.getId(),
                    ChatColor.GRAY + "範囲: " + ChatColor.WHITE + String.format(
                        "(%d,%d,%d)→(%d,%d,%d)",
                        region.getMinimumPoint().x(), region.getMinimumPoint().y(), region.getMinimumPoint().z(),
                        region.getMaximumPoint().x(), region.getMaximumPoint().y(), region.getMaximumPoint().z()),
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.DARK_GRAY + "クリックで開く"
                ).build());

        boolean showVisitorLog = PlayerGuard.getInstance().getVisitorLogService() != null && canViewVisitorLog();
        if (showVisitorLog) {
            inventory.setItem(SLOT_VISITOR_LOG, ItemStackBuilder.of(Material.BOOK)
                    .name(ChatColor.YELLOW + "" + ChatColor.BOLD + "訪問者ログ")
                    .lore(
                        ChatColor.DARK_GRAY + "─────────────────",
                        ChatColor.GRAY + "入退場・操作の履歴を確認",
                        ChatColor.DARK_GRAY + "─────────────────",
                        ChatColor.DARK_GRAY + "クリックで開く"
                    ).build());
        } else {
            inventory.setItem(SLOT_VISITOR_LOG, ItemStackBuilder.of(GLASS).name(" ").build());
        }

        inventory.setItem(SLOT_CLOSE, ItemStackBuilder.of(Material.BARRIER)
                .name(ChatColor.RED + "" + ChatColor.BOLD + "閉じる")
                .lore(ChatColor.DARK_GRAY + "インベントリを閉じる")
                .build());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);

        switch (e.getRawSlot()) {
            case SLOT_BACK:
                if (getParent() != null) back();
                break;
            case SLOT_FLAGS:
                new MenuGUI(PlayerGuard.getInstance(), getPlayer(), region, this).open();
                break;
            case SLOT_MEMBERS:
                new MemberGUI(getPlayer(), this, region).open();
                break;
            case SLOT_INFO:
                break;
            case SLOT_VISITOR_LOG:
                if (PlayerGuard.getInstance().getVisitorLogService() != null && canViewVisitorLog())
                    new VisitorLogGUI(getPlayer(), this, region).open();
                break;
            case SLOT_CLOSE:
                getPlayer().closeInventory();
                break;
            default:
                break;
        }
    }

    private boolean canViewVisitorLog() {
        RegionRoles.Role role = RegionRoles.roleOf(region, getPlayer().getUniqueId());
        if (role == RegionRoles.Role.PRIMARY_OWNER) return true;
        if (role == RegionRoles.Role.SUB_OWNER) return PGConfig.allowSubownerViewVisitorLog();
        if (role == RegionRoles.Role.BUILDER) return PGConfig.allowBuilderViewVisitorLog();
        return false;
    }
}
