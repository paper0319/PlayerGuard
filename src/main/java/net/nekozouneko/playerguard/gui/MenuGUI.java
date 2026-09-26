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
import net.nekozouneko.playerguard.region.ProtectionAccess;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataContainer;
import java.util.*;

public class MenuGUI extends AbstractGUI {
    private static final int SIZE = 45;
    private static final int SLOT_BACK = 44;
    private final ProtectedRegion region;
    public MenuGUI(PlayerGuard instance, Player player, ProtectedRegion region) { this(instance, player, region, null); }
    public MenuGUI(PlayerGuard instance, Player player, ProtectedRegion region, AbstractGUI parent) { super(player, parent); this.region = region; }
    @Override public void init() {
        if (inventory == null) inventory = Bukkit.createInventory(this, SIZE, ChatColor.BLACK + "権限を管理");
        inventory.clear();
        org.bukkit.inventory.ItemStack glass = ItemStackBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < SIZE; i++) inventory.setItem(i, glass);
        NamespacedKey key = new NamespacedKey(PlayerGuard.getInstance(), "flag");
        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25};
        GuardFlags[] flags = GuardFlags.values();
        for (int i = 0; i < flags.length && i < slots.length; i++) {
            GuardFlags flag = flags[i];
            inventory.setItem(slots[i], ItemStackBuilder.of(flag.getIcon()).name(ChatColor.YELLOW + "§l" + flag.getDisplayName()).lore(ChatColor.GRAY + "現在: " + stateText(GuardFlags.getState(region, flag)), ChatColor.DARK_GRAY + "クリックで切替").persistentData(key, new EnumDataType<>(GuardFlags.class), flag).build());
        }
        if (getParent() != null) inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW).name(ChatColor.WHITE + "§l戻る").lore(ChatColor.GRAY + "前の画面へ戻ります").build());
    }
    @EventHandler public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);
        if (e.getRawSlot() == SLOT_BACK) { back(); return; }
        if (!ProtectionAccess.canManageFlags(getPlayer(), region)) {
            getPlayer().sendMessage(ChatColor.RED + "フラグを変更する権限がありません。");
            return;
        }
        if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;
        PersistentDataContainer data = e.getCurrentItem().getItemMeta().getPersistentDataContainer();
        GuardFlags flag = data.get(new NamespacedKey(PlayerGuard.getInstance(), "flag"), new EnumDataType<>(GuardFlags.class));
        if (flag == null) return;
        if (PGConfig.isFlagDisabled(flag)) { GuardFlags.initRegionFlag(region, flag); init(); return; }
        GuardFlags.State current = GuardFlags.getState(region, flag);
        GuardFlags.State next = current == GuardFlags.State.ALLOW ? GuardFlags.State.DENY : GuardFlags.State.ALLOW;
        for (StateFlag stateFlag : flag.getFlags()) { stateFlagSet(stateFlag, next, flag); }
        getPlayer().playSound(getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 1, 2); init();
    }
    private void stateFlagSet(StateFlag stateFlag, GuardFlags.State state, GuardFlags flag) { region.setFlag(stateFlag, PGUtil.boolToState(state == GuardFlags.State.ALLOW)); region.setFlag(stateFlag.getRegionGroupFlag(), flag.regionGroup()); }
    private String stateText(GuardFlags.State state) { return state == GuardFlags.State.ALLOW ? ChatColor.GREEN + "許可" : state == GuardFlags.State.DENY ? ChatColor.RED + "拒否" : ChatColor.GRAY + "未設定"; }
}