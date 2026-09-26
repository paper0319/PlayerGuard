package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.paid.ProtectionTeleportRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ProtectionTeleportDeleteConfirmGUI extends AbstractGUI {
    private static final int SIZE = 27;
    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_INFO = 13;
    private static final int SLOT_CANCEL = 15;
    private final ProtectedRegion region;

    public ProtectionTeleportDeleteConfirmGUI(Player player, ProtectionTeleportGUI parent, ProtectedRegion region) {
        super(player, parent);
        this.region = region;
    }

    @Override public void init() {
        if (inventory == null) inventory = Bukkit.createInventory(this, SIZE, ChatColor.BLACK + "テレポート地点削除確認");
        inventory.clear();
        ItemStack glass = ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, glass);
        ProtectionTeleportRepository.TeleportPoint point = PlayerGuard.getInstance().getProtectionTeleportRepository().find(region);
        inventory.setItem(SLOT_INFO, ItemStackBuilder.of(Material.COMPASS).name(ChatColor.YELLOW + "削除するテレポート地点")
                .lore(ChatColor.DARK_GRAY + "────────────",
                        ChatColor.GRAY + "保護名: " + ChatColor.WHITE + PlayerGuard.getInstance().getProtectionNameRepository().displayName(region),
                        ChatColor.GRAY + "座標: " + ChatColor.WHITE + (point == null ? "未設定" : String.format("X%d Y%d Z%d", (int) Math.floor(point.x()), (int) Math.floor(point.y()), (int) Math.floor(point.z()))),
                        ChatColor.GRAY + "削除しても返金はありません。")
                .build());
        inventory.setItem(SLOT_CONFIRM, ItemStackBuilder.of(Material.LIME_CONCRETE).name(ChatColor.GREEN + "削除する").build());
        inventory.setItem(SLOT_CANCEL, ItemStackBuilder.of(Material.RED_CONCRETE).name(ChatColor.RED + "キャンセル").build());
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);
        if (e.getRawSlot() == SLOT_CANCEL) { back(); return; }
        if (e.getRawSlot() == SLOT_CONFIRM && getParent() instanceof ProtectionTeleportGUI gui) {
            gui.deleteTeleportPoint();
        }
    }
}
