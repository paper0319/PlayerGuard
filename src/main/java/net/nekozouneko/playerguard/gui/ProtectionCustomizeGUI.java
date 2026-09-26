package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.paid.ProtectionCustomizationRepository;
import net.nekozouneko.playerguard.region.ProtectionAccess;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ProtectionCustomizeGUI extends AbstractGUI {
    private static final int SLOT_CATEGORY = 11;
    private static final int SLOT_COPY = 15;
    private static final int SLOT_BACK = 18;
    private final ProtectedRegion region;
    private final World world;

    public ProtectionCustomizeGUI(Player player, AbstractGUI parent, ProtectedRegion region, World world) {
        super(player, parent);
        this.region = region;
        this.world = world;
    }

    @Override
    public void init() {
        if (inventory == null) inventory = Bukkit.createInventory(this, 27, ChatColor.BLACK + "保護カスタマイズ");
        inventory.clear();
        ItemStack glass = ItemStackBuilder.of(Material.LIGHT_BLUE_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) inventory.setItem(i, glass);
        ProtectionCustomizationRepository repo = PlayerGuard.getInstance().getProtectionCustomizationRepository();
        ProtectionCustomizationRepository.Category category = repo.category(region);
        inventory.setItem(SLOT_CATEGORY, ItemStackBuilder.of(category.icon)
                .name(ChatColor.YELLOW + "保護カテゴリ")
                .lore(ChatColor.GRAY + "現在: " + ChatColor.YELLOW + category.label)
                .build());
        inventory.setItem(SLOT_COPY, ItemStackBuilder.of(Material.WRITABLE_BOOK)
                .name(ChatColor.GREEN + "保護設定コピー")
                .lore(ChatColor.GRAY + "別の自分の保護から設定をコピーします")
                .build());
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "戻る").build());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);
        switch (e.getRawSlot()) {
            case SLOT_CATEGORY:
                if (!ProtectionAccess.canManageProtection(getPlayer(), region)) { getPlayer().sendMessage(ChatColor.RED + "保護をカスタマイズする権限がありません。"); return; }
                new ProtectionCategoryGUI(getPlayer(), this, region).open();
                break;
            case SLOT_COPY:
                if (!ProtectionAccess.canManageProtection(getPlayer(), region)) { getPlayer().sendMessage(ChatColor.RED + "保護をカスタマイズする権限がありません。"); return; }
                new ProtectionCopySourceGUI(getPlayer(), this, region, world).open();
                break;
            case SLOT_BACK: back(); break;
            default: break;
        }
    }
}
