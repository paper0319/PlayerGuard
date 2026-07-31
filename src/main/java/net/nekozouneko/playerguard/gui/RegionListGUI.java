package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.region.RegionRoles;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.math.BigDecimal;

public class RegionListGUI extends AbstractGUI {

    private static final int SIZE      = 54;
    private static final int PAGE_SIZE = 45;

    // Nav bar: [CLOSE(45)][G][PREV(47)][G][INFO(49)][G][NEXT(51)][G][G]
    private static final int SLOT_CLOSE = 45;
    private static final int SLOT_PREV  = 47;
    private static final int SLOT_INFO  = 49;
    private static final int SLOT_NEXT  = 51;

    private static final Material GLASS = Material.BLACK_STAINED_GLASS_PANE;

    private final List<Map.Entry<ProtectedRegion, World>> regions = new ArrayList<>();
    private int page;

    public RegionListGUI(Player player) {
        super(player, null);
    }

    @Override
    public void init() {
        if (inventory == null)
            inventory = Bukkit.createInventory(this, SIZE,
                ChatColor.AQUA + "■ " + ChatColor.WHITE + "あなたの領域一覧");
        inventory.clear();
        regions.clear();

        // Fill bottom bar with glass
        ItemStack glass = ItemStackBuilder.of(GLASS).name(" ").build();
        for (int i = PAGE_SIZE; i < SIZE; i++) inventory.setItem(i, glass);

        regions.addAll(PGUtil.getPlayerRegions(getPlayer()).entrySet());
        Collections.sort(regions, Comparator
                .comparing((Map.Entry<ProtectedRegion, World> e) -> e.getValue().getName())
                .thenComparing(e -> e.getKey().getId()));

        int maxPage = regions.isEmpty() ? 0 : (regions.size() - 1) / PAGE_SIZE;
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= regions.size()) break;
            Map.Entry<ProtectedRegion, World> entry = regions.get(idx);
            inventory.setItem(i, toItem(entry.getKey(), entry.getValue()));
        }

        // Close at left-most (no parent, this is root screen)
        inventory.setItem(SLOT_CLOSE, ItemStackBuilder.of(Material.BARRIER)
                .name(ChatColor.RED + "" + ChatColor.BOLD + "閉じる")
                .lore(ChatColor.DARK_GRAY + "インベントリを閉じる")
                .build());

        if (page > 0) {
            inventory.setItem(SLOT_PREV, ItemStackBuilder.of(Material.ARROW)
                    .name(ChatColor.WHITE + "← 前のページ")
                    .lore(ChatColor.DARK_GRAY + "ページ " + page + " へ")
                    .build());
        }

        PlayerGuard pg = PlayerGuard.getInstance();
        inventory.setItem(SLOT_INFO, ItemStackBuilder.of(Material.PAPER)
                .name(ChatColor.YELLOW + "" + ChatColor.BOLD + "あなたの情報")
                .lore(
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.GRAY + "保護制限: " + ChatColor.WHITE
                        + pg.getProtectionUsed(getPlayer()) + " / " + pg.getProtectLimit(getPlayer()),
                    ChatColor.GRAY + "領域数: " + ChatColor.WHITE + regions.size(),
                    ChatColor.GRAY + "ページ: " + ChatColor.WHITE + (page + 1) + " / " + (maxPage + 1),
                    ChatColor.DARK_GRAY + "─────────────────"
                ).build());

        if (page < maxPage) {
            inventory.setItem(SLOT_NEXT, ItemStackBuilder.of(Material.ARROW)
                    .name(ChatColor.WHITE + "次のページ →")
                    .lore(ChatColor.DARK_GRAY + "ページ " + (page + 2) + " へ")
                    .build());
        }
    }

    private ItemStack toItem(ProtectedRegion region, World world) {
        UUID me = getPlayer().getUniqueId();
        RegionRoles.Role role = RegionRoles.roleOf(region, me);
        String roleLabel;
        switch (role) {
            case PRIMARY_OWNER: roleLabel = ChatColor.GOLD + "オーナー"; break;
            case SUB_OWNER:     roleLabel = ChatColor.YELLOW + "サブオーナー"; break;
            default:            roleLabel = ChatColor.WHITE + "建築士"; break;
        }

        return ItemStackBuilder.of(Material.MAP)
                .name(ChatColor.YELLOW + "" + ChatColor.BOLD + region.getId())
                .lore(
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.GRAY + "ワールド: " + ChatColor.WHITE + world.getName(),
                    ChatColor.GRAY + "役割: " + roleLabel,
                    ChatColor.GRAY + "範囲: " + ChatColor.WHITE + String.format(
                        "(%d,%d,%d)→(%d,%d,%d)",
                        region.getMinimumPoint().x(), region.getMinimumPoint().y(), region.getMinimumPoint().z(),
                        region.getMaximumPoint().x(), region.getMaximumPoint().y(), region.getMaximumPoint().z()),
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.DARK_GRAY + "クリックで管理"
                ).build();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == SLOT_CLOSE) { getPlayer().closeInventory(); return; }
        if (slot == SLOT_PREV)  { page--; init(); return; }
        if (slot == SLOT_NEXT)  { page++; init(); return; }
        if (slot < 0 || slot >= PAGE_SIZE) return;

        int idx = page * PAGE_SIZE + slot;
        if (idx < 0 || idx >= regions.size()) return;
        ProtectedRegion region = regions.get(idx).getKey();
        new RegionHubGUI(getPlayer(), region, this).open();
    }
}
