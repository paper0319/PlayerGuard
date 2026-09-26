package net.nekozouneko.playerguard.gui;

import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.flag.PGCustomFlags;
import net.nekozouneko.playerguard.region.RegionRoles;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectionSettingsCopyTest {

    @Test
    void copiesMemberRolesWithoutReplacingPrimaryOwner() {
        UUID sourceOwner = UUID.randomUUID();
        UUID sourceSub = UUID.randomUUID();
        UUID sourceMember = UUID.randomUUID();
        UUID targetOwner = UUID.randomUUID();
        UUID targetSub = UUID.randomUUID();
        UUID targetMember = UUID.randomUUID();

        ProtectedRegion source = region("src");
        source.getOwners().addPlayer(sourceOwner);
        source.getOwners().addPlayer(sourceSub);
        source.getMembers().addPlayer(sourceMember);
        RegionRoles.setPrimaryOwner(source, sourceOwner);

        ProtectedRegion target = region("dst");
        target.getOwners().addPlayer(targetOwner);
        target.getOwners().addPlayer(targetSub);
        target.getMembers().addPlayer(targetMember);
        RegionRoles.setPrimaryOwner(target, targetOwner);

        ProtectionSettingsCopy.copy(source, target, EnumSet.of(ProtectionCopyOptionsGUI.Option.MEMBER_ROLES));

        assertTrue(RegionRoles.isPrimaryOwner(target, targetOwner));
        assertTrue(target.getOwners().contains(sourceSub));
        assertFalse(target.getOwners().contains(sourceOwner));
        assertFalse(target.getOwners().contains(targetSub));
        assertTrue(target.getMembers().contains(sourceMember));
        assertFalse(target.getMembers().contains(targetMember));
    }

    @Test
    void copiesRentalsWithMemberRoles() {
        UUID sourceOwner = UUID.randomUUID();
        UUID rental = UUID.randomUUID();
        UUID targetOwner = UUID.randomUUID();
        ProtectedRegion source = region("src");
        source.getOwners().addPlayer(sourceOwner);
        RegionRoles.setPrimaryOwner(source, sourceOwner);
        source.getMembers().addPlayer(rental);
        source.setFlag(PGCustomFlags.RENTALS, new HashSet<>(Set.of(rental + ":1000")));

        ProtectedRegion target = region("dst");
        target.getOwners().addPlayer(targetOwner);
        RegionRoles.setPrimaryOwner(target, targetOwner);

        ProtectionSettingsCopy.copy(source, target, EnumSet.of(ProtectionCopyOptionsGUI.Option.MEMBER_ROLES));

        assertTrue(target.getMembers().contains(rental));
        assertEquals(Set.of(rental + ":1000"), target.getFlag(PGCustomFlags.RENTALS));
    }

    @Test
    void copiesWorldFlagsWithoutPlayerPermissionFlags() {
        ProtectedRegion source = region("src");
        ProtectedRegion target = region("dst");
        source.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
        target.setFlag(Flags.BLOCK_BREAK, StateFlag.State.ALLOW);
        source.setFlag(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);

        ProtectionSettingsCopy.copy(source, target, EnumSet.of(ProtectionCopyOptionsGUI.Option.FLAGS));

        assertEquals(StateFlag.State.DENY, target.getFlag(Flags.CREEPER_EXPLOSION));
        assertEquals(StateFlag.State.ALLOW, target.getFlag(Flags.BLOCK_BREAK));
    }

    @Test
    void restoreFlagsClearsFlagsThatWereUnset() {
        ProtectedRegion source = region("src");
        ProtectedRegion target = region("dst");
        source.setFlag(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);

        Map<Flag<?>, Object> previous = ProtectionSettingsCopy.copy(source, target, EnumSet.of(ProtectionCopyOptionsGUI.Option.FLAGS));
        assertEquals(StateFlag.State.DENY, target.getFlag(Flags.CREEPER_EXPLOSION));

        ProtectionSettingsCopy.restoreFlags(target, previous);
        assertNull(target.getFlag(Flags.CREEPER_EXPLOSION));
    }

    private static ProtectedRegion region(String id) {
        return new ProtectedCuboidRegion(id, BlockVector3.at(0, 0, 0), BlockVector3.at(3, 3, 3));
    }
}
