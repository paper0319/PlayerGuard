package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.visitlog.VisitorLogEntry;
import net.nekozouneko.playerguard.visitlog.VisitorLogType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VisitorLogGUI extends AbstractGUI {

    private static final int SIZE      = 54;
    private static final int PAGE_SIZE = 45;

    // Nav bar: [BACK(45)][G][PREV(47)][G][CLOLE(49)][G][NEXT(51)][G][G]
    private static final int SLOT_BACK  = 45;
    private static final int SLOT_PREV  = 47;
    private static final int SLOT_NEXT  = 51;

    private static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("MM/dd HH:mm:ss", Locale.JAPAN);

    private static final Material GLASS = Material.BLACK_STAINED_GLASS_PANE;

    private final ProtectedRegion region;
    private final String worldName;
    private int page;

    public VisitorLogGUI(Player player, AbstractGUI parent, ProtectedRegion region) {
        super(player, parent);
        this.region = region;
        this.worldName = player.getWorld().getName();
    }

    @Override
    public void init() {
        if (PlayerGuard.getInstance().getVisitorLogService() == null) {
            getPlayer().closeInventory();
            return;
        }
        if (inventory == null)
            inventory = Bukkit.createInventory(this, SIZE,
                ChatColor.AQUA + "■ " + ChatColor.WHITE + "訪問者ログ: " + region.getId());
        inventory.clear();

        // Fill bottom nav bar with glass
        ItemStack glass = ItemStackBuilder.of(GLASS).name(" ").build();
        for (int i = PAGE_SIZE; i < SIZE; i++) inventory.setItem(i, glass);

        List<VisitorLogEntry> entries = PlayerGuard.getInstance().getVisitorLogService()
                .getEntries(worldName, region.getId());

        int maxPage = entries.isEmpty() ? 0 : (entries.size() - 1) / PAGE_SIZE;
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= entries.size()) break;
            inventory.setItem(i, toItem(entries.get(idx)));
        }

        // Back (left-most)
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW)
                .name(ChatColor.WHITE + "← 戻る")
                .lore(ChatColor.DARK_GRAY + "前の画面へ")
                .build());

        if (page > 0) {
            inventory.setItem(SLOT_PREV, ItemStackBuilder.of(Material.ARROW)
                    .name(ChatColor.WHITE + "← 前のページ")
                    .lore(ChatColor.DARK_GRAY + "ページ " + page + " へ")
                    .build());
        }

        if (page < maxPage) {
            inventory.setItem(SLOT_NEXT, ItemStackBuilder.of(Material.ARROW)
                    .name(ChatColor.WHITE + "次のページ →")
                    .lore(ChatColor.DARK_GRAY + "ページ " + (page + 2) + " へ")
                    .build());
        }
    }

    private ItemStack toItem(VisitorLogEntry e) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(e.getPlayer());
        String name   = op.getName() != null ? op.getName() : e.getPlayer().toString();
        String at     = DATE_FORMAT.format(new Date(e.getAt()));
        String type   = typeLabel(e.getType());
        String detail = e.getDetail() != null && !e.getDetail().isEmpty() ? e.getDetail() : "-";

        return ItemStackBuilder.of(Material.PAPER)
                .name(ChatColor.YELLOW + "" + ChatColor.BOLD + name)
                .lore(
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.GRAY + "日時: " + ChatColor.WHITE + at,
                    ChatColor.GRAY + "操作: " + ChatColor.WHITE + type,
                    ChatColor.GRAY + "詳細: " + ChatColor.WHITE + detail,
                    ChatColor.DARK_GRAY + "─────────────────"
                ).build();
    }

    private String typeLabel(VisitorLogType type) {
        if (type == null) return "不明";
        switch (type) {
            case ENTER:    return "入場";
            case EXIT:     return "退場";
            case BREAK:    return "破壊";
            case PLACE:    return "設置";
            case INTERACT: return "操作";
            default:       return "不明";
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == SLOT_BACK)  { back(); return; }
        if (slot == SLOT_PREV)  { page--; init(); return; }
        if (slot == SLOT_NEXT)  { page++; init(); return; }
    }
}
