package net.nekozouneko.playerguard.region;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.PGConfig;
import org.bukkit.entity.Player;

/** Centralized protection authority: administrator, owner, sub-owner, member. */
public final class ProtectionAccess {
    private static final String ADMIN_MANAGE = "playerguard.admin.manage";
    private ProtectionAccess() {}
    public static boolean isAdmin(Player actor) { return actor != null && actor.hasPermission(ADMIN_MANAGE); }
    public static boolean canViewProtection(Player actor, ProtectedRegion region) {
        if (actor == null || region == null) return false;
        if (isAdmin(actor)) return true;
        if (RegionBlacklist.contains(region, actor.getUniqueId())) return false;
        return RegionRoles.roleOf(region, actor.getUniqueId()) != RegionRoles.Role.NONE;
    }
    public static boolean canManageProtection(Player actor, ProtectedRegion region) { return actor != null && region != null && (isAdmin(actor) || RegionRoles.isPrimaryOwner(region, actor.getUniqueId())); }
    public static boolean canDeleteProtection(Player actor, ProtectedRegion region) {
        if (canManageProtection(actor, region)) return true;
        return actor != null && region != null
                && RegionRoles.roleOf(region, actor.getUniqueId()) == RegionRoles.Role.SUB_OWNER
                && PGConfig.allowSubownerDisclaim();
    }
    public static boolean canManageMembers(Player actor, ProtectedRegion region) { return actor != null && region != null && (isAdmin(actor) || RegionRoles.isPrimaryOwner(region, actor.getUniqueId()) || RegionRoles.roleOf(region, actor.getUniqueId()) == RegionRoles.Role.SUB_OWNER); }
    public static boolean canManageSubOwners(Player actor, ProtectedRegion region) { return canManageProtection(actor, region); }
    public static boolean canManageFlags(Player actor, ProtectedRegion region) { return canManageProtection(actor, region); }
    public static boolean canUseTeleport(Player actor, ProtectedRegion region) { return canViewProtection(actor, region); }
    public static boolean canManageTeleport(Player actor, ProtectedRegion region) { return canManageProtection(actor, region); }
    public static boolean canViewLogs(Player actor, ProtectedRegion region) {
        if (!canViewProtection(actor, region)) return false;
        if (isAdmin(actor) || RegionRoles.isPrimaryOwner(region, actor.getUniqueId())) return true;
        RegionRoles.Role role = RegionRoles.roleOf(region, actor.getUniqueId());
        if (role == RegionRoles.Role.SUB_OWNER) return PGConfig.allowSubownerViewVisitorLog();
        if (role == RegionRoles.Role.BUILDER) return PGConfig.allowBuilderViewVisitorLog();
        return false;
    }
}