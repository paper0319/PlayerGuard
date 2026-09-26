package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.flag.PGCustomFlags;
import net.nekozouneko.playerguard.paid.ProtectionCustomizationRepository;
import net.nekozouneko.playerguard.region.RegionRoles;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProtectionCopyConfirmGUI extends AbstractGUI {
    private final ProtectedRegion source;
    private final ProtectedRegion target;
    private final World targetWorld;
    private final EnumSet<ProtectionCopyOptionsGUI.Option> options;
    private boolean executed;

    public ProtectionCopyConfirmGUI(Player player, AbstractGUI parent, ProtectedRegion source, ProtectedRegion target, World targetWorld, EnumSet<ProtectionCopyOptionsGUI.Option> options) {
        super(player, parent);
        this.source = source;
        this.target = target;
        this.targetWorld = targetWorld;
        this.options = EnumSet.copyOf(options);
    }

    @Override public void init() {
        if (inventory == null) inventory = Bukkit.createInventory(this, 27, ChatColor.GREEN + "コピー確認");
        inventory.clear();
        org.bukkit.inventory.ItemStack glass = ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) inventory.setItem(i, glass);
        String src = PlayerGuard.getInstance().getProtectionNameRepository().displayName(source);
        String dst = PlayerGuard.getInstance().getProtectionNameRepository().displayName(target);
        inventory.setItem(13, ItemStackBuilder.of(Material.PAPER).name(ChatColor.YELLOW + "保護設定コピー")
                .lore(ChatColor.GRAY + "コピー元: " + ChatColor.WHITE + src,
                        ChatColor.GRAY + "コピー先: " + ChatColor.WHITE + dst,
                        ChatColor.GRAY + "項目: " + ChatColor.WHITE + options.stream().map(o -> o.label).collect(Collectors.joining(", ")),
                        ChatColor.RED + "選択項目を上書きします")
                .build());
        inventory.setItem(11, ItemStackBuilder.of(Material.LIME_CONCRETE).name(ChatColor.GREEN + "コピー実行").build());
        inventory.setItem(15, ItemStackBuilder.of(Material.RED_CONCRETE).name(ChatColor.RED + "キャンセル").build());
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() != this) return;
        e.setCancelled(true);
        if (e.getRawSlot() == 15) { back(); return; }
        if (e.getRawSlot() != 11 || executed) return;
        executed = true;
        if (!RegionRoles.isPrimaryOwner(source, getPlayer().getUniqueId()) || !RegionRoles.isPrimaryOwner(target, getPlayer().getUniqueId())) {
            getPlayer().sendMessage(ChatColor.RED + "正式なオーナーの保護だけコピーできます。");
            back();
            return;
        }
        copy();
    }

    private void copy() {
        ProtectionCustomizationRepository repo = PlayerGuard.getInstance().getProtectionCustomizationRepository();
        ProtectionCustomizationRepository.Category oldCat = repo.category(target);
        Set<UUID> oldOwners = new HashSet<>(target.getOwners().getUniqueIds());
        Set<UUID> oldMembers = new HashSet<>(target.getMembers().getUniqueIds());
        Set<String> oldRentals = target.getFlag(PGCustomFlags.RENTALS) == null ? null : new HashSet<>(target.getFlag(PGCustomFlags.RENTALS));
        UUID oldPrimary = RegionRoles.getPrimaryOwner(target);
        Map<Flag<?>, Object> copiedFlags = Map.of();
        try {
            if (options.contains(ProtectionCopyOptionsGUI.Option.CATEGORY)) repo.saveCategory(target, repo.category(source));
            copiedFlags = ProtectionSettingsCopy.copy(source, target, options);
            PlayerGuard.getInstance().getProtectionLogService().log(target, "保護設定コピー", getPlayer().getUniqueId(), null, source.getId(), target.getId(), options.stream().map(o -> o.label).collect(Collectors.joining(", ")));
            getPlayer().sendMessage(ChatColor.GREEN + "保護設定をコピーしました。");
            new ProtectionCustomizeGUI(getPlayer(), getParent().getParent(), target, targetWorld).open();
        } catch (RuntimeException ex) {
            repo.saveCategory(target, oldCat);
            ProtectionSettingsCopy.restoreFlags(target, copiedFlags);
            if (options.contains(ProtectionCopyOptionsGUI.Option.MEMBER_ROLES)) {
                ProtectionSettingsCopy.restoreMemberRoles(target, oldOwners, oldMembers, oldRentals, oldPrimary);
            }
            getPlayer().sendMessage(ChatColor.RED + "コピーに失敗したため、変更を戻しました。");
            back();
        }
    }
}
