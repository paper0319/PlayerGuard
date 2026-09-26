package net.nekozouneko.playerguard.region;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.flag.PGCustomFlags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 保護ごとのプレイヤーブラックリストのコアロジック。
 * ブラックリスト入りしたプレイヤーはメンバーであっても侵入・操作を拒否される。
 * UUID文字列の集合として {@link PGCustomFlags#BLACKLIST} に保存する。
 * Bukkit に依存せず単体テスト可能。
 */
public final class RegionBlacklist {

    public enum AddResult { ADDED, ALREADY_BLACKLISTED, IS_OWNER, INVALID }
    public enum RemoveResult { REMOVED, NOT_BLACKLISTED, INVALID }

    private RegionBlacklist() {}

    public static AddResult add(ProtectedRegion region, UUID target) {
        if (region == null || target == null) return AddResult.INVALID;
        if (region.getOwners().contains(target)) return AddResult.IS_OWNER;
        if (contains(region, target)) return AddResult.ALREADY_BLACKLISTED;

        Set<String> entries = new HashSet<>();
        Set<String> current = region.getFlag(PGCustomFlags.BLACKLIST);
        if (current != null) entries.addAll(current);
        entries.add(target.toString());
        region.setFlag(PGCustomFlags.BLACKLIST, entries);
        return AddResult.ADDED;
    }

    public static RemoveResult remove(ProtectedRegion region, UUID target) {
        if (region == null || target == null || !contains(region, target)) return RemoveResult.NOT_BLACKLISTED;

        Set<String> keep = new HashSet<>();
        Set<String> current = region.getFlag(PGCustomFlags.BLACKLIST);
        if (current != null) {
            for (String raw : current) {
                UUID uuid = parse(raw);
                if (uuid == null || uuid.equals(target)) continue;
                keep.add(raw);
            }
        }
        region.setFlag(PGCustomFlags.BLACKLIST, keep.isEmpty() ? null : keep);
        return RemoveResult.REMOVED;
    }

    public static boolean contains(ProtectedRegion region, UUID target) {
        if (region == null || target == null) return false;
        Set<String> current = region.getFlag(PGCustomFlags.BLACKLIST);
        if (current == null) return false;
        for (String raw : current) {
            UUID uuid = parse(raw);
            if (uuid != null && uuid.equals(target)) return true;
        }
        return false;
    }

    /** 登録済みのUUIDを重複なく列挙する。壊れたエントリは読み飛ばす。 */
    public static List<UUID> list(ProtectedRegion region) {
        if (region == null) return Collections.emptyList();
        Set<String> current = region.getFlag(PGCustomFlags.BLACKLIST);
        if (current == null) return Collections.emptyList();

        List<UUID> result = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (String raw : current) {
            UUID uuid = parse(raw);
            if (uuid != null && seen.add(uuid)) result.add(uuid);
        }
        return result;
    }

    public static boolean isEmpty(ProtectedRegion region) {
        return list(region).isEmpty();
    }

    private static UUID parse(String raw) {
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
