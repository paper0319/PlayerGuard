package net.nekozouneko.playerguard.region;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.flag.PGCustomFlags;

import java.util.Set;
import java.util.UUID;

/**
 * 領域ロール(主オーナー/サブオーナー/builder)のコアロジック。
 * 主オーナーはカスタムフラグ pg-primary-owner で識別する。
 * Bukkit に依存せず単体テスト可能。
 */
public final class RegionRoles {

    public enum Role { PRIMARY_OWNER, SUB_OWNER, BUILDER, NONE }
    public enum PromoteResult { PROMOTED, NOT_BUILDER, IS_RENTAL, INVALID }
    public enum DemoteResult { DEMOTED, NOT_CO_OWNER, INVALID }
    public enum RemoveRoleResult { REMOVED, NOT_REMOVABLE, INVALID }

    private RegionRoles() {}

    /**
     * 主オーナーUUIDを返す。フラグ未設定・不整合(主オーナーがownersに居ない)の場合は
     * レイジー移行: オーナーがちょうど1人ならその人を主オーナーとして確定する。
     * 複数オーナーで未設定(レガシー)なら null(=全オーナーを主オーナー扱い)。
     */
    public static UUID getPrimaryOwner(ProtectedRegion region) {
        if (region == null) return null;
        String raw = region.getFlag(PGCustomFlags.PRIMARY_OWNER);
        if (raw != null) {
            try {
                UUID u = UUID.fromString(raw);
                if (region.getOwners().contains(u)) return u;
            } catch (IllegalArgumentException ignored) {}
        }
        Set<UUID> owners = region.getOwners().getUniqueIds();
        if (owners.size() == 1) {
            UUID sole = owners.iterator().next();
            region.setFlag(PGCustomFlags.PRIMARY_OWNER, sole.toString());
            return sole;
        }
        return null;
    }

    public static Role roleOf(ProtectedRegion region, UUID uuid) {
        if (region == null || uuid == null) return Role.NONE;
        if (region.getOwners().contains(uuid)) {
            UUID primary = getPrimaryOwner(region);
            return (primary == null || primary.equals(uuid)) ? Role.PRIMARY_OWNER : Role.SUB_OWNER;
        }
        if (region.getMembers().contains(uuid)) return Role.BUILDER;
        return Role.NONE;
    }

    /** 譲渡・領域削除・サブオーナー任免が可能か(=主オーナーか)。レガシー(複数owner・未設定)は全owners可。 */
    public static boolean isPrimaryOwner(ProtectedRegion region, UUID uuid) {
        return roleOf(region, uuid) == Role.PRIMARY_OWNER;
    }

    /** 領域作成時・譲渡成立時に主オーナーを確定する。 */
    public static void setPrimaryOwner(ProtectedRegion region, UUID uuid) {
        if (region == null || uuid == null) return;
        region.setFlag(PGCustomFlags.PRIMARY_OWNER, uuid.toString());
    }

    /** builder を サブオーナー へ昇格(members→owners)。貸出中builderは昇格不可。 */
    public static PromoteResult promote(ProtectedRegion region, UUID target) {
        if (region == null || target == null) return PromoteResult.INVALID;
        if (!region.getMembers().contains(target)) return PromoteResult.NOT_BUILDER;
        if (RegionRentals.isRental(region, target)) return PromoteResult.IS_RENTAL;
        // 昇格でownersが複数になる前に、現在の主オーナーをレイジー移行で確定しておく
        getPrimaryOwner(region);
        region.getMembers().removePlayer(target);
        region.getOwners().addPlayer(target);
        return PromoteResult.PROMOTED;
    }

    /** サブオーナー を builder へ降格(owners→members)。主オーナーは降格不可。 */
    public static DemoteResult demote(ProtectedRegion region, UUID target) {
        if (region == null || target == null) return DemoteResult.INVALID;
        if (roleOf(region, target) != Role.SUB_OWNER) return DemoteResult.NOT_CO_OWNER;
        region.getOwners().removePlayer(target);
        region.getMembers().addPlayer(target);
        return DemoteResult.DEMOTED;
    }

    /** サブオーナー/builder を領域から外す。主オーナー・非メンバーには使えない。 */
    public static RemoveRoleResult removeMember(ProtectedRegion region, UUID target) {
        if (region == null || target == null) return RemoveRoleResult.INVALID;
        switch (roleOf(region, target)) {
            case SUB_OWNER:
                region.getOwners().removePlayer(target);
                return RemoveRoleResult.REMOVED;
            case BUILDER:
                RegionMembers.remove(region, target);
                return RemoveRoleResult.REMOVED;
            default:
                return RemoveRoleResult.NOT_REMOVABLE;
        }
    }
}
