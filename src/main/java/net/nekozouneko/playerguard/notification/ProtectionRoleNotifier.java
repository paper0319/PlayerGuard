package net.nekozouneko.playerguard.notification;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.region.RegionRoles;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Delivers non-configurable notices for member and sub-owner role changes. */
public final class ProtectionRoleNotifier {
    private final PlayerGuard plugin;

    public ProtectionRoleNotifier(PlayerGuard plugin) {
        this.plugin = plugin;
    }

    public boolean notifyTargetAndPrimaryOwner(ProtectedRegion region, UUID actor, UUID target,
                                                String targetMessage, String ownerMessage) {
        boolean targetOnline = sendOnline(target, targetMessage);
        UUID primaryOwner = RegionRoles.getPrimaryOwner(region);
        if (primaryOwner != null && !primaryOwner.equals(actor) && !primaryOwner.equals(target)) {
            sendOnline(primaryOwner, ownerMessage);
        }
        return targetOnline;
    }

    private boolean sendOnline(UUID recipient, String message) {
        Player player = Bukkit.getPlayer(recipient);
        if (player == null) {
            return false;
        }
        plugin.getScheduler().runOnEntity(player, () -> player.sendMessage(message));
        return true;
    }

    public String displayName(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? uuid.toString() : name;
    }

    public String protectionName(ProtectedRegion region) {
        return plugin.getProtectionNameRepository().displayName(region);
    }

    public String memberAddedTarget(String actorName, String protectionName) {
        return ChatColor.GREEN + actorName + " があなたを " + ChatColor.WHITE + protectionName + ChatColor.GREEN + " のメンバーに追加しました。";
    }

    public String memberRemovedTarget(String actorName, String protectionName) {
        return ChatColor.YELLOW + actorName + " により " + ChatColor.WHITE + protectionName + ChatColor.YELLOW + " のメンバーから削除されました。";
    }

    public String subOwnerAddedTarget(String actorName, String protectionName) {
        return ChatColor.GOLD + actorName + " があなたを " + ChatColor.WHITE + protectionName + ChatColor.GOLD + " のサブオーナーに追加しました。\n"
                + ChatColor.YELLOW + "通常メンバーより強い管理権限を持っています。";
    }

    public String subOwnerRemovedTarget(String actorName, String protectionName) {
        return ChatColor.YELLOW + actorName + " により " + ChatColor.WHITE + protectionName + ChatColor.YELLOW + " のサブオーナーから削除されました。";
    }

    public String primaryOwnerMessage(String actorName, String targetName, String protectionName, String role, boolean added) {
        return ChatColor.YELLOW + actorName + " が " + targetName + " を " + ChatColor.WHITE + protectionName
                + ChatColor.YELLOW + " の" + role + (added ? "に追加しました。" : "から削除しました。");
    }
}