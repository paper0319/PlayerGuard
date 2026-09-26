package net.nekozouneko.playerguard.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.command.ProtectionCreationService;
import net.nekozouneko.playerguard.gui.ClaimConfirmGUI;
import net.nekozouneko.playerguard.gui.ProtectionGuiText;
import net.nekozouneko.playerguard.paid.CreationConfirmationMode;
import net.nekozouneko.playerguard.paid.ProtectionPricingPreview;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getItem() == null || e.getItem().getType() != Material.GOLDEN_AXE) return;
        e.setCancelled(true);

        CuboidRegion selection = PlayerGuard.getInstance().getSelectionStorage().getSelection(e.getPlayer().getUniqueId());
        if (selection == null || !selection.getPos1().equals(selection.getPos2())) {
            PlayerGuard.getInstance().getCreationConfirmationManager().clear(e.getPlayer().getUniqueId());
            selection = CuboidRegion.fromCenter(BukkitAdapter.asBlockVector(e.getClickedBlock().getLocation()), 0);
            e.getPlayer().sendMessage(String.format(ChatColor.DARK_AQUA + "■ " + ChatColor.AQUA + "%s を設定しました。", locationFormatted(selection.getPos1())));
            PlayerGuard.getInstance().getSelectionStorage().putSelection(e.getPlayer().getUniqueId(), selection, e.getClickedBlock().getWorld().getName());
            return;
        }

        selection = selection.clone();
        selection.setPos2(BukkitAdapter.asBlockVector(e.getClickedBlock().getLocation()));
        PlayerGuard.getInstance().getSelectionStorage().putSelection(e.getPlayer().getUniqueId(), selection, e.getClickedBlock().getWorld().getName());
        ProtectionPricingPreview.Result preview = ProtectionCreationService.preview(e.getPlayer(), selection);
        sendPreview(e.getPlayer(), selection, preview);
        PlayerGuard.getInstance().getCreationConfirmationManager().begin(e.getPlayer(), selection, e.getClickedBlock().getWorld(), preview);
        if (PlayerGuard.getInstance().getCreationConfirmationSettings().get(e.getPlayer().getUniqueId()) != CreationConfirmationMode.CHAT) {
            new ClaimConfirmGUI(e.getPlayer()).open();
        }
    }

    private void sendPreview(org.bukkit.entity.Player player, CuboidRegion selection, ProtectionPricingPreview.Result preview) {
        player.sendMessage(ChatColor.AQUA + "土地保護の確認");
        player.sendMessage(ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━");
        player.sendMessage(ChatColor.GRAY + "範囲: " + ChatColor.WHITE + locationFormatted(selection.getPos1()) + " → " + locationFormatted(selection.getPos2()));
        player.sendMessage(ChatColor.GRAY + "体積: " + ChatColor.YELLOW + ProtectionGuiText.blocks(selection.getVolume()));
        player.sendMessage(ChatColor.GRAY + "現在の総保護体積: " + ChatColor.WHITE + ProtectionGuiText.blocks(preview.beforeVolume()));
        player.sendMessage(ChatColor.GRAY + "作成後の総保護体積: " + ChatColor.WHITE + ProtectionGuiText.blocks(preview.afterVolume()));
        player.sendMessage(ChatColor.GRAY + "無料保護上限: " + ChatColor.YELLOW + ProtectionGuiText.blocks(preview.freeLimit()));
        player.sendMessage(ChatColor.GRAY + "無料対象: " + ChatColor.GREEN + ProtectionGuiText.blocks(preview.freeAddedVolume()));
        player.sendMessage(ChatColor.GRAY + "有料対象: " + ChatColor.RED + ProtectionGuiText.blocks(preview.paidAddedVolume()));
        if (!preview.bands().isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "価格内訳:");
            for (ProtectionPricingPreview.Band band : preview.bands()) {
                player.sendMessage(ChatColor.WHITE + String.format("%,d～%,d: %,d × %s = %s", band.from(), band.to(), band.volume(), ProtectionGuiText.rate(band.rate()), ProtectionGuiText.money(band.amount())));
            }
        }
        player.sendMessage(ChatColor.GRAY + "必要金額: " + ChatColor.YELLOW + ProtectionGuiText.money(preview.total()));
        if (PlayerGuard.getInstance().getEconomyTransactionService().available()) {
            java.math.BigDecimal balance = java.math.BigDecimal.valueOf(PlayerGuard.getInstance().getEconomyTransactionService().balance(player));
            player.sendMessage(ChatColor.GRAY + "所持金: " + ChatColor.WHITE + ProtectionGuiText.money(balance));
            player.sendMessage(ChatColor.GRAY + "作成後残高: " + ChatColor.WHITE + ProtectionGuiText.money(balance.subtract(preview.total())));
        }
        player.sendMessage(ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━");
        player.sendMessage(ChatColor.YELLOW + "確認GUIを開きました");
    }

    private String locationFormatted(BlockVector3 vector3) { return String.format("(%d,%d,%d)", vector3.x(), vector3.y(), vector3.z()); }
}