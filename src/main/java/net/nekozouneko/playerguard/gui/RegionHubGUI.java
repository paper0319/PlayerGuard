package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.paid.ProtectionCustomizationRepository;
import net.nekozouneko.playerguard.paid.ProtectionNameRepository;
import net.nekozouneko.playerguard.paid.ProtectionPaymentRecord;
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
import java.util.Map;

public class RegionHubGUI extends AbstractGUI {
    private static final int SIZE = 27;
    private static final int SLOT_BACK = 26;

    private static final int SLOT_DETAIL = 4;
    private static final int SLOT_FLAGS = 10;
    private static final int SLOT_MEMBERS = 11;
    private static final int SLOT_RENAME = 12;
    private static final int SLOT_TELEPORT = 13;
    private static final int SLOT_PROTECTION_LOG = 14;
    private static final int SLOT_CUSTOMIZE = 15;
    private static final int SLOT_DELETE = 16;
    private static final int SLOT_BLACKLIST = 22;
    private static final Material GLASS = Material.GRAY_STAINED_GLASS_PANE;

    private final ProtectedRegion region;
    private final World world;

    public RegionHubGUI(Player player, ProtectedRegion region) { this(player, region, null, null); }
    public RegionHubGUI(Player player, ProtectedRegion region, AbstractGUI parent) { this(player, region, findWorld(region, player), parent); }
    public RegionHubGUI(Player player, ProtectedRegion region, World world, AbstractGUI parent) { super(player, parent); this.region = region; this.world = world == null ? player.getWorld() : world; }

    @Override public void init() {
        if (inventory == null) inventory = Bukkit.createInventory(this, SIZE, ChatColor.AQUA + "■ " + ChatColor.WHITE + "保護管理");
        inventory.clear();
        ItemStack glass = ItemStackBuilder.of(GLASS).name(" ").build();
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, glass);

