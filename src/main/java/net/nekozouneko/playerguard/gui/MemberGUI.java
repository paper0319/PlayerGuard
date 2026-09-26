package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.command.sub.playerguard.TransferCommand;
import net.nekozouneko.playerguard.region.RegionMembers;
import net.nekozouneko.playerguard.notification.ProtectionRoleNotifier;
import net.nekozouneko.playerguard.region.ProtectionAccess;
import net.nekozouneko.playerguard.region.RegionRentals;
import net.nekozouneko.playerguard.region.RegionRoles;
import net.nekozouneko.playerguard.region.RegionRoles.Role;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MemberGUI extends AbstractGUI {

    private static final int SIZE      = 54;
    private static final int PAGE_SLOTL = 45;

    // Bottom bar: [BACK(45)][G][ADD(47)][G][RENT(49)][G][TRANLFER(51)][G][G]
    private static final int SLOT_BACK     = 53;
    private static final int SLOT_SEARCH   = 46;
    private static final int SLOT_ADD      = 47;
    private static final int SLOT_RENT     = 49;
    private static final int SLOT_TRANLFER = 51;
    private static final Material GLASS = Material.BLACK_STAINED_GLASS_PANE;

    private final ProtectedRegion region;
    private final List<UUID> memberSlots = new ArrayList<>();
    private final List<UUID> allMembers = new ArrayList<>();
    private int page;
    private String query = "";

    public MemberGUI(Player player, AbstractGUI parent, ProtectedRegion region) {
        super(player, parent);
        this.region = region;
    }

    @Override
    public void init() {
        if (inventory == null)
            inventory = Bukkit.createInventory(this, SIZE,
                ChatColor.AQUA + "■ " + ChatColor.WHITE + "メンバー管理");
        inventory.clear();
        memberSlots.clear();

        // Fill action bar with glass
        ItemStack glass = ItemStackBuilder.of(GLASS).name(" ").build();
        for (int i = PAGE_SLOTL; i < SIZE; i++) inventory.setItem(i, glass);

        long now = System.currentTimeMillis();
        allMembers.clear();
        for (UUID uuid : region.getOwners().getUniqueIds()) {
            if (RegionRoles.roleOf(region, uuid) == Role.SUB_OWNER && matchesQuery(uuid)) allMembers.add(uuid);
        }
        for (UUID uuid : region.getMembers().getUniqueIds()) {
            if (matchesQuery(uuid)) allMembers.add(uuid);
        }
        allMembers.sort(java.util.Comparator.comparing(this::memberName, String.CASE_INSENSITIVE_ORDER));
        int maxPage = allMembers.isEmpty() ? 0 : (allMembers.size() - 1) / PAGE_SLOTL;
        page = Math.max(0, Math.min(page, maxPage));
        for (int index = page * PAGE_SLOTL; index < Math.min(allMembers.size(), (page + 1) * PAGE_SLOTL); index++) {
            UUID uuid = allMembers.get(index);
            int slot = index - page * PAGE_SLOTL;
            Long expiry = RegionRentals.getExpiry(region, uuid);
            String rentalLore = expiry != null ? "貸出中: あと " + RegionRentals.formatRemaining(expiry - now) : null;
            Role role = RegionRoles.roleOf(region, uuid);
            inventory.setItem(slot, memberHead(uuid, role == Role.SUB_OWNER ? ChatColor.GOLD : ChatColor.WHITE, role == Role.SUB_OWNER ? "サブオーナー" : expiry != null ? "メンバー（貸出中）" : "メンバー", rentalLore));
            memberSlots.add(uuid);
        }        // Back (left-most of bottom bar)
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW)
                .name(ChatColor.WHITE + "← 戻る")
                .lore(ChatColor.DARK_GRAY + "前の画面へ")
                .build());

        inventory.setItem(SLOT_SEARCH, ItemStackBuilder.of(Material.COMPASS).name(ChatColor.AQUA + "メンバー検索").lore(ChatColor.GRAY + "現在: " + ChatColor.WHITE + (query.isBlank() ? "なし" : query), ChatColor.YELLOW + "クリックして検索").build());
        if (page > 0) inventory.setItem(48, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "前ページ").build());
        inventory.setItem(50, ItemStackBuilder.of(Material.PAPER).name(ChatColor.WHITE + "ページ " + (page + 1) + " / " + (maxPage + 1)).build());
        if (page < maxPage) inventory.setItem(52, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "次ページ").build());

        inventory.setItem(SLOT_ADD, ItemStackBuilder.of(Material.LIME_DYE)
                .name(ChatColor.GREEN + "" + ChatColor.BOLD + "メンバーを追加")
                .lore(
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.GRAY + "オンラインプレイヤーをメンバーとして追加",
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.DARK_GRAY + "クリックで選択"
                ).build());

        inventory.setItem(SLOT_RENT, ItemStackBuilder.of(Material.CLOCK)
                .name(ChatColor.YELLOW + "" + ChatColor.BOLD + "建築権を貸し出す")
                .lore(
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.GRAY + "期間限定でメンバー権限を付与",
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.DARK_GRAY + "クリックで選択"
                ).build());

        Role viewerRole = RegionRoles.roleOf(region, getPlayer().getUniqueId());
        if (viewerRole == Role.PRIMARY_OWNER || (viewerRole == Role.SUB_OWNER && PGConfig.allowSubownerTransfer())) {
            inventory.setItem(SLOT_TRANLFER, ItemStackBuilder.of(Material.GOLDEN_APPLE)
                    .name(ChatColor.GOLD + "" + ChatColor.BOLD + "領域を譲渡")
                    .lore(
                        ChatColor.DARK_GRAY + "─────────────────",
                        ChatColor.RED + "この操作は取り消せません",
                        ChatColor.DARK_GRAY + "─────────────────",
                        ChatColor.DARK_GRAY + "クリックで選択"
                    ).build());
        }
    }

    private String memberName(UUID uuid) { String name = Bukkit.getOfflinePlayer(uuid).getName(); return name == null ? uuid.toString() : name; }

    private boolean matchesQuery(UUID uuid) {
        if (query.isBlank()) return true;
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        String lower = query.toLowerCase(java.util.Locale.ROOT);
        return uuid.toString().contains(lower) || (name != null && name.toLowerCase(java.util.Locale.ROOT).contains(lower));
    }

    private ItemStack memberHead(UUID uuid, ChatColor nameColor, String roleLabel, String extraLore) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "─────────────────");
        lore.add(ChatColor.GRAY + "役割: " + ChatColor.WHITE + roleLabel);
        if (extraLore != null) lore.add(ChatColor.YELLOW + extraLore);
        lore.add(ChatColor.DARK_GRAY + "─────────────────");
        lore.add(ChatColor.DARK_GRAY + "クリックで操作");
        ItemStack head = ItemStackBuilder.of(Material.PLAYER_HEAD)
                .name(nameColor + "" + ChatColor.BOLD + (op.getName() != null ? op.getName() : uuid.toString()))
                .lore(lore.toArray(new String[0]))
                .build();
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

        if (slot == SLOT_BACK)     { back(); return; }
        if (slot == SLOT_SEARCH)   { ChatInputManager.waitFor(getPlayer(), (player, input) -> { query = "cancel".equalsIgnoreCase(input.trim()) ? "" : input.trim(); page = 0; open(); }); return; }
        if (slot == 48)            { page--; init(); return; }
        if (slot == 52)            { page++; init(); return; }
        if (slot == SLOT_ADD)      { if (ProtectionAccess.canManageMembers(getPlayer(), region)) openAddLelector(); else getPlayer().sendMessage(ChatColor.RED + "メンバーを管理する権限がありません。"); return; }
        if (slot == SLOT_RENT)     { if (ProtectionAccess.canManageMembers(getPlayer(), region)) openRentLelector(); else getPlayer().sendMessage(ChatColor.RED + "建築権を貸し出す権限がありません。"); return; }
        if (slot == SLOT_TRANLFER) {
            Role viewerRole = RegionRoles.roleOf(region, getPlayer().getUniqueId());
            if (viewerRole == Role.PRIMARY_OWNER || (viewerRole == Role.SUB_OWNER && PGConfig.allowSubownerTransfer())) {
                openTransferLelector();
            }
            return;
        }
        if (slot < memberSlots.size()) {
            if (!ProtectionAccess.canManageMembers(getPlayer(), region)) {
                getPlayer().sendMessage(ChatColor.RED + "メンバーを管理する権限がありません。");
                return;
            }
            new MemberActionGUI(getPlayer(), this, region, memberSlots.get(slot)).open();
        }
    }

    private List<OfflinePlayer> nonMemberCandidates() {
        List<OfflinePlayer> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            UUID u = online.getUniqueId();
            if (u.equals(getPlayer().getUniqueId())) continue;
            if (region.getOwners().contains(u)) continue;
            if (region.getMembers().contains(u)) continue;
            candidates.add(online);
        }
        return candidates;
    }

    private void openAddLelector() {
        if (!ProtectionAccess.canManageMembers(getPlayer(), region)) return;
        List<OfflinePlayer> candidates = nonMemberCandidates();
        new PlayerSelectGUI(getPlayer(), this, "■ 追加するプレイヤー", candidates, uuid -> {
            RegionMembers.add(region, uuid);            String protectionName = PlayerGuard.getInstance().getProtectionNameRepository().displayName(region);
            String targetName = java.util.Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse(uuid.toString());
            PlayerGuard.getInstance().getProtectionLogService().log(region, "メンバー追加", getPlayer().getUniqueId(), targetName + " (" + uuid + ")", null, "メンバー", "actor=" + getPlayer().getName() + ";actorUuid=" + getPlayer().getUniqueId());
            getPlayer().sendMessage(ChatColor.GREEN + targetName + " をメンバーに追加しました。");
            getPlayer().sendMessage(ChatColor.GRAY + "保護名: " + ChatColor.WHITE + protectionName);
            ProtectionRoleNotifier notifier = new ProtectionRoleNotifier(PlayerGuard.getInstance());
            boolean targetOnline = notifier.notifyTargetAndPrimaryOwner(region, getPlayer().getUniqueId(), uuid,
                    notifier.memberAddedTarget(getPlayer().getName(), protectionName),
                    notifier.primaryOwnerMessage(getPlayer().getName(), targetName, protectionName, "メンバー", true));
            if (!targetOnline) getPlayer().sendMessage(ChatColor.GRAY + "対象プレイヤーはオフラインのため、現在は本人へ通知されません。");
            getPlayer().playSound(getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 10, 2);
            open();
        }).open();
    }

    private void openRentLelector() {
        List<OfflinePlayer> candidates = nonMemberCandidates();
        new PlayerSelectGUI(getPlayer(), this, "■ 貸出先プレイヤー", candidates, uuid -> {
            Player t = Bukkit.getPlayer(uuid);
            String name = t != null ? t.getName() : Bukkit.getOfflinePlayer(uuid).getName();
            new RentalDurationGUI(getPlayer(), this, region, uuid,
                    name != null ? name : uuid.toString()).open();
        }).open();
    }

    private void openTransferLelector() {
        List<OfflinePlayer> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(getPlayer().getUniqueId())) continue;
            candidates.add(online);
        }
        new PlayerSelectGUI(getPlayer(), this, "■ 譲渡先プレイヤー", candidates, uuid -> {
            Player to = Bukkit.getPlayer(uuid);
            if (to != null) {
                TransferCommand.requestTransfer(getPlayer(), region, to);
            } else {
                getPlayer().sendMessage(PGMessages.warn("対象プレイヤーはオフラインになりました。"));
            }
            getPlayer().closeInventory();
        }).open();
    }
}
