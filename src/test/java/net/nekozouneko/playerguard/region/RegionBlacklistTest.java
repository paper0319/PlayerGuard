package net.nekozouneko.playerguard.region;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.flag.PGCustomFlags;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegionBlacklistTest {

    private ProtectedRegion newRegion() {
        return new ProtectedCuboidRegion("test",
                BlockVector3.at(0, 0, 0), BlockVector3.at(10, 10, 10));
    }

    @Test
    void addsPlayerToBlacklist() {
        ProtectedRegion r = newRegion();
        UUID u = UUID.randomUUID();
        assertEquals(RegionBlacklist.AddResult.ADDED, RegionBlacklist.add(r, u));
        assertTrue(RegionBlacklist.contains(r, u));
        assertEquals(List.of(u), RegionBlacklist.list(r));
    }

    @Test
    void rejectsDuplicateBlacklist() {
        ProtectedRegion r = newRegion();
        UUID u = UUID.randomUUID();
        RegionBlacklist.add(r, u);
        assertEquals(RegionBlacklist.AddResult.ALREADY_BLACKLISTED, RegionBlacklist.add(r, u));
    }

    @Test
    void rejectsOwner() {
        ProtectedRegion r = newRegion();
        UUID owner = UUID.randomUUID();
        r.getOwners().addPlayer(owner);
        assertEquals(RegionBlacklist.AddResult.IS_OWNER, RegionBlacklist.add(r, owner));
        assertFalse(RegionBlacklist.contains(r, owner));
    }

    @Test
    void rejectsNull() {
        assertEquals(RegionBlacklist.AddResult.INVALID, RegionBlacklist.add(newRegion(), null));
        assertEquals(RegionBlacklist.AddResult.INVALID, RegionBlacklist.add(null, UUID.randomUUID()));
    }

    @Test
    void blacklistOverridesMembership() {
        ProtectedRegion r = newRegion();
        UUID u = UUID.randomUUID();
        r.getMembers().addPlayer(u);
        assertEquals(RegionBlacklist.AddResult.ADDED, RegionBlacklist.add(r, u));
        assertTrue(r.getMembers().contains(u), "blacklisting must not strip membership");
        assertTrue(RegionBlacklist.contains(r, u));
    }

    @Test
    void removesBlacklistedPlayer() {
        ProtectedRegion r = newRegion();
        UUID u = UUID.randomUUID();
        RegionBlacklist.add(r, u);
        assertEquals(RegionBlacklist.RemoveResult.REMOVED, RegionBlacklist.remove(r, u));
        assertFalse(RegionBlacklist.contains(r, u));
        assertTrue(RegionBlacklist.isEmpty(r));
    }

    @Test
    void removeMissingReports() {
        assertEquals(RegionBlacklist.RemoveResult.NOT_BLACKLISTED,
                RegionBlacklist.remove(newRegion(), UUID.randomUUID()));
    }

    @Test
    void skipsBrokenEntries() {
        ProtectedRegion r = newRegion();
        UUID u = UUID.randomUUID();
        Set<String> raw = new HashSet<>();
        raw.add("not-a-uuid");
        raw.add(u.toString());
        r.setFlag(PGCustomFlags.BLACKLIST, raw);

        assertTrue(RegionBlacklist.contains(r, u));
        assertEquals(List.of(u), RegionBlacklist.list(r));
        assertEquals(RegionBlacklist.RemoveResult.REMOVED, RegionBlacklist.remove(r, u));
        assertNull(r.getFlag(PGCustomFlags.BLACKLIST), "broken entries are dropped on rewrite");
    }
}
