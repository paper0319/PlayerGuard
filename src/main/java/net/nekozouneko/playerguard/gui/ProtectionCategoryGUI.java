package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.paid.ProtectionCustomizationRepository;
import net.nekozouneko.playerguard.region.ProtectionAccess;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Fixed-layout category chooser. Category rows and navigation never move between pages. */
public class ProtectionCategoryGUI extends AbstractGUI {
    private static final int SIZE = 27;
    private static final int SLOT_PREV = 18;
    private static final int SLOT_PAGE = 22;
    private static final int SLOT_NEXT = 24;
    private static final int SLOT_BACK = 26;
    static final int[] CATEGORY_SLOTS = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15, 16};

    private final ProtectedRegion region;
    private int page;

    public ProtectionCategoryGUI(Player player, AbstractGUI parent, ProtectedRegion region) {
        super(player, parent);
        this.region = region;
    }

    @Override
    public void init() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, SIZE, ChatColor.YELLOW + "カテゴリ選択");
        }
        inventory.clear();
        ItemStack pane = ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, pane);
        }

        ProtectionCustomizationRepository repository = PlayerGuard.getInstance().getProtectionCustomizationRepository();
        ProtectionCustomizationRepository.Category[] categories = ProtectionCustomizationRepository.Category.values();
        int totalPages = totalPages(categories.length);
        page = Math.clamp(page, 0, totalPages - 1);
        int offset = page * CATEGORY_SLOTS.length;
        int count = Math.min(CATEGORY_SLOTS.length, categories.length - offset);
        int[] slots = categorySlotsForCount(count);
        ProtectionCustomizationRepository.Category current = repository.category(region);
        for (int index = 0; index < count; index++) {
            ProtectionCustomizationRepository.Category category = categories[offset + index];
            boolean selected = category == current;
            inventory.setItem(slots[index], categoryItem(category, selected));
        }

        inventory.setItem(SLOT_PREV, page > 0 ? navigationItem(Material.ARROW, ChatColor.WHITE + "前のページ") : disabledNavigationItem());
        inventory.setItem(SLOT_PAGE, ItemStackBuilder.of(Material.PAPER)
                .name(ChatColor.YELLOW + "" + ChatColor.BOLD + (page + 1) + " / " + totalPages + " ページ")
                .lore(ChatColor.GRAY + "カテゴリ選択")
                .build());
        inventory.setItem(SLOT_NEXT, page < totalPages - 1 ? navigationItem(Material.ARROW, ChatColor.WHITE + "次のページ") : disabledNavigationItem());
        inventory.setItem(SLOT_BACK, navigationItem(Material.ARROW, ChatColor.WHITE + "戻る"));
    }

    private ItemStack categoryItem(ProtectionCustomizationRepository.Category category, boolean selected) {
        return ItemStackBuilder.of(category.icon)
                .name((selected ? ChatColor.GREEN + "" + ChatColor.BOLD : ChatColor.YELLOW.toString()) + category.label)
                .lore(ChatColor.DARK_GRAY + "────────────",
                        selected ? ChatColor.GRAY + "現在選択中のカテゴリです" : ChatColor.GRAY + "クリックして設定")
                .build();
    }

    private ItemStack navigationItem(Material material, String name) {
        return ItemStackBuilder.of(material).name(name).build();
    }

    private ItemStack disabledNavigationItem() {
        return ItemStackBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
    }

    static int totalPages(int categoryCount) {
        return Math.max(1, (categoryCount + CATEGORY_SLOTS.length - 1) / CATEGORY_SLOTS.length);
    }

    static int[] categorySlotsForCount(int count) {
        if (count < 1 || count > CATEGORY_SLOTS.length) {
            throw new IllegalArgumentException("count must fit the category area");
        }
        if (count <= 7) {
            return centeredRowSlots(count, 1);
        }
        int topCount = (count + 1) / 2;
        int bottomCount = count / 2;
        int[] slots = new int[count];
        System.arraycopy(centeredRowSlots(topCount, 0), 0, slots, 0, topCount);
        System.arraycopy(centeredRowSlots(bottomCount, 1), 0, slots, topCount, bottomCount);
        return slots;
    }

    private static int[] centeredRowSlots(int count, int row) {
        int[][] columns = {
                {}, {4}, {3, 5}, {3, 4, 5}, {2, 3, 5, 6},
                {2, 3, 4, 5, 6}, {1, 2, 3, 5, 6, 7}, {1, 2, 3, 4, 5, 6, 7}
        };
        int[] slots = new int[count];
        for (int index = 0; index < count; index++) {
            slots[index] = row * 9 + columns[count][index];
        }
        return slots;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getHolder() != this) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != topInventory) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot == SLOT_BACK) {
            back();
            return;
        }
        int totalPages = totalPages(ProtectionCustomizationRepository.Category.values().length);
        if (slot == SLOT_PREV && page > 0) {
            page--;
            init();
            return;
        }
        if (slot == SLOT_NEXT && page < totalPages - 1) {
            page++;
            init();
            return;
        }

        int offset = page * CATEGORY_SLOTS.length;
        int[] slots = categorySlotsForCount(Math.min(CATEGORY_SLOTS.length, ProtectionCustomizationRepository.Category.values().length - offset));
        for (int index = 0; index < slots.length; index++) {
            if (slot != slots[index]) {
                continue;
            }
            if (!ProtectionAccess.canManageProtection(getPlayer(), region)) {
                getPlayer().sendMessage(ChatColor.RED + "カテゴリを変更する権限がありません。");
                return;
            }
            ProtectionCustomizationRepository.Category[] categories = ProtectionCustomizationRepository.Category.values();
            ProtectionCustomizationRepository.Category selected = categories[offset + index];
            ProtectionCustomizationRepository repository = PlayerGuard.getInstance().getProtectionCustomizationRepository();
            String before = repository.category(region).label;
            repository.saveCategory(region, selected);
            PlayerGuard.getInstance().getProtectionLogService().log(region, "カテゴリ変更", getPlayer().getUniqueId(), null, before, selected.label, null);
            getPlayer().sendMessage(ChatColor.GREEN + "保護カテゴリを " + selected.label + " に設定しました。");
            back();
            return;
        }
    }
}