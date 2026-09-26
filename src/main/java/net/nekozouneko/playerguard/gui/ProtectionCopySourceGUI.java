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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProtectionCopySourceGUI extends AbstractGUI {
    private final ProtectedRegion target;
    private final World targetWorld;
    private final List<Map.Entry<ProtectedRegion, World>> sources = new ArrayList<>();
    public ProtectionCopySourceGUI(Player player, AbstractGUI parent, ProtectedRegion target, World targetWorld) { super(player, parent); this.target = target; this.targetWorld = targetWorld; }
    @Override public void init() {
        if (inventory == null) inventory = Bukkit.createInventory(this, 54, ChatColor.GREEN + "コピー元保護を選択");
        inventory.clear();
        org.bukkit.inventory.ItemStack glass = ItemStackBuilder.of(Material.GREEN_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 45; i < 54; i++) inventory.setItem(i, glass);
        sources.clear();
        for (Map.Entry<ProtectedRegion, World> entry : PGUtil.getPlayerRegions(getPlayer()).entrySet()) {
            if (entry.getKey().getId().equals(target.getId())) continue;
            if (!RegionRoles.isPrimaryOwner(entry.getKey(), getPlayer().getUniqueId())) continue;
            sources.add(entry);
        }
        for (int i = 0; i < Math.min(45, sources.size()); i++) {
            ProtectedRegion r = sources.get(i).getKey();
            String name = PlayerGuard.getInstance().getProtectionNameRepository().displayName(r);
            inventory.setItem(i, ItemStackBuilder.of(Material.MAP).name(ChatColor.AQUA + name)
                    .lore(ChatColor.GRAY + "保護ID: " + ChatColor.WHITE + r.getId()).build());
        }
        if (sources.isEmpty()) inventory.setItem(22, ItemStackBuilder.of(Material.PAPER).name(ChatColor.GRAY + "コピー元にできる保護がありません").build());
        inventory.setItem(53, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "戻る").build());

    }
    @EventHandler public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot == 53) { back(); return; }

        if (slot < 0 || slot >= sources.size() || slot >= 45) return;
        new ProtectionCopyOptionsGUI(getPlayer(), this, sources.get(slot).getKey(), target, targetWorld).open();
    }
}