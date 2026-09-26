package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.paid.ProtectionLogEntry;
import net.nekozouneko.playerguard.visitlog.VisitorLogEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ProtectionLogCategoryGUI extends AbstractGUI {
    private static final int SIZE = 54, LOG_START = 18, LOG_SIZE = 27, SLOT_PREV = 46, SLOT_SORT = 48, SLOT_PAGE = 49, SLOT_NEXT = 52, SLOT_BACK = 53;
    private static final int[] CATEGORY_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    enum Category {
        ALL("\u3059\u3079\u3066", Material.BOOK), VISITOR("\u8a2a\u554f", Material.OAK_DOOR), MEMBER("\u30e1\u30f3\u30d0\u30fc", Material.PLAYER_HEAD), SUBOWNER("\u30b5\u30d6\u30aa\u30fc\u30ca\u30fc", Material.GOLDEN_HELMET), FLAG("\u30d5\u30e9\u30b0", Material.REDSTONE_TORCH), PERMISSION("\u6a29\u9650", Material.COMPARATOR), RENAME("\u4fdd\u8b77\u540d", Material.NAME_TAG), PAYMENT("\u652f\u6255\u3044", Material.EMERALD), DELETE("\u524a\u9664", Material.TNT);
        final String label; final Material icon;
        Category(String label, Material icon) { this.label = label; this.icon = icon; }
        boolean matches(String type) { if (this == ALL) return true; if (type == null) return false; return switch (this) { case VISITOR -> type.contains("\u8a2a\u554f") || type.contains("\u5165\u5834") || type.contains("\u9000\u5834"); case MEMBER -> type.contains("\u30e1\u30f3\u30d0\u30fc"); case SUBOWNER -> type.contains("\u30b5\u30d6\u30aa\u30fc\u30ca\u30fc"); case FLAG -> type.contains("\u30d5\u30e9\u30b0"); case PERMISSION -> type.contains("\u6a29\u9650"); case RENAME -> type.contains("\u4fdd\u8b77\u540d"); case PAYMENT -> type.contains("\u652f\u6255") || type.contains("\u8fd4\u91d1"); case DELETE -> type.contains("\u524a\u9664"); default -> true; }; }
    }
    private final ProtectedRegion region; private final World world; private final List<ProtectionLogEntry> entries = new ArrayList<>();
    private Category selected = Category.ALL; private int page; private boolean newestFirst = true;
    public ProtectionLogCategoryGUI(Player player, AbstractGUI parent, ProtectedRegion region, World world) { super(player, parent); this.region = region; this.world = world; }
    @Override public void init() {
        if (inventory == null) inventory = Bukkit.createInventory(this, SIZE, ChatColor.YELLOW + "\u4fdd\u8b77\u30ed\u30b0");
        inventory.clear(); ItemStack dark = ItemStackBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build(), gray = ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, i >= 9 && i < 18 ? gray : dark);
        for (int i = 0; i < Category.values().length; i++) { Category c = Category.values()[i]; inventory.setItem(CATEGORY_SLOTS[i], ItemStackBuilder.of(c.icon).name((c == selected ? ChatColor.GREEN : ChatColor.YELLOW) + "\u00a7l" + c.label + "\u30ed\u30b0").lore(c == selected ? ChatColor.GRAY + "\u73fe\u5728\u9078\u629e\u4e2d" : ChatColor.DARK_GRAY + "\u30af\u30ea\u30c3\u30af\u3067\u8868\u793a").build()); }
        loadEntries(); int maxPage = entries.isEmpty() ? 0 : (entries.size() - 1) / LOG_SIZE; page = Math.max(0, Math.min(page, maxPage));
        for (int i = 0; i < LOG_SIZE; i++) { int index = page * LOG_SIZE + i; if (index >= entries.size()) break; inventory.setItem(LOG_START + i, item(entries.get(index))); }
        if (entries.isEmpty()) inventory.setItem(31, ItemStackBuilder.of(Material.PAPER).name(ChatColor.GRAY + "\u3053\u306e\u30ab\u30c6\u30b4\u30ea\u306e\u30ed\u30b0\u306f\u307e\u3060\u3042\u308a\u307e\u305b\u3093").build());
        if (page > 0) inventory.setItem(SLOT_PREV, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "\u524d\u30da\u30fc\u30b8").build());
        inventory.setItem(SLOT_SORT, ItemStackBuilder.of(Material.HOPPER).name(ChatColor.YELLOW + (newestFirst ? "\u65b0\u3057\u3044\u9806" : "\u53e4\u3044\u9806")).build());
        inventory.setItem(SLOT_PAGE, ItemStackBuilder.of(Material.PAPER).name(ChatColor.WHITE + "\u30da\u30fc\u30b8 " + (page + 1) + " / " + (maxPage + 1)).build());
        if (page < maxPage) inventory.setItem(SLOT_NEXT, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "\u6b21\u30da\u30fc\u30b8").build());
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "\u623b\u308b").build());
    }
    private void loadEntries() { entries.clear(); entries.addAll(PlayerGuard.getInstance().getProtectionLogService().getEntries(region, false)); if (PlayerGuard.getInstance().getVisitorLogService() != null) for (VisitorLogEntry v : PlayerGuard.getInstance().getVisitorLogService().getEntries(world.getName(), region.getId())) entries.add(new ProtectionLogEntry(v.getAt(), "\u8a2a\u554f\u30ed\u30b0", v.getPlayer(), null, null, null, region.getId(), v.getDetail())); entries.removeIf(e -> !selected.matches(e.type())); entries.sort(Comparator.comparingLong(ProtectionLogEntry::at)); if (newestFirst) Collections.reverse(entries); }
    private ItemStack item(ProtectionLogEntry e) { OfflinePlayer player = e.actor() == null ? null : Bukkit.getOfflinePlayer(e.actor()); String actor = player == null ? "\u4e0d\u660e" : player.getName() == null ? e.actor().toString() : player.getName(); return ItemStackBuilder.of(icon(e.type())).name(ChatColor.YELLOW + "\u00a7l" + blank(e.type())).lore(ChatColor.DARK_GRAY + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500", ChatColor.GRAY + "\u5b9f\u884c\u8005: " + ChatColor.WHITE + actor, ChatColor.GRAY + "\u5bfe\u8c61: " + ChatColor.WHITE + blank(e.target()), ChatColor.GRAY + "\u4fdd\u8b77\u540d: " + ChatColor.WHITE + PlayerGuard.getInstance().getProtectionNameRepository().displayName(region), ChatColor.GRAY + "\u5909\u66f4\u524d: " + ChatColor.WHITE + blank(e.before()), ChatColor.GRAY + "\u5909\u66f4\u5f8c: " + ChatColor.WHITE + blank(e.after()), ChatColor.GRAY + "\u65e5\u6642: " + ChatColor.WHITE + ProtectionGuiText.date(Instant.ofEpochMilli(e.at())), ChatColor.DARK_GRAY + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500").build(); }
    private static Material icon(String type) { if (type == null) return Material.PAPER; if (type.contains("\u524a\u9664")) return Material.TNT; if (type.contains("\u30e1\u30f3\u30d0\u30fc")) return Material.PLAYER_HEAD; if (type.contains("\u30b5\u30d6\u30aa\u30fc\u30ca\u30fc")) return Material.GOLDEN_HELMET; if (type.contains("\u30d5\u30e9\u30b0")) return Material.REDSTONE_TORCH; if (type.contains("\u6a29\u9650")) return Material.COMPARATOR; return Material.PAPER; }
    private static String blank(String value) { return value == null || value.isBlank() ? "-" : value; }
    @EventHandler public void onClick(InventoryClickEvent event) { if (event.getView().getTopInventory().getHolder() != this) return; event.setCancelled(true); int slot = event.getRawSlot(); for (int i = 0; i < CATEGORY_SLOTS.length; i++) if (slot == CATEGORY_SLOTS[i]) { selected = Category.values()[i]; page = 0; init(); return; } if (slot == SLOT_BACK) back(); else if (slot == SLOT_PREV) { page--; init(); } else if (slot == SLOT_SORT) { newestFirst = !newestFirst; page = 0; init(); } else if (slot == SLOT_NEXT) { page++; init(); } }
}