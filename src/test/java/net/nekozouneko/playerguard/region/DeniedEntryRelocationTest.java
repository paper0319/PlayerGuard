package net.nekozouneko.playerguard.region;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeniedEntryRelocationTest {

    private static final DeniedEntryRelocation.Bounds BOX =
            new DeniedEntryRelocation.Bounds(0, 0, 0, 9, 9, 9);

    @Test
    void ejectsWestWhenEnteringFromWest() {
        DeniedEntryRelocation.BlockPos pos = DeniedEntryRelocation.exteriorAnchor(
                BOX, -5.2, 5.0, 5.4, 1.1, 5.0, 5.4);
        assertEquals(new DeniedEntryRelocation.BlockPos(-2, 5, 5), pos);
    }

    @Test
    void ejectsEastWhenEnteringFromEast() {
        DeniedEntryRelocation.BlockPos pos = DeniedEntryRelocation.exteriorAnchor(
                BOX, 14.0, 5.0, 5.4, 8.2, 5.0, 5.4);
        assertEquals(new DeniedEntryRelocation.BlockPos(11, 5, 5), pos);
    }

    @Test
    void ejectsNorthWhenEnteringFromNorth() {
        DeniedEntryRelocation.BlockPos pos = DeniedEntryRelocation.exteriorAnchor(
                BOX, 5.4, 5.0, -4.0, 5.4, 5.0, 1.2);
        assertEquals(new DeniedEntryRelocation.BlockPos(5, 5, -2), pos);
    }

    @Test
    void ejectsSouthWhenEnteringFromSouth() {
        DeniedEntryRelocation.BlockPos pos = DeniedEntryRelocation.exteriorAnchor(
                BOX, 5.4, 5.0, 14.0, 5.4, 5.0, 8.2);
        assertEquals(new DeniedEntryRelocation.BlockPos(5, 5, 11), pos);
    }

    @Test
    void usesTheHitFaceWhenElytraCrossesFromFarAway() {
        DeniedEntryRelocation.BlockPos pos = DeniedEntryRelocation.exteriorAnchor(
                BOX, -50.0, 80.0, 5.3, 3.0, 80.0, 5.3);
        assertEquals(new DeniedEntryRelocation.BlockPos(-2, 80, 5), pos);
    }

    @Test
    void ejectsHorizontallyWhenEnteringFromAbove() {
        DeniedEntryRelocation.BlockPos pos = DeniedEntryRelocation.exteriorAnchor(
                BOX, 5.4, 20.0, 5.4, 5.4, 8.0, 5.4);
        assertTrue(pos.y() == 20);
        assertTrue(!BOX.contains(pos.x(), 5, pos.z()),
                "horizontal eject must land outside the region footprint");
    }

    @Test
    void ejectsThroughNearestWallWhenAlreadyInside() {
        DeniedEntryRelocation.BlockPos pos = DeniedEntryRelocation.exteriorAnchor(
                BOX, 1.2, 5.0, 5.0, 1.4, 5.0, 5.0);
        assertEquals(new DeniedEntryRelocation.BlockPos(-2, 5, 5), pos);
    }

    @Test
    void findSafePrefersTheAnchorColumn() {
        HeightMap space = HeightMap.flat(10);
        DeniedEntryRelocation.BlockPos found = DeniedEntryRelocation.findSafe(
                space, new DeniedEntryRelocation.BlockPos(-2, 80, 5), 8, 80);
        assertEquals(new DeniedEntryRelocation.BlockPos(-2, 10, 5), found);
    }

    @Test
    void findSafeSkipsDeniedColumnsThenUsesSpiral() {
        HeightMap space = HeightMap.flat(4);
        space.denyColumn(-2, 5);
        DeniedEntryRelocation.BlockPos found = DeniedEntryRelocation.findSafe(
                space, new DeniedEntryRelocation.BlockPos(-2, 20, 5), 8, 30);
        assertNotNull(found);
        assertTrue(found.x() != -2 || found.z() != 5);
        assertEquals(4, found.y());
        assertTrue(Math.abs(found.x() + 2) <= 1 && Math.abs(found.z() - 5) <= 1);
    }

    @Test
    void findSafeReturnsNullWhenNoStandableBlockExists() {
        HeightMap space = HeightMap.empty();
        assertNull(DeniedEntryRelocation.findSafe(
                space, new DeniedEntryRelocation.BlockPos(0, 20, 0), 2, 10));
    }

    @Test
    void walkingOnGroundOutsideDoesNotNeedForcedTeleport() {
        assertFalse(DeniedEntryRelocation.needsForcedTeleport(false, false, false, false, true, 0f, false));
    }

    @Test
    void jumpElytraAndBeingInsideNeedForcedTeleport() {
        assertTrue(DeniedEntryRelocation.needsForcedTeleport(false, false, false, false, false, 0f, false), "jump / off-ground");
        assertTrue(DeniedEntryRelocation.needsForcedTeleport(true, false, false, false, false, 0f, false), "elytra");
        assertTrue(DeniedEntryRelocation.needsForcedTeleport(false, true, false, false, true, 0f, false), "creative flight");
        assertTrue(DeniedEntryRelocation.needsForcedTeleport(false, false, true, false, false, 0f, false), "riptide");
        assertTrue(DeniedEntryRelocation.needsForcedTeleport(false, false, false, true, true, 0f, false), "vehicle");
        assertTrue(DeniedEntryRelocation.needsForcedTeleport(false, false, false, false, true, 2.1f, false), "long fall");
        assertTrue(DeniedEntryRelocation.needsForcedTeleport(false, false, false, false, true, 0f, true), "already inside");
    }

    @Test
    void yawFacesAwayFromRegionCenter() {
        float yaw = DeniedEntryRelocation.yawAway(BOX, -2.5, 5.0);
        // looking west, away from center x=5
        assertEquals(90.0f, yaw, 5.0f);
    }

    private static final class HeightMap implements DeniedEntryRelocation.StandingSpace {
        private final int standY;
        private final Set<Long> deniedColumns = new HashSet<>();

        private HeightMap(int standY) {
            this.standY = standY;
        }

        static HeightMap flat(int standY) {
            return new HeightMap(standY);
        }

        static HeightMap empty() {
            return new HeightMap(Integer.MIN_VALUE);
        }

        void denyColumn(int x, int z) {
            deniedColumns.add(pack(x, z));
        }

        @Override public int minY() { return -64; }
        @Override public int maxY() { return 320; }

        @Override
        public boolean canStand(int x, int y, int z) {
            return standY != Integer.MIN_VALUE && y == standY;
        }

        @Override
        public boolean denied(int x, int y, int z) {
            return deniedColumns.contains(pack(x, z));
        }

        private static long pack(int x, int z) {
            return (((long) x) << 32) ^ (z & 0xffffffffL);
        }
    }
}
