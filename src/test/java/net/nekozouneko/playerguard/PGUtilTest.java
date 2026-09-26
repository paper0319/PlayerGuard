package net.nekozouneko.playerguard;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.flag.PGCustomFlags;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PGUtilTest {

    @Test
    void primaryOwnedVolumeIgnoresMemberAndSubOwnerLand() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID member = UUID.randomUUID();

        ProtectedRegion owned = new ProtectedCuboidRegion("owned", BlockVector3.at(0, 0, 0), BlockVector3.at(9, 9, 9));
        owned.getOwners().addPlayer(owner);
        owned.setFlag(PGCustomFlags.PRIMARY_OWNER, owner.toString());

        ProtectedRegion joined = new ProtectedCuboidRegion("joined", BlockVector3.at(0, 0, 0), BlockVector3.at(19, 19, 19));
        joined.getOwners().addPlayer(other);
        joined.getMembers().addPlayer(member);
        joined.setFlag(PGCustomFlags.PRIMARY_OWNER, other.toString());

        ProtectedRegion subOwned = new ProtectedCuboidRegion("sub", BlockVector3.at(0, 0, 0), BlockVector3.at(4, 4, 4));
        subOwned.getOwners().addPlayer(other);
        subOwned.getOwners().addPlayer(owner);
        subOwned.setFlag(PGCustomFlags.PRIMARY_OWNER, other.toString());

        List<ProtectedRegion> regions = List.of(owned, joined, subOwned);
        assertEquals(owned.volume(), PGUtil.primaryOwnedVolume(regions, owner));
        assertEquals(joined.volume() + subOwned.volume(), PGUtil.primaryOwnedVolume(regions, other));
        assertEquals(0L, PGUtil.primaryOwnedVolume(regions, member));
    }
}
