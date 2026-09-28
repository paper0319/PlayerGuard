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
    void relocateSkipsDeniedColumnsThenUsesSpiral() {
        HeightMap space = HeightMap.flat(4);
        space.denyColumn(48, 268);
        DeniedEntryRelocation.Relocation reloc = relocateFalling(space, 48.5, 40.0, 274.5, 48.5, 37.0, 274.5);
        assertTrue(reloc.standable());
        assertEquals(4, reloc.pos().y());
        assertTrue(Math.abs(reloc.pos().x() - 48) <= 1 && Math.abs(reloc.pos().z() - 268) <= 1,
                "the spiral must stay around the anchor column");
    }

    @Test
    void relocatePrefersTheAnchorColumnOverTheSpiral() {
        HeightMap space = HeightMap.flat(4);
        DeniedEntryRelocation.Relocation reloc = relocateFalling(space, 48.5, 40.0, 274.5, 48.5, 37.0, 274.5);
        assertEquals(new DeniedEntryRelocation.BlockPos(48, 4, 268), reloc.pos());
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

    // --- relocate: 退避先は必ず保護の外側かつ通過可能な位置であること ---

    @Test
    void relocateLandsOnGroundJustOutsideTheClaim() {
        HeightMap space = HeightMap.flat(40);
        DeniedEntryRelocation.Relocation reloc = relocateFalling(space, 48.5, 81.0, 274.5, 48.5, 78.0, 274.5);
        assertTrue(reloc.standable(), "flat ground outside must be standable");
        assertEquals(new DeniedEntryRelocation.BlockPos(48, 40, 268), reloc.pos());
    }

    @Test
    void relocateFindsGroundBelowTheConfiguredSearchDepth() {
        // 崖の麓まで 61 ブロック。search-down=48 では届かないが、退避は成立させる。
        HeightMap space = HeightMap.flat(20);
        DeniedEntryRelocation.Relocation reloc = relocateFalling(space, 48.5, 81.0, 274.5, 48.5, 78.0, 274.5);
        assertTrue(reloc.standable());
        assertEquals(20, reloc.pos().y());
    }

    @Test
    void relocatePrefersGroundAboveTheEntryHeight() {
        // 崖の上に立っているプレイヤーが上から侵入したケース。
        HeightMap space = HeightMap.flat(95);
        DeniedEntryRelocation.Relocation reloc = relocateFalling(space, 48.5, 81.0, 274.5, 48.5, 78.0, 274.5);
        assertTrue(reloc.standable(), "the plateau the player fell from must be found");
        assertEquals(95, reloc.pos().y());
    }

    @Test
    void relocateNeverReturnsASpotInsideSolidRock() {
        // 足場も空間も存在しない。実体ブロックへ入れることだけは禁止する。
        HeightMap space = HeightMap.solid();
        DeniedEntryRelocation.Relocation reloc = relocateFalling(space, 48.5, 81.0, 274.5, 48.5, 78.0, 274.5);
        assertNull(reloc, "no space at all means the caller must not teleport");
    }

    @Test
    void relocateReturnsAirWhenOnlyTheGroundIsMissing() {
        // 床が無いが空間はある。落下を止めず、空中へ出す。
        HeightMap space = HeightMap.airOnly();
        DeniedEntryRelocation.Relocation reloc = relocateFalling(space, 48.5, 81.0, 274.5, 48.5, 78.0, 274.5);
        assertNotNull(reloc);
        assertFalse(reloc.standable(), "no ground means the fall must not be stopped");
        assertOutsideClaim(reloc.pos());
        assertTrue(space.passable(reloc.pos().x(), reloc.pos().y(), reloc.pos().z()),
                "the air fallback must still be a passable position");
    }

    @Test
    void relocateFallsBackToAirBesideTheWall() {
        // 保護のすぐ外に山体がある。壁の中へは入れず、隣の空気へ出す。
        HeightMap space = HeightMap.airOnly().solidWithin(48, 268, 1);
        DeniedEntryRelocation.Relocation reloc = relocateFalling(space, 48.5, 81.0, 274.5, 48.5, 78.0, 274.5);
        assertNotNull(reloc);
        assertFalse(reloc.standable());
        assertTrue(space.passable(reloc.pos().x(), reloc.pos().y(), reloc.pos().z()),
                "the player must never be placed inside the wall");
    }

    @Test
    void relocateGivesUpWhenTheClaimIsSurroundedByRock() {
        // 保護の周囲がすべて山体。退避先が無いのでテレポートせず null を返す。
        HeightMap space = HeightMap.flat(40).solidWithin(48, 268, 8);
        assertNull(relocateFalling(space, 48.5, 81.0, 274.5, 48.5, 78.0, 274.5),
                "a claim walled in by rock must not teleport the player at all");
    }

    @Test
    void relocateNeverReturnsASpotInsideTheClaim() {
        HeightMap space = HeightMap.flat(40);
        space.denyColumn(48, 268);
        space.denyColumn(47, 268);
        DeniedEntryRelocation.Relocation reloc = relocateFalling(space, 48.5, 81.0, 274.5, 48.5, 78.0, 274.5);
        assertNotNull(reloc);
        assertOutsideClaim(reloc.pos());
    }

    @Test
    void relocateGoesStraightUpInsteadOfAcrossAHugeClaim() {
        // 1000x1000 の保護の中央。側面まで 500 ブロックも飛ぶのは避けて真上へ出す。
        DeniedEntryRelocation.Bounds huge = new DeniedEntryRelocation.Bounds(-500, 0, -500, 500, 255, 500);
        HeightMap space = HeightMap.flat(256).within(huge);
        DeniedEntryRelocation.Relocation reloc = DeniedEntryRelocation.relocate(
                space, huge, 8, 48, 0.5, 70.0, 0.5, 0.6, 70.0, 0.5);
        assertNotNull(reloc);
        assertEquals(256, reloc.pos().y(), "must land on top of the claim, not 500 blocks sideways");
        assertTrue(Math.abs(reloc.pos().x()) <= 2 && Math.abs(reloc.pos().z()) <= 2);
    }

    @Test
    void relocateReturnsNullWithoutAStandingSpace() {
        assertNull(DeniedEntryRelocation.relocate(null, BOX, 8, 48,
                -5.0, 5.0, 5.0, 1.0, 5.0, 5.0));
    }

    private static final DeniedEntryRelocation.Bounds CLAIM =
            new DeniedEntryRelocation.Bounds(40, 40, 270, 60, 80, 280);

    private static DeniedEntryRelocation.Relocation relocateFalling(HeightMap space,
                                                                     double fx, double fy, double fz,
                                                                     double tx, double ty, double tz) {
        return DeniedEntryRelocation.relocate(space.within(CLAIM), CLAIM, 8, 48, fx, fy, fz, tx, ty, tz);
    }

    private static void assertOutsideClaim(DeniedEntryRelocation.BlockPos pos) {
        assertFalse(CLAIM.contains(pos.x(), pos.y(), pos.z()),
                "relocation target must be outside the claim");
    }

    /**
     * 足場の高さが一定のテスト用空間。
     * {@code standYs} はプレイヤーの足を置く高さ、つまりその Y に立てると判定される値。
     */
    private static final class HeightMap implements DeniedEntryRelocation.StandingSpace {
        private final Set<Integer> standYs;
        private final boolean air;
        private final Set<Long> deniedColumns = new HashSet<>();
        private final Set<Long> solidColumns = new HashSet<>();
        private DeniedEntryRelocation.Bounds region;

        private HeightMap(Set<Integer> standYs, boolean air) {
            this.standYs = standYs;
            this.air = air;
        }

        /** 全世界が standY の高さだけ足場を持つ平坦な地形。 */
        static HeightMap flat(int standY) {
            return new HeightMap(Set.of(standY), true);
        }

        /** 足場が一切ないが空間はある地形。 */
        static HeightMap airOnly() {
            return new HeightMap(Set.of(), true);
        }

        /** どこも固体で埋まっている地形。 */
        static HeightMap solid() {
            return new HeightMap(Set.of(), false);
        }

        void denyColumn(int x, int z) {
            deniedColumns.add(pack(x, z));
        }

        /** 保護の範囲を進入拒否として扱う。 */
        HeightMap within(DeniedEntryRelocation.Bounds region) {
            this.region = region;
            return this;
        }

        /** 指定位置の周囲を岩で囲う。 */
        HeightMap solidWithin(int cx, int cz, int radius) {
            for (int x = cx - radius; x <= cx + radius; x++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    solidColumns.add(pack(x, z));
                }
            }
            return this;
        }

        @Override public int minY() { return -64; }
        @Override public int maxY() { return 320; }

        @Override
        public boolean canStand(int x, int y, int z) {
            return air && standYs.contains(y) && isPassableColumn(x, z);
        }

        @Override
        public boolean passable(int x, int y, int z) {
            return air && isPassableColumn(x, z);
        }

        @Override
        public boolean denied(int x, int y, int z) {
            return (region != null && region.contains(x, y, z)) || deniedColumns.contains(pack(x, z));
        }

        private boolean isPassableColumn(int x, int z) {
            return !solidColumns.contains(pack(x, z));
        }

        private static long pack(int x, int z) {
            return (((long) x) << 32) ^ (z & 0xffffffffL);
        }
    }
}