        String name = PlayerGuard.getInstance().getProtectionNameRepository().displayName(region);
        ProtectionCustomizationRepository.Category category = PlayerGuard.getInstance().getProtectionCustomizationRepository().category(region);
        ProtectionPaymentRecord payment = PlayerGuard.getInstance().getProtectionPaymentRepository().find(region);
        BigDecimal paid = payment == null ? BigDecimal.ZERO : payment.amountPaid();
        BigDecimal refund = payment == null ? BigDecimal.ZERO : ProtectionGuiText.refund(paid, payment.createdAt(), Instant.now());
        inventory.setItem(SLOT_DETAIL, ItemStackBuilder.of(Material.NETHER_STAR).name(ChatColor.AQUA + "" + ChatColor.BOLD + "土地保護情報")
                .lore(ChatColor.DARK_GRAY + "────────────",
                        ChatColor.GRAY + "保護名: " + ChatColor.WHITE + name,
                        ChatColor.GRAY + "保護ID: " + ChatColor.WHITE + region.getId(),
                        ChatColor.GRAY + "カテゴリ: " + ChatColor.YELLOW + category.label,
                        ChatColor.GRAY + "体積: " + ChatColor.YELLOW + ProtectionGuiText.blocks(region.volume()),
                        ChatColor.GRAY + "支払金額: " + ChatColor.YELLOW + ProtectionGuiText.money(paid),
                        ChatColor.GRAY + "返金予定額: " + ChatColor.GREEN + ProtectionGuiText.money(refund),
                        ChatColor.DARK_GRAY + "────────────",
                        ChatColor.YELLOW + "▶ クリックして詳細情報を開く")
                .build());
        Player viewer = getPlayer();
        if (ProtectionAccess.canManageFlags(viewer, region)) {
            inventory.setItem(SLOT_FLAGS, ItemStackBuilder.of(Material.YELLOW_CONCRETE).name(ChatColor.YELLOW + "権限管理・フラグ管理").lore(ChatColor.GRAY + "既存の権限とフラグ設定を開きます").build());
        }
        if (ProtectionAccess.canManageMembers(viewer, region)) {
            inventory.setItem(SLOT_MEMBERS, ItemStackBuilder.of(Material.CHEST).name(ChatColor.GREEN + "メンバー管理").lore(ChatColor.GRAY + "メンバー、サブオーナー、貸出、譲渡").build());
            inventory.setItem(SLOT_BLACKLIST, ItemStackBuilder.of(Material.BARRIER).name(ChatColor.RED + "ブラックリスト管理").lore(ChatColor.GRAY + "侵入・操作を禁止するプレイヤーを管理します").build());
        }
        if (ProtectionAccess.canManageProtection(viewer, region)) {
            inventory.setItem(SLOT_RENAME, ItemStackBuilder.of(Material.NAME_TAG).name(ChatColor.AQUA + "保護名変更").lore(ChatColor.GRAY + "1～32文字、カラーコード不可").build());
        }
        if (ProtectionAccess.canUseTeleport(viewer, region)) {
            boolean manageTeleport = ProtectionAccess.canManageTeleport(viewer, region);
            inventory.setItem(SLOT_TELEPORT, ItemStackBuilder.of(Material.ENDER_PEARL).name(ChatColor.AQUA + "" + ChatColor.BOLD + (manageTeleport ? "テレポート管理" : "保護へテレポート"))
                    .lore(ChatColor.GRAY + (manageTeleport ? "保護のテレポート地点を管理します" : "設定済みの地点へテレポートします"),
                            manageTeleport ? ChatColor.GRAY + "地点の作成・更新・削除ができます" : ChatColor.GRAY + "地点の作成はオーナーのみです")
                    .build());
        }
        if (ProtectionAccess.canViewLogs(viewer, region)) {
            inventory.setItem(SLOT_PROTECTION_LOG, ItemStackBuilder.of(Material.BOOK).name(ChatColor.YELLOW + "保護ログ").lore(ChatColor.GRAY + "訪問履歴や保護操作の履歴を確認します", ChatColor.GRAY + "クリックでログメニューを開きます").build());
        }
        if (ProtectionAccess.canManageProtection(viewer, region)) {
            inventory.setItem(SLOT_CUSTOMIZE, ItemStackBuilder.of(Material.PAINTING).name(ChatColor.AQUA + "保護カスタマイズ").lore(ChatColor.GRAY + "保護のカテゴリ・設定を管理します", ChatColor.GRAY + "クリックでカスタマイズメニューを開きます").build());
        }
        if (ProtectionAccess.canDeleteProtection(viewer, region)) {
            inventory.setItem(SLOT_DELETE, ItemStackBuilder.of(Material.RED_CONCRETE).name(ChatColor.RED + "削除").lore(ChatColor.GRAY + "確認画面を開きます").build());
        }
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "戻る").build());
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);
        switch (e.getRawSlot()) {
            case SLOT_BACK: back(); break;

            case SLOT_DETAIL: new ProtectionDetailGUI(getPlayer(), this, region, world).open(); break;
            case SLOT_FLAGS:
                if (!ProtectionAccess.canManageFlags(getPlayer(), region)) { deny("フラグを変更する権限がありません。"); return; }
                new MenuGUI(PlayerGuard.getInstance(), getPlayer(), region, this).open();
                break;
            case SLOT_MEMBERS:
                if (!ProtectionAccess.canManageMembers(getPlayer(), region)) { deny("メンバーを管理する権限がありません。"); return; }
                new MemberGUI(getPlayer(), this, region).open();
                break;
            case SLOT_BLACKLIST:
                if (!ProtectionAccess.canManageMembers(getPlayer(), region)) { deny("ブラックリストを管理する権限がありません。"); return; }
                new BlacklistGUI(getPlayer(), this, region).open();
                break;
            case SLOT_RENAME:
                if (!ProtectionAccess.canManageProtection(getPlayer(), region)) { deny("保護名を変更する権限がありません。"); return; }
                startRename();
                break;
            case SLOT_TELEPORT:
                if (!ProtectionAccess.canUseTeleport(getPlayer(), region)) { deny("この保護へテレポートする権限がありません。"); return; }
                new ProtectionTeleportGUI(getPlayer(), this, region, world).open();
                break;
            case SLOT_PROTECTION_LOG:
                if (!ProtectionAccess.canViewLogs(getPlayer(), region)) { deny("保護ログを見る権限がありません。"); return; }
                new ProtectionLogCategoryGUI(getPlayer(), this, region, world).open();
                break;
            case SLOT_CUSTOMIZE:
                if (!ProtectionAccess.canManageProtection(getPlayer(), region)) { deny("保護をカスタマイズする権限がありません。"); return; }
                new ProtectionCustomizeGUI(getPlayer(), this, region, world).open();
                break;
            case SLOT_DELETE:
                if (!ProtectionAccess.canDeleteProtection(getPlayer(), region)) { deny("保護を削除する権限がありません。"); return; }
                PlayerGuard.getInstance().getProtectionLogService().log(region, "削除操作開始", getPlayer().getUniqueId(), null, null, null, "GUI");
                new ProtectionDeleteConfirmGUI(getPlayer(), getParent(), region, world).open();
                break;
            default: break;
        }
    }

    private void deny(String message) {
        getPlayer().sendMessage(ChatColor.RED + message);
    }

    private void startRename() {
        if (!ProtectionAccess.canManageProtection(getPlayer(), region)) { deny("保護名を変更する権限がありません。"); return; }
        String before = PlayerGuard.getInstance().getProtectionNameRepository().displayName(region);
        ChatInputManager.waitFor(getPlayer(), (player, input) -> {
            if ("cancel".equalsIgnoreCase(input.trim())) { player.sendMessage(ChatColor.YELLOW + "保護名変更をキャンセルしました。"); open(); return; }
            ProtectionNameRepository repository = PlayerGuard.getInstance().getProtectionNameRepository();
            ProtectionNameRepository.ValidationResult result = repository.validate(input);
            if (result != ProtectionNameRepository.ValidationResult.OK) { player.sendMessage(ChatColor.RED + renameError(result)); open(); return; }
            repository.save(region, input);
            PlayerGuard.getInstance().getProtectionLogService().log(region, "保護名変更", player.getUniqueId(), null, before, input.trim(), null);
            player.sendMessage(ChatColor.GREEN + "保護名を変更しました: " + ChatColor.WHITE + input.trim());
            open();
        });
    }

    private String renameError(ProtectionNameRepository.ValidationResult result) {
        switch (result) {
            case INVALID_LENGTH: return "保護名は1～32文字で入力してください。";
            case COLOR_CODE: return "保護名にカラーコードは使用できません。";
            case LINE_BREAK: return "保護名に改行は使用できません。";
            default: return "保護名を変更できませんでした。";
        }
    }

    private static World findWorld(ProtectedRegion region, Player fallback) {
        Map.Entry<ProtectedRegion, World> entry = PGUtil.findPlayerGuardRegions(region.getId());
        return entry == null ? fallback.getWorld() : entry.getValue();
    }
}
