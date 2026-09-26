package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.EnumSet;

public class ProtectionCopyOptionsGUI extends AbstractGUI {
    enum Option {
        FLAGS("フラグ設定", Material.REDSTONE_TORCH),
        PERMISSIONS("権限設定", Material.COMPARATOR),
        MEMBER_ROLES("メンバー権限", Material.PLAYER_HEAD),
        CATEGORY("カテゴリ", Material.ITEM_FRAME);

        final String label;
        final Material icon;

        Option(String label, Material icon) {
            this.label = label;
            this.icon = icon;
        }
    }

    private static final int[] OPTION_SLOTS = {10, 12, 14, 16};
    private final ProtectedRegion source;
    private final ProtectedRegion target;
    private final World targetWorld;
    private final EnumSet<Option> selected = EnumSet.noneOf(Option.class);

    public ProtectionCopyOptionsGUI(Player player, AbstractGUI parent, ProtectedRegion source, ProtectedRegion target, World targetWorld) {
        super(player, parent);
        this.source = source;
        this.target = target;
        this.targetWorld = targetWorld;
    }

    @Override public void init() {
        if (inventory == null) inventory = Bukkit.createInventory(this, 27, ChatColor.GREEN + "コピー項目を選択");
        inventory.clear();
        org.bukkit.inventory.ItemStack glass = ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) inventory.setItem(i, glass);
        Option[] options = Option.values();
        for (int i = 0; i < options.length; i++) {
            Option option = options[i];
            boolean enabled = selected.contains(option);
            inventory.setItem(OPTION_SLOTS[i], ItemStackBuilder.of(option.icon)
                    .name((enabled ? ChatColor.GREEN : ChatColor.YELLOW) + option.label)
                    .lore(ChatColor.DARK_GRAY + "────────────", enabled ? ChatColor.GREEN + "コピーします" : ChatColor.GRAY + "クリックして選択")
                    .build());
        }
        inventory.setItem(26, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "戻る").build());
        inventory.setItem(22, ItemStackBuilder.of(Material.LIME_CONCRETE).name(ChatColor.GREEN + "確認へ").build());
        
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot == 26) { back(); return; }

        if (slot == 22) {
            if (!selected.isEmpty()) new ProtectionCopyConfirmGUI(getPlayer(), this, source, target, targetWorld, selected).open();
            return;
        }
        Option[] options = Option.values();
        for (int i = 0; i < OPTION_SLOTS.length && i < options.length; i++) {
            if (slot == OPTION_SLOTS[i]) {
                Option option = options[i];
                if (selected.contains(option)) selected.remove(option); else selected.add(option);
                init();
                return;
            }
        }
    }
}
