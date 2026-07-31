package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.commons.spigot.persistence.EnumDataType;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.flag.GuardFlags;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.*;

public class MenuGUI extends AbstractGUI {

    private static final int INV_SIZE = 54;
    private static final Material BORDER = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material SEP_BG = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material CAT_ICON = Material.CYAN_STAINED_GLASS_PANE;

    private static final Map<Integer, String> CATEGORY_NAMES = new LinkedHashMap<>();
    static {
        CATEGORY_NAMES.put(1, "プレイヤー操作");
        CATEGORY_NAMES.put(2, "環境・ワールド");
    }

    private final ProtectedRegion region;

    public MenuGUI(PlayerGuard instance, Player player, ProtectedRegion region) {
        this(instance, player, region, null);
    }

    public MenuGUI(PlayerGuard instance, Player player, ProtectedRegion region, AbstractGUI parent) {
        super(player, parent);
        this.region = region;
    }

    @Override
    public void init() {
        if (inventory == null)
            inventory = Bukkit.createInventory(this, INV_SIZE, ChatColor.AQUA + "■ " + ChatColor.WHITE + "権限を管理");
        inventory.clear();

        // Fill everything with border glass as background
        ItemStack border = ItemStackBuilder.of(BORDER).name(" ").build();
        for (int i = 0; i < INV_SIZE; i++) {
            inventory.setItem(i, border);
        }

        NamespacedKey key = new NamespacedKey(PlayerGuard.getInstance(), "flag");

        // Group flags by guiRow, preserving declaration order
        Map<Integer, List<GuardFlags>> byRow = new LinkedHashMap<>();
        for (GuardFlags gf : GuardFlags.values()) {
            byRow.computeIfAbsent(gf.getGuiRow(), k -> new ArrayList<>()).add(gf);
        }

        boolean isFirst = true;
        for (Map.Entry<Integer, List<GuardFlags>> entry : byRow.entrySet()) {
            int guiRow = entry.getKey();
            List<GuardFlags> flags = entry.getValue();

            int contentVisualRow;
            if (isFirst) {
                contentVisualRow = 1;
                isFirst = false;

                // First category label at top border center
                String catName = CATEGORY_NAMES.getOrDefault(guiRow, "設定");
                inventory.setItem(4, ItemStackBuilder.of(CAT_ICON)
                        .name(ChatColor.AQUA + "" + ChatColor.BOLD + "◆ " + catName)
                        .build());
            } else {
                // Separator row before this category
                int sepVisualRow = (guiRow - 1) * 2;
                contentVisualRow = sepVisualRow + 1;

                int sepOffset = sepVisualRow * 9;
                ItemStack sep = ItemStackBuilder.of(SEP_BG).name(" ").build();
                for (int i = 0; i < 9; i++) {
                    inventory.setItem(sepOffset + i, sep);
                }

                // Category label at center of separator row
                String catName = CATEGORY_NAMES.getOrDefault(guiRow, "設定");
                inventory.setItem(sepOffset + 4, ItemStackBuilder.of(CAT_ICON)
                        .name(ChatColor.AQUA + "" + ChatColor.BOLD + "◆ " + catName)
                        .build());
            }

            // Place items centered in their content row
            int offset = contentVisualRow * 9;
            int startCol = (9 - flags.size()) / 2;
            for (int i = 0; i < flags.size(); i++) {
                GuardFlags gf = flags.get(i);
                GuardFlags.State state = GuardFlags.getState(region, gf);

                ItemStack item = ItemStackBuilder.of(gf.getIcon())
                        .name(ChatColor.YELLOW + "" + ChatColor.BOLD + gf.getDisplayName())
                        .lore(
                            ChatColor.DARK_GRAY + "─────────────────",
                            ChatColor.GRAY + "状態：" + stateToColored(state),
                            ChatColor.DARK_GRAY + "─────────────────",
                            ChatColor.DARK_GRAY + "クリックで切り替え"
                        )
                        .persistentData(key, new EnumDataType<>(GuardFlags.class), gf)
                        .build();
                inventory.setItem(offset + startCol + i, item);
            }
        }

        // Back button at bottom-left (slot 45)
        if (getParent() != null) {
            inventory.setItem(45, ItemStackBuilder.of(Material.ARROW)
                    .name(ChatColor.WHITE + "← 戻る")
                    .build());
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;

        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getType().isAir()) return;

        if (getParent() != null && e.getCurrentItem().getType() == Material.ARROW) {
            back();
            return;
        }

        NamespacedKey key = new NamespacedKey(PlayerGuard.getInstance(), "flag");
        PersistentDataContainer c = e.getCurrentItem().getItemMeta().getPersistentDataContainer();

        GuardFlags flag = c.get(key, new EnumDataType<>(GuardFlags.class));
        if (flag == null)
            return;

        if (PGConfig.isFlagDisabled(flag)) {
            GuardFlags.initRegionFlag(region, flag);

            getPlayer().playSound(getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 10, 0);
            init();
            return;
        }

        GuardFlags.State state = GuardFlags.getState(region, flag);

        List<GuardFlags.State> states = Arrays.asList(GuardFlags.State.values());

        int index = states.indexOf(state) + 1;
        if (index >= states.size()) {
            index = 0;
        }

        GuardFlags.State nextState = states.get(index);
        if (nextState == GuardFlags.State.SOME_CHANGED) {
            nextState = GuardFlags.State.ALLOW;
        }

        if (nextState == GuardFlags.State.UNSET) {
            for (StateFlag sff : flag.getFlags()) {
                region.setFlag(sff, null);
                region.setFlag(sff.getRegionGroupFlag(), RegionGroup.NONE);
            }
        } else {
            boolean b = state == GuardFlags.State.ALLOW;
            for (StateFlag sff : flag.getFlags()) {
                region.setFlag(sff, PGUtil.boolToState(!b));
                region.setFlag(sff.getRegionGroupFlag(), flag.regionGroup());
            }
        }

        getPlayer().playSound(getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 10, 2);

        init();
    }

    private String stateToColored(GuardFlags.State state) {
        switch (state) {
            case ALLOW:        return ChatColor.GREEN + "✔ 許可";
            case DENY:         return ChatColor.RED + "✘ 拒否";
            case SOME_CHANGED: return ChatColor.GOLD + "⚠ 管理者設定";
            case UNSET:        return ChatColor.GRAY + "◉ 設定解除";
        }
        return ChatColor.DARK_GRAY + "不明";
    }
}
