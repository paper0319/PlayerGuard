package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.flag.GuardFlags;
import net.nekozouneko.playerguard.flag.PGCustomFlags;
import net.nekozouneko.playerguard.region.RegionRoles;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Copies selected protection settings. Callers snapshot and restore on failure. */
final class ProtectionSettingsCopy {
    private ProtectionSettingsCopy() {}

    static Map<Flag<?>, Object> copy(ProtectedRegion source, ProtectedRegion target, EnumSet<ProtectionCopyOptionsGUI.Option> options) {
        Map<Flag<?>, Object> previousFlags = new LinkedHashMap<>();
        try {
            if (options.contains(ProtectionCopyOptionsGUI.Option.FLAGS) || options.contains(ProtectionCopyOptionsGUI.Option.PERMISSIONS)) {
                for (GuardFlags gf : GuardFlags.values()) {
                    boolean take = (options.contains(ProtectionCopyOptionsGUI.Option.PERMISSIONS) && gf.getGuiRow() == 1)
                            || (options.contains(ProtectionCopyOptionsGUI.Option.FLAGS) && gf.getGuiRow() == 2);
                    if (!take) continue;
                    for (StateFlag flag : gf.getFlags()) {
                        snapshotAndCopy(source, target, flag, previousFlags);
                        snapshotAndCopy(source, target, flag.getRegionGroupFlag(), previousFlags);
                    }
                }
            }
            if (options.contains(ProtectionCopyOptionsGUI.Option.MEMBER_ROLES)) copyMemberRoles(source, target);
            return previousFlags;
        } catch (RuntimeException ex) {
            restoreFlags(target, previousFlags);
            throw ex;
        }
    }

    static void copyMemberRoles(ProtectedRegion source, ProtectedRegion target) {
        UUID targetPrimary = RegionRoles.getPrimaryOwner(target);
        UUID sourcePrimary = RegionRoles.getPrimaryOwner(source);

        for (UUID uuid : new HashSet<>(target.getMembers().getUniqueIds())) {
            target.getMembers().removePlayer(uuid);
        }
        for (UUID uuid : new HashSet<>(target.getOwners().getUniqueIds())) {
            if (targetPrimary != null && targetPrimary.equals(uuid)) continue;
            target.getOwners().removePlayer(uuid);
        }

        for (UUID uuid : source.getOwners().getUniqueIds()) {
            if (sourcePrimary != null && sourcePrimary.equals(uuid)) continue;
            if (uuid.equals(targetPrimary)) continue;
            target.getOwners().addPlayer(uuid);
        }
        for (UUID uuid : source.getMembers().getUniqueIds()) {
            if (uuid.equals(targetPrimary)) continue;
            if (target.getOwners().contains(uuid)) continue;
            target.getMembers().addPlayer(uuid);
        }

        Set<String> rentals = source.getFlag(PGCustomFlags.RENTALS);
        target.setFlag(PGCustomFlags.RENTALS, rentals == null ? null : new HashSet<>(rentals));
    }

    static void restoreFlags(ProtectedRegion target, Map<Flag<?>, Object> previous) {
        if (previous == null) return;
        for (Map.Entry<Flag<?>, Object> entry : previous.entrySet()) {
            restoreFlag(target, entry.getKey(), entry.getValue());
        }
    }

    static void restoreMemberRoles(ProtectedRegion target, Set<UUID> owners, Set<UUID> members, Set<String> rentals, UUID primaryOwner) {
        for (UUID uuid : new HashSet<>(target.getOwners().getUniqueIds())) {
            target.getOwners().removePlayer(uuid);
        }
        for (UUID uuid : new HashSet<>(target.getMembers().getUniqueIds())) {
            target.getMembers().removePlayer(uuid);
        }
        if (owners != null) {
            for (UUID uuid : owners) target.getOwners().addPlayer(uuid);
        }
        if (members != null) {
            for (UUID uuid : members) target.getMembers().addPlayer(uuid);
        }
        if (primaryOwner != null) RegionRoles.setPrimaryOwner(target, primaryOwner);
        target.setFlag(PGCustomFlags.RENTALS, rentals == null ? null : new HashSet<>(rentals));
    }

    private static <T> void snapshotAndCopy(ProtectedRegion from, ProtectedRegion to, Flag<T> flag, Map<Flag<?>, Object> previous) {
        if (flag == null) return;
        previous.putIfAbsent(flag, to.getFlag(flag));
        to.setFlag(flag, from.getFlag(flag));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void restoreFlag(ProtectedRegion region, Flag flag, Object value) {
        region.setFlag(flag, value);
    }
}
