package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.paid.CreationConfirmationMode;
import net.nekozouneko.playerguard.paid.ProtectionCustomizationRepository;
import net.nekozouneko.playerguard.paid.ProtectionPaymentRecord;
import net.nekozouneko.playerguard.region.RegionRoles;
import net.nekozouneko.playerguard.region.ProtectionAccess;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class RegionListGUI extends AbstractGUI {
    static final int SIZE = 54;
    private static final int PAGE_SIZE = 45;
    static final int SLOT_BACK = 45;
    private static final int SLOT_SEARCH = 46;
    private static final int SLOT_PREV = 47;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_CONFIRM_MODE = 50;
    private static final int SLOT_NEXT = 51;
    private static final int SLOT_STATILTICL = 52;
    static final int SLOT_GUIDE = 53;

    private final List<Map.Entry<ProtectedRegion, World>> regions = new ArrayList<>();
    private int page;
    private SortMode sortMode = SortMode.NEWEST;
    private String query = "";
    private final UUID targetId;
    private final String targetName;

    public RegionListGUI(Player player) { this(player, player.getUniqueId(), player.getName()); }

    public RegionListGUI(Player player, UUID targetId, String targetName) { super(player, null); this.targetId = targetId; this.targetName = targetName; }

    @Override public void init() {
        if (inventory == null) inventory = Bukkit.createInventory(this, SIZE, ChatColor.BLACK + (targetId.equals(getPlayer().getUniqueId()) ? "保護一覧" : "保護一覧 - " + targetName));
        inventory.clear();
        ItemStack glass = ItemStackBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = PAGE_SIZE; i < SIZE; i++) inventory.setItem(i, glass);
        loadRegions();
        int maxPage = regions.isEmpty() ? 0 : (regions.size() - 1) / PAGE_SIZE;
        page = Math.max(0, Math.min(page, maxPage));
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = page * PAGE_SIZE + i;
            if (idx >= regions.size()) break;
            Map.Entry<ProtectedRegion, World> entry = regions.get(idx);
            inventory.setItem(i, toItem(entry.getKey(), entry.getValue()));
        }
        if (regions.isEmpty()) inventory.setItem(22, ItemStackBuilder.of(Material.BARRIER).name(ChatColor.RED + "条件に一致する保護がありません").lore(ChatColor.GRAY + "検索条件を変更してください").build());
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "戻る").build());
        inventory.setItem(SLOT_SEARCH, ItemStackBuilder.of(Material.COMPASS).name(ChatColor.AQUA + "検索").lore(ChatColor.GRAY + "保護名 / 保護ID", ChatColor.GRAY + "現在: " + ChatColor.WHITE + (query.isBlank() ? "なし" : query), ChatColor.YELLOW + "クリックして検索").build());
        if (page > 0) inventory.setItem(SLOT_PREV, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "前ページ").build());
        inventory.setItem(SLOT_INFO, ItemStackBuilder.of(Material.PAPER).name(ChatColor.YELLOW + "表示情報").lore(ChatColor.GRAY + "件数: " + ChatColor.WHITE + regions.size(), ChatColor.GRAY + "ページ: " + ChatColor.WHITE + (page + 1) + " / " + (maxPage + 1), ChatColor.GRAY + "並び順: " + ChatColor.WHITE + sortMode.label, ChatColor.YELLOW + "クリックで並び替え").build());
        inventory.setItem(SLOT_CONFIRM_MODE, confirmModeItem());
        if (page < maxPage) inventory.setItem(SLOT_NEXT, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "次ページ").build());
        inventory.setItem(SLOT_STATILTICL, ItemStackBuilder.of(Material.RECOVERY_COMPASS).name(ChatColor.AQUA + "§l保護統計").lore(ChatColor.DARK_GRAY + "────────────", ChatColor.GRAY + "現在の保護状況や", ChatColor.GRAY + "無料で使える残り体積、", ChatColor.GRAY + "料金情報を確認できます。", ChatColor.DARK_GRAY + "────────────", ChatColor.YELLOW + "§l▶ クリックして統計を開く").build());
        inventory.setItem(SLOT_GUIDE, ItemStackBuilder.of(Material.KNOWLEDGE_BOOK).name(ChatColor.YELLOW + "土地保護ガイド").lore(ChatColor.GRAY + "土地保護の使い方や料金、", ChatColor.GRAY + "返金制度について確認できます", ChatColor.YELLOW + "クリックでガイドを開く").build());
    }

    private void loadRegions() {
        regions.clear();
        String lower = query.toLowerCase(Locale.ROOT);
        for (Map.Entry<ProtectedRegion, World> entry : PGUtil.getPlayerRegions(targetId).entrySet()) {
            String name = PlayerGuard.getInstance().getProtectionNameRepository().displayName(entry.getKey());
            if (!lower.isBlank() && !name.toLowerCase(Locale.ROOT).contains(lower) && !entry.getKey().getId().toLowerCase(Locale.ROOT).contains(lower)) continue;
            regions.add(entry);
        }
        regions.sort(comparator());
    }

    private Comparator<Map.Entry<ProtectedRegion, World>> comparator() {
        Comparator<Map.Entry<ProtectedRegion, World>> byName = Comparator.comparing((Map.Entry<ProtectedRegion, World> e) -> PlayerGuard.getInstance().getProtectionNameRepository().displayName(e.getKey()), String.CASE_INSENSITIVE_ORDER).thenComparing(e -> e.getKey().getId());
        Comparator<Map.Entry<ProtectedRegion, World>> byCreated = Comparator.comparing(this::createdAt);
        switch (sortMode) { case OLDELT: return byCreated.thenComparing(byName); case NAME: return byName; case NEWEST: default: return byCreated.reversed().thenComparing(byName); }
    }

    private Instant createdAt(Map.Entry<ProtectedRegion, World> entry) {
        ProtectionPaymentRecord payment = PlayerGuard.getInstance().getProtectionPaymentRepository().find(entry.getKey());
        return payment == null ? Instant.EPOCH : payment.createdAt();
    }

    private ItemStack toItem(ProtectedRegion region, World world) {
        ProtectionPaymentRecord payment = PlayerGuard.getInstance().getProtectionPaymentRepository().find(region);
        BigDecimal paid = payment == null ? BigDecimal.ZERO : payment.amountPaid();
        BigDecimal refund = payment == null ? BigDecimal.ZERO : ProtectionGuiText.refund(paid, payment.createdAt(), Instant.now());
        ProtectionCustomizationRepository.Category category = PlayerGuard.getInstance().getProtectionCustomizationRepository().category(region);
        String name = PlayerGuard.getInstance().getProtectionNameRepository().displayName(region);
        boolean adminView = !targetId.equals(getPlayer().getUniqueId()) && ProtectionAccess.isAdmin(getPlayer());
        RegionRoles.Role role = RegionRoles.roleOf(region, targetId);
        ChatColor nameColor = adminView ? ChatColor.RED : role == RegionRoles.Role.SUB_OWNER ? ChatColor.GOLD : role == RegionRoles.Role.BUILDER ? ChatColor.AQUA : ChatColor.GREEN;
        UUID ownerId = RegionRoles.getPrimaryOwner(region);
        String ownerName = ownerId == null ? "\u4e0d\u660e" : java.util.Optional.ofNullable(Bukkit.getOfflinePlayer(ownerId).getName()).orElse(ownerId.toString());
        String roleLine = adminView ? ChatColor.GRAY + "\u95b2\u89a7\u30e2\u30fc\u30c9: " + ChatColor.RED + ChatColor.BOLD + "\u7ba1\u7406\u8005" : ChatColor.GRAY + "\u3042\u306a\u305f\u306e\u7acb\u5834: " + (role == RegionRoles.Role.PRIMARY_OWNER ? ChatColor.GREEN + "\u00a7l\u30aa\u30fc\u30ca\u30fc" : role == RegionRoles.Role.SUB_OWNER ? ChatColor.GOLD + "\u00a7l\u30b5\u30d6\u30aa\u30fc\u30ca\u30fc" : ChatColor.AQUA + "\u00a7l\u30e1\u30f3\u30d0\u30fc");
        String ownerLine = adminView ? ChatColor.GRAY + "\u571f\u5730\u30aa\u30fc\u30ca\u30fc: " + ChatColor.WHITE + ownerName : role == RegionRoles.Role.SUB_OWNER ? ChatColor.GRAY + "\u30aa\u30fc\u30ca\u30fc: " + ChatColor.WHITE + ownerName : "";        return ItemStackBuilder.of(category.icon).name(nameColor + "" + ChatColor.BOLD + name)
                .lore(ChatColor.DARK_GRAY + "────────────",
                        ChatColor.GRAY + "保護ID: " + ChatColor.WHITE + region.getId(),
                        ChatColor.GRAY + "カテゴリ: " + ChatColor.YELLOW + category.label,
                        ChatColor.GRAY + "ワールド: " + ChatColor.WHITE + world.getName(),
                        ChatColor.GRAY + "体積: " + ChatColor.YELLOW + ProtectionGuiText.blocks(region.volume()),
                        ChatColor.GRAY + "支払額: " + ChatColor.YELLOW + ProtectionGuiText.money(paid),
                        ChatColor.GRAY + "返金予定額: " + ChatColor.GREEN + ProtectionGuiText.money(refund),
                        roleLine,
                        ownerLine,
                        ChatColor.DARK_GRAY + "────────────",
                        ChatColor.YELLOW + "▶ クリックして保護管理を開く")
                .build();
    }
    private ItemStack confirmModeItem() {
        CreationConfirmationMode mode = PlayerGuard.getInstance().getCreationConfirmationSettings().get(getPlayer().getUniqueId());
        if (mode == CreationConfirmationMode.CHAT) {
            return ItemStackBuilder.of(Material.COMPARATOR).name(ChatColor.AQUA + "" + ChatColor.BOLD + "保護作成の確認方法")
                    .lore(ChatColor.DARK_GRAY + "────────────",
                            ChatColor.GRAY + "現在: " + ChatColor.YELLOW + "チャット確認",
                            ChatColor.GRAY + "範囲選択後に",
                            ChatColor.GRAY + "チャットへ確認内容を表示します",
                            ChatColor.DARK_GRAY + "────────────",
                            ChatColor.YELLOW + "クリックでGUI確認へ変更")
                    .build();
        }
        return ItemStackBuilder.of(Material.COMPARATOR).name(ChatColor.AQUA + "" + ChatColor.BOLD + "保護作成の確認方法")
                .lore(ChatColor.DARK_GRAY + "────────────",
                        ChatColor.GRAY + "現在: " + ChatColor.GREEN + "GUI確認",
                        ChatColor.GRAY + "範囲選択後に",
                        ChatColor.GRAY + "確認GUIを開きます",
                        ChatColor.DARK_GRAY + "────────────",
                        ChatColor.YELLOW + "クリックでチャット確認へ変更")
                .build();
    }

    private void toggleConfirmationMode() {
        CreationConfirmationMode mode = PlayerGuard.getInstance().getCreationConfirmationSettings().toggle(getPlayer().getUniqueId());
        getPlayer().sendMessage(ChatColor.GREEN + "保護作成の確認方法を" + mode.label + "に変更しました。");
        init();
    }

    static String coords(ProtectedRegion region) { return String.format("(%d,%d,%d) -> (%d,%d,%d)", region.getMinimumPoint().x(), region.getMinimumPoint().y(), region.getMinimumPoint().z(), region.getMaximumPoint().x(), region.getMaximumPoint().y(), region.getMaximumPoint().z()); }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        int slot = e.getRawSlot();
        if (slot == SLOT_BACK) { getPlayer().closeInventory(); return; }
        if (slot == SLOT_STATILTICL) { new ProtectionStatisticsGUI(getPlayer(), this).open(); return; }
        if (slot == SLOT_SEARCH) { startLearch(); return; }
        if (slot == SLOT_PREV) { page--; init(); return; }
        if (slot == SLOT_NEXT) { page++; init(); return; }
        if (slot == SLOT_INFO) { sortMode = sortMode.next(); page = 0; init(); return; }
        if (slot == SLOT_CONFIRM_MODE) { toggleConfirmationMode(); return; }
        if (slot == SLOT_GUIDE) { navigateTo(new ProtectionGuideGUI(getPlayer(), this)); return; }
        if (slot < 0 || slot >= PAGE_SIZE) return;
        int idx = page * PAGE_SIZE + slot;
        if (idx < 0 || idx >= regions.size()) return;
        Map.Entry<ProtectedRegion, World> entry = regions.get(idx);
        new RegionHubGUI(getPlayer(), entry.getKey(), entry.getValue(), this).open();
    }

    private void startLearch() { ChatInputManager.waitFor(getPlayer(), (player, input) -> { if (!"cancel".equalsIgnoreCase(input.trim())) { query = input.trim(); page = 0; } open(); }); }

    private enum SortMode { NEWEST("新しい順"), OLDELT("古い順"), NAME("名前順"); private final String label; SortMode(String label) { this.label = label; } private SortMode next() { return values()[(ordinal() + 1) % values().length]; } }
}