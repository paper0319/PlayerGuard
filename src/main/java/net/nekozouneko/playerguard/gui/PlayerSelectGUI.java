package net.nekozouneko.playerguard.gui;

import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 渡されたプレイヤー候補をヘッドで一覧し、クリックで onSelect を呼ぶ汎用GUI。
 * 戻るボタンはナビ行の左端（leftmost of nav row）に配置。
 */
public class PlayerSelectGUI extends AbstractGUI {

    private static final Material GLASS = Material.BLACK_STAINED_GLASS_PANE;

    private final String title;
    private final List<OfflinePlayer> candidates;
    private final Consumer<UUID> onSelect;
    private final List<UUID> slotOwners = new ArrayList<>();

    public PlayerSelectGUI(Player player, AbstractGUI parent,
                           String title, List<OfflinePlayer> candidates, Consumer<UUID> onSelect) {
        super(player, parent);
        this.title = title;
        this.candidates = candidates;
        this.onSelect = onSelect;
    }

    @Override
    public void init() {
        int playerRows = candidates.isEmpty() ? 1 : (int) Math.ceil(candidates.size() / 9.0);
        int totalRows  = Math.min(6, playerRows + 1);
        int size       = totalRows * 9;

        if (inventory == null)
            inventory = Bukkit.createInventory(this, size, title);
        inventory.clear();
        slotOwners.clear();

        int playerAreaSize = size - 9;

        // Fill nav row with glass
        ItemStack glass = ItemStackBuilder.of(GLASS).name(" ").build();
        for (int i = playerAreaSize; i < size; i++) inventory.setItem(i, glass);

        int slot = 0;
        for (OfflinePlayer op : candidates) {
            if (slot >= playerAreaSize) break;
            ItemStack head = ItemStackBuilder.of(Material.PLAYER_HEAD)
                    .name(ChatColor.YELLOW + "" + ChatColor.BOLD
                        + (op.getName() != null ? op.getName() : op.getUniqueId().toString()))
                    .lore(
                        ChatColor.DARK_GRAY + "─────────────────",
                        ChatColor.DARK_GRAY + "クリックで選択"
                    ).build();
            if (head.getItemMeta() instanceof SkullMeta) {
                SkullMeta sm = (SkullMeta) head.getItemMeta();
                sm.setOwningPlayer(op);
                head.setItemMeta(sm);
            }
            inventory.setItem(slot, head);
            slotOwners.add(op.getUniqueId());
            slot++;
        }

        // Back at left-most of nav row
        inventory.setItem(playerAreaSize, ItemStackBuilder.of(Material.ARROW)
                .name(ChatColor.WHITE + "← 戻る")
                .lore(ChatColor.DARK_GRAY + "前の画面へ")
                .build());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        int playerAreaSize = inventory.getSize() - 9;
        if (slot == playerAreaSize) {  // back button (left-most of nav row)
            back();
            return;
        }
        if (slot < slotOwners.size()) {
            UUID selected = slotOwners.get(slot);
            onSelect.accept(selected);
        }
    }
}
