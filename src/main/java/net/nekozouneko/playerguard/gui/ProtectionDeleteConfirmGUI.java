package net.nekozouneko.playerguard.gui;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.command.ProtectionDeletionService;
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

import java.math.BigDecimal;
import java.time.Instant;

public class ProtectionDeleteConfirmGUI extends AbstractGUI {
    private static final int SLOT_INFO = 4;
    private static final int SLOT_DELETE = 11;
    private static final int SLOT_CANCEL = 15;
    private final ProtectedRegion region;
    private final World world;
    private boolean deleting;

    public ProtectionDeleteConfirmGUI(Player player, AbstractGUI parent, ProtectedRegion region, World world) {
        super(player, parent); this.region = region; this.world = world;
    }

    @Override public void init() {
        if (inventory == null) inventory = Bukkit.createInventory(this, 27, ChatColor.RED + "削除確認");
        inventory.clear();
        ProtectionPaymentRecord payment = PlayerGuard.getInstance().getProtectionPaymentRepository().find(region);
        BigDecimal paid = payment == null ? BigDecimal.ZERO : payment.amountPaid();
        Instant created = payment == null ? null : payment.createdAt();
        Instant now = Instant.now();
        BigDecimal rate = ProtectionGuiText.refundRate(created, now);
        BigDecimal refund = ProtectionGuiText.refund(paid, created, now);
        ChatColor rateColor = rate.compareTo(new BigDecimal("1.0")) >= 0 ? ChatColor.GREEN : rate.compareTo(new BigDecimal("0.8")) >= 0 ? ChatColor.YELLOW : ChatColor.RED;
        inventory.setItem(SLOT_INFO, ItemStackBuilder.of(Material.PAPER)
                .name(ChatColor.YELLOW + "返金情報")
                .lore(ChatColor.GRAY + "保護名: " + ChatColor.WHITE + PlayerGuard.getInstance().getProtectionNameRepository().displayName(region),
                        ChatColor.GRAY + "ワールド: " + ChatColor.WHITE + world.getName(),
                        ChatColor.GRAY + "体積: " + ChatColor.YELLOW + ProtectionGuiText.blocks(region.volume()),
                        ChatColor.GRAY + "支払金額: " + ChatColor.YELLOW + ProtectionGuiText.money(paid),
                        ChatColor.GRAY + "作成日時: " + ChatColor.WHITE + ProtectionGuiText.date(created),
                        ChatColor.GRAY + "経過時間: " + ChatColor.WHITE + ProtectionGuiText.elapsed(created, now),
                        ChatColor.GRAY + "適用返金率: " + rateColor + ProtectionGuiText.percent(rate),
                        ChatColor.GRAY + "返金予定額: " + ChatColor.GREEN + ProtectionGuiText.money(refund))
                .build());
        inventory.setItem(SLOT_DELETE, ItemStackBuilder.of(Material.LIME_CONCRETE).name(ChatColor.GREEN + "削除する").lore(ChatColor.RED + "この操作は取り消せません").build());
        inventory.setItem(SLOT_CANCEL, ItemStackBuilder.of(Material.RED_CONCRETE).name(ChatColor.RED + "キャンセル").build());
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return; e.setCancelled(true);
        if (e.getRawSlot() == SLOT_CANCEL) { back(); return; }
        if (e.getRawSlot() != SLOT_DELETE || deleting) return;
        deleting = true;
        deleteProtection();
    }

    private void deleteProtection() {
        Player player = getPlayer();
        PlayerGuard.getInstance().getScheduler().runGlobal(() -> {
            ProtectionDeletionService.Result result = ProtectionDeletionService.delete(player, region, world, "gui");
            if (result == ProtectionDeletionService.Result.DELETED) {
                sendAndReopen(ChatColor.GREEN + "保護 " + region.getId() + " を削除しました。");
            } else {
                fail("保護を削除できませんでした: " + result, "削除失敗");
            }
        });
    }
    private void fail(String message, String logType) {
        PlayerGuard.getInstance().getProtectionLogService().log(region, logType, getPlayer().getUniqueId(), null, null, null, message);
        sendAndReopen(ChatColor.RED + message);
    }

    private void sendAndReopen(String message) {
        PlayerGuard plugin = PlayerGuard.getInstance();
        plugin.getScheduler().runOnEntity(getPlayer(), () -> { getPlayer().sendMessage(message); if (getParent() != null) getParent().open(); else new RegionListGUI(getPlayer()).open(); });
    }
}