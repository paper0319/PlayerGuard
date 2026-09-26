package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.region.ProtectionAccess;
import net.nekozouneko.playerguard.region.RegionBlacklist;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 保護ごとのプレイヤーブラックリスト管理GUI。
 * ブラックリスト入りのプレイヤーはメンバーであっても侵入・操作を拒否される。
 */
public class BlacklistGUI extends AbstractGUI {

    private static final int SIZE = 54;
    private static final int PAGE_SIZE = 45;

    private static final int SLOT_BACK = 53;
    private static final int SLOT_SEARCH = 46;
    private static final int SLOT_ADD = 49;
    private static final Material GLASS = Material.BLACK_STAINED_GLASS_PANE;

    private final ProtectedRegion region;
    private final List<UUID> shownSlots = new ArrayList<>();
    private final List<UUID> allEntries = new ArrayList<>();
    private int page;
    private String query = "";

    public BlacklistGUI(Player player, AbstractGUI parent, ProtectedRegion region) {
        super(player, parent);
        this.region = region;
    }

    @Override
    public void init() {
        if (inventory == null)
            inventory = Bukkit.createInventory(this, SIZE,
                ChatColor.AQUA + "■ " + ChatColor.WHITE + "ブラックリスト管理");
        inventory.clear();
        shownSlots.clear();

        ItemStack glass = ItemStackBuilder.of(GLASS).name(" ").build();
        for (int i = PAGE_SIZE; i < SIZE; i++) inventory.setItem(i, glass);

        allEntries.clear();
        for (UUID uuid : RegionBlacklist.list(region)) {
            if (matchesQuery(uuid)) allEntries.add(uuid);
        }
        allEntries.sort(java.util.Comparator.comparing(this::entryName, String.CASE_INSENSITIVE_ORDER));
        int maxPage = allEntries.isEmpty() ? 0 : (allEntries.size() - 1) / PAGE_SIZE;
        page = Math.max(0, Math.min(page, maxPage));
        for (int index = page * PAGE_SIZE; index < Math.min(allEntries.size(), (page + 1) * PAGE_SIZE); index++) {
            UUID uuid = allEntries.get(index);
            int slot = index - page * PAGE_SIZE;
            inventory.setItem(slot, entryHead(uuid));
            shownSlots.add(uuid);
        }

        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW)
                .name(ChatColor.WHITE + "← 戻る")
                .lore(ChatColor.DARK_GRAY + "前の画面へ")
                .build());
        inventory.setItem(SLOT_SEARCH, ItemStackBuilder.of(Material.COMPASS)
                .name(ChatColor.AQUA + "ブラックリスト検索")
                .lore(ChatColor.GRAY + "現在: " + ChatColor.WHITE + (query.isBlank() ? "なし" : query),
                        ChatColor.YELLOW + "クリックして検索")
                .build());
        if (page > 0) inventory.setItem(48, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "前ページ").build());
        inventory.setItem(50, ItemStackBuilder.of(Material.PAPER)
                .name(ChatColor.WHITE + "ページ " + (page + 1) + " / " + (maxPage + 1)).build());
        if (page < maxPage) inventory.setItem(52, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "次ページ").build());

        inventory.setItem(SLOT_ADD, ItemStackBuilder.of(Material.LIME_DYE)
                .name(ChatColor.GREEN + "" + ChatColor.BOLD + "ブラックリストに追加")
                .lore(
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.GRAY + "オンラインプレイヤーを侵入・操作禁止に追加",
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.DARK_GRAY + "クリックで選択"
                ).build());
    }

    private String entryName(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? uuid.toString() : name;
    }

    private boolean matchesQuery(UUID uuid) {
        if (query.isBlank()) return true;
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        String lower = query.toLowerCase(Locale.ROOT);
        return uuid.toString().contains(lower) || (name != null && name.toLowerCase(Locale.ROOT).contains(lower));
    }

    private ItemStack entryHead(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        ItemStack head = ItemStackBuilder.of(Material.PLAYER_HEAD)
                .name(ChatColor.RED + "" + ChatColor.BOLD + entryName(uuid))
                .lore(
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.GRAY + "状態: " + ChatColor.RED + "侵入・操作禁止",
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.DARK_GRAY + "クリックでブラックリストから削除"
                ).build();
        if (head.getItemMeta() instanceof SkullMeta) {
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            sm.setOwningPlayer(op);
            head.setItemMeta(sm);
        }
        return head;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;

        if (slot == SLOT_BACK) { back(); return; }
        if (slot == SLOT_SEARCH) {
            ChatInputManager.waitFor(getPlayer(), (player, input) -> {
                query = "cancel".equalsIgnoreCase(input.trim()) ? "" : input.trim();
                page = 0;
                open();
            });
            return;
        }
        if (slot == 48) { page--; init(); return; }
        if (slot == 52) { page++; init(); return; }
        if (slot == SLOT_ADD) {
            if (ProtectionAccess.canManageMembers(getPlayer(), region)) openAddSelector();
            else deny();
            return;
        }
        if (slot < shownSlots.size()) {
            if (!ProtectionAccess.canManageMembers(getPlayer(), region)) { deny(); return; }
            removeEntry(shownSlots.get(slot));
        }
    }

    private void openAddSelector() {
        if (!ProtectionAccess.canManageMembers(getPlayer(), region)) return;
        List<OfflinePlayer> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            UUID uuid = online.getUniqueId();
            if (uuid.equals(getPlayer().getUniqueId())) continue;
            if (region.getOwners().contains(uuid)) continue;
            if (RegionBlacklist.contains(region, uuid)) continue;
            candidates.add(online);
        }
        new PlayerSelectGUI(getPlayer(), this, "■ 追加するプレイヤー", candidates, uuid -> {
            RegionBlacklist.AddResult result = RegionBlacklist.add(region, uuid);
            String name = entryName(uuid);
            if (result == RegionBlacklist.AddResult.ADDED) {
                log("ブラックリスト追加", name, uuid);
                getPlayer().sendMessage(ChatColor.GREEN + name + " をブラックリストに追加しました。");
                getPlayer().playSound(getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 10, 2);
            } else if (result == RegionBlacklist.AddResult.ALREADY_BLACKLISTED) {
                getPlayer().sendMessage(ChatColor.YELLOW + name + " はすでにブラックリストに入っています。");
            } else if (result == RegionBlacklist.AddResult.IS_OWNER) {
                getPlayer().sendMessage(ChatColor.RED + "オーナーはブラックリストに追加できません。");
            } else {
                getPlayer().sendMessage(ChatColor.RED + "ブラックリストに追加できませんでした。");
            }
            open();
        }).open();
    }

    private void removeEntry(UUID uuid) {
        if (RegionBlacklist.remove(region, uuid) == RegionBlacklist.RemoveResult.REMOVED) {
            String name = entryName(uuid);
            log("ブラックリスト削除", name, uuid);
            getPlayer().sendMessage(ChatColor.GREEN + name + " をブラックリストから削除しました。");
            getPlayer().playSound(getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 10, 1);
        }
        open();
    }

    private void log(String action, String name, UUID uuid) {
        PlayerGuard plugin = PlayerGuard.getInstance();
        if (plugin == null || plugin.getProtectionLogService() == null) return;
        plugin.getProtectionLogService().log(region, action, getPlayer().getUniqueId(),
                name + " (" + uuid + ")", null, null,
                "actor=" + getPlayer().getName() + ";actorUuid=" + getPlayer().getUniqueId());
    }

    private void deny() {
        getPlayer().sendMessage(ChatColor.RED + "ブラックリストを管理する権限がありません。");
    }
}
