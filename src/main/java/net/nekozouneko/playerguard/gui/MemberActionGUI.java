package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.inventory.ItemStackBuilder;
import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.region.RegionRentals;
import net.nekozouneko.playerguard.region.RegionRoles;
import net.nekozouneko.playerguard.region.RegionRoles.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class MemberActionGUI extends AbstractGUI {

    // Layout: [BACK][HEAD][GLASS][PROMOTE][DEMOTE][CANCEL_RENTAL][GLASS][REMOVE][GLASS]
    //            0     1     2       3        4          5            6      7       8
    private static final int SLOT_BACK          = 0;
    private static final int SLOT_HEAD          = 1;
    private static final int SLOT_PROMOTE       = 3;
    private static final int SLOT_DEMOTE        = 4;
    private static final int SLOT_CANCEL_RENTAL = 5;
    private static final int SLOT_REMOVE        = 7;

    private static final Material GLASS = Material.BLACK_STAINED_GLASS_PANE;

    private final ProtectedRegion region;
    private final UUID target;

    public MemberActionGUI(Player player, AbstractGUI parent, ProtectedRegion region, UUID target) {
        super(player, parent);
        this.region = region;
        this.target = target;
    }

    private String targetName() {
        String name = Bukkit.getOfflinePlayer(target).getName();
        return name != null ? name : target.toString();
    }

    private boolean canPromote(Role viewer, Role targetRole, boolean rental) {
        return viewer == Role.PRIMARY_OWNER && targetRole == Role.BUILDER && !rental;
    }

    private boolean canDemote(Role viewer, Role targetRole) {
        return viewer == Role.PRIMARY_OWNER && targetRole == Role.SUB_OWNER;
    }

    private boolean canCancelRental(Role viewer, boolean rental) {
        return rental && (viewer == Role.PRIMARY_OWNER || viewer == Role.SUB_OWNER);
    }

    private boolean canRemove(Role viewer, Role targetRole, boolean rental) {
        if (rental) return false;
        if (viewer == Role.PRIMARY_OWNER)
            return targetRole == Role.SUB_OWNER || targetRole == Role.BUILDER;
        return viewer == Role.SUB_OWNER && targetRole == Role.BUILDER;
    }

    @Override
    public void init() {
        if (inventory == null)
            inventory = Bukkit.createInventory(this, 9,
                ChatColor.AQUA + "■ " + ChatColor.WHITE + "メンバー操作");
        inventory.clear();

        // Fill all with glass
        ItemStack glass = ItemStackBuilder.of(GLASS).name(" ").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, glass);

        Role viewer    = RegionRoles.roleOf(region, getPlayer().getUniqueId());
        Role targetRole = RegionRoles.roleOf(region, target);
        boolean rental  = RegionRentals.isRental(region, target);

        // Back (left-most)
        inventory.setItem(SLOT_BACK, ItemStackBuilder.of(Material.ARROW)
                .name(ChatColor.WHITE + "← 戻る")
                .lore(ChatColor.DARK_GRAY + "前の画面へ")
                .build());

        // Player head
        String roleLbl = roleDisplayName(targetRole, rental);
        String roleDesc = roleDescription(targetRole, rental);
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        ItemStack head = ItemStackBuilder.of(Material.PLAYER_HEAD)
                .name(ChatColor.YELLOW + "" + ChatColor.BOLD + targetName())
                .lore(
                    ChatColor.DARK_GRAY + "─────────────────",
                    ChatColor.GRAY + "役割: " + ChatColor.WHITE + roleLbl,
                    ChatColor.GRAY + "  " + ChatColor.DARK_GRAY + "└ " + roleDesc,
                    ChatColor.DARK_GRAY + "─────────────────"
                ).build();
        if (head.getItemMeta() instanceof SkullMeta) {
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            sm.setOwningPlayer(op);
            head.setItemMeta(sm);
        }
        inventory.setItem(SLOT_HEAD, head);

        // Promote: 建築士 → サブオーナー
        if (canPromote(viewer, targetRole, rental)) {
            inventory.setItem(SLOT_PROMOTE, ItemStackBuilder.of(Material.EMERALD)
                    .name(ChatColor.GREEN + "" + ChatColor.BOLD + "サブオーナーに昇格")
                    .lore(
                        ChatColor.DARK_GRAY + "─────────────────",
                        ChatColor.GRAY + "建築士 → " + ChatColor.GOLD + "サブオーナー",
                        ChatColor.DARK_GRAY + "サブオーナー: " + ChatColor.GRAY + "メンバーの管理ができます",
                        ChatColor.DARK_GRAY + "─────────────────",
                        ChatColor.DARK_GRAY + "クリックで昇格"
                    ).build());
        }

        // Demote: サブオーナー → 建築士
        if (canDemote(viewer, targetRole)) {
            inventory.setItem(SLOT_DEMOTE, ItemStackBuilder.of(Material.GUNPOWDER)
                    .name(ChatColor.YELLOW + "" + ChatColor.BOLD + "建築士に降格")
                    .lore(
                        ChatColor.DARK_GRAY + "─────────────────",
                        ChatColor.GRAY + "サブオーナー → " + ChatColor.WHITE + "建築士",
                        ChatColor.DARK_GRAY + "建築士: " + ChatColor.GRAY + "建築だけができます",
                        ChatColor.DARK_GRAY + "─────────────────",
                        ChatColor.DARK_GRAY + "クリックで降格"
                    ).build());
        }

        // Cancel rental
        if (canCancelRental(viewer, rental)) {
            Long expiry = RegionRentals.getExpiry(region, target);
            String remaining = expiry != null
                    ? RegionRentals.formatRemaining(expiry - System.currentTimeMillis()) : "不明";
            inventory.setItem(SLOT_CANCEL_RENTAL, ItemStackBuilder.of(Material.CLOCK)
                    .name(ChatColor.YELLOW + "" + ChatColor.BOLD + "貸出を解約")
                    .lore(
                        ChatColor.DARK_GRAY + "─────────────────",
                        ChatColor.GRAY + "残り時間: " + ChatColor.WHITE + remaining,
                        ChatColor.DARK_GRAY + "─────────────────",
                        ChatColor.DARK_GRAY + "クリックで解約"
                    ).build());
        }

        // Remove
        if (canRemove(viewer, targetRole, rental)) {
            inventory.setItem(SLOT_REMOVE, ItemStackBuilder.of(Material.RED_DYE)
                    .name(ChatColor.RED + "" + ChatColor.BOLD + "メンバーから削除")
                    .lore(
                        ChatColor.DARK_GRAY + "─────────────────",
                        ChatColor.RED + "この操作は取り消せません",
                        ChatColor.DARK_GRAY + "─────────────────",
                        ChatColor.DARK_GRAY + "クリックで削除"
                    ).build());
        }
    }

    /** 表示用の役割名（子供でもわかる名前） */
    private String roleDisplayName(Role role, boolean rental) {
        if (rental) return ChatColor.YELLOW + "建築士（貸出中）";
        switch (role) {
            case PRIMARY_OWNER: return ChatColor.GOLD + "オーナー";
            case SUB_OWNER:     return ChatColor.GOLD + "サブオーナー";
            case BUILDER:       return ChatColor.WHITE + "建築士";
            default:            return ChatColor.GRAY + "非メンバー";
        }
    }

    /** 役割の一言説明 */
    private String roleDescription(Role role, boolean rental) {
        if (rental) return ChatColor.GRAY + "期間限定で建築できます";
        switch (role) {
            case PRIMARY_OWNER: return ChatColor.GRAY + "領域のすべてを管理できます";
            case SUB_OWNER:     return ChatColor.GRAY + "メンバーの管理ができます";
            case BUILDER:       return ChatColor.GRAY + "領域の中で建築できます";
            default:            return "";
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);

        Role viewer    = RegionRoles.roleOf(region, getPlayer().getUniqueId());
        Role targetRole = RegionRoles.roleOf(region, target);
        boolean rental  = RegionRentals.isRental(region, target);

        switch (e.getRawSlot()) {
            case SLOT_BACK:
                back();
                break;
            case SLOT_PROMOTE:
                if (canPromote(viewer, targetRole, rental)
                        && RegionRoles.promote(region, target) == RegionRoles.PromoteResult.PROMOTED) {
                    getPlayer().sendMessage(PGMessages.success("%s を サブオーナー に昇格しました。", PGMessages.highlight(targetName())));
                    clickFeedbackAndBack();
                } else init();
                break;
            case SLOT_DEMOTE:
                if (canDemote(viewer, targetRole)
                        && RegionRoles.demote(region, target) == RegionRoles.DemoteResult.DEMOTED) {
                    getPlayer().sendMessage(PGMessages.success("%s を 建築士 に降格しました。", PGMessages.highlight(targetName())));
                    clickFeedbackAndBack();
                } else init();
                break;
            case SLOT_CANCEL_RENTAL:
                if (canCancelRental(viewer, rental)
                        && RegionRoles.removeMember(region, target) == RegionRoles.RemoveRoleResult.REMOVED) {
                    getPlayer().sendMessage(PGMessages.success("%s への貸出を解約しました。", PGMessages.highlight(targetName())));
                    clickFeedbackAndBack();
                } else init();
                break;
            case SLOT_REMOVE:
                if (canRemove(viewer, targetRole, rental)
                        && RegionRoles.removeMember(region, target) == RegionRoles.RemoveRoleResult.REMOVED) {
                    getPlayer().sendMessage(PGMessages.success("%s をメンバーから削除しました。", PGMessages.highlight(targetName())));
                    clickFeedbackAndBack();
                } else init();
                break;
            default:
                break;
        }
    }

    private void clickFeedbackAndBack() {
        getPlayer().playSound(getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 10, 1);
        back();
    }
}
