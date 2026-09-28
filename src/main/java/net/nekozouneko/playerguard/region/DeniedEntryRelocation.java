package net.nekozouneko.playerguard.region;

/**
 * 進入拒否された保護の外側へ退避する座標を、Bukkit に依存せず計算する。
 * エリトラ等の高速移動では WorldGuard の空中引き戻しではなく、当たった面の外側を使う。
 *
 * <p>返るのは必ず「保護の外側かつ通過できる」位置だけ。
 * 足場が見つからなくても空間の有無を確かめてから諦める。
 * 実体ブロックや保護の中へ退避させるとプレイヤーが詰まり、
 * 次の移動で同じ場所へ戻され続けて脱出できなくなるため。
 */
public final class DeniedEntryRelocation {

    public static final int EXTERIOR_OFFSET = 2;
    /** アンカー直下を何倍まで深く足場探しに行くか。崖の麓まで届かない場合の保険。 */
    private static final int DEEP_DOWN_FACTOR = 4;
    /** アンカー位置より足場を探す高さ。 */
    private static final int UP_NEAR = 4;
    private static final int UP_FAR = 48;
    /** 足場なし・真上への退避で用いる探索半径。 */
    private static final int NARROW_RADIUS = 2;

    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }

    public record BlockPos(int x, int y, int z) {}

    public interface StandingSpace {
        int minY();
        int maxY();
        boolean canStand(int x, int y, int z);
        /** 足場を要せず、固体でも他の進入拒否保護でもない空間か。 */
        boolean passable(int x, int y, int z);
        boolean denied(int x, int y, int z);
    }

    /** 退避先と、そこに足場があるかどうか。足場が無い場合は落下を止められない。 */
    public record Relocation(BlockPos pos, boolean standable) {}

    enum Face { NONE, MIN_X, MAX_X, MIN_Y, MAX_Y, MIN_Z, MAX_Z }

    private DeniedEntryRelocation() {}

    public static BlockPos exteriorAnchor(Bounds bounds,
                                          double fromX, double fromY, double fromZ,
                                          double toX, double toY, double toZ) {
        return anchor(bounds, firstHit(bounds, fromX, fromY, fromZ, toX, toY, toZ),
                fromX, fromY, fromZ);
    }

    /**
     * 保護の外側へ退避する先を決める。
     * 当たった面の外側から足場を探し、上にも足場があれば上へ出す。
     * 足場が無くても空間の有る場所へ退避し、どこにも空間が無いときだけ null を返す。
     * null のときは呼び出し側でテレポートを諦める。
     */
    public static Relocation relocate(StandingSpace space, Bounds bounds, int radius, int searchDown,
                                      double fromX, double fromY, double fromZ,
                                      double toX, double toY, double toZ) {
        if (space == null || bounds == null) return null;

        Hit hit = firstHit(bounds, fromX, fromY, fromZ, toX, toY, toZ);
        BlockPos anchor = anchor(bounds, hit, fromX, fromY, fromZ);
        boolean alreadyInside = bounds.contains(floor(fromX), floor(fromY), floor(fromZ));

        int hint = boundedY(space, floor(fromY));
        int wide = Math.max(0, radius);
        int down = Math.max(0, searchDown);
        int narrow = Math.min(wide, NARROW_RADIUS);
        int over = boundedY(space, bounds.maxY() + 1);

        // 足場。従来どおりまずアンカー直下から下へ、次に周囲へ。
        BlockPos found = searchRing(space, anchor.x(), anchor.z(), wide,
                hint + UP_NEAR, hint - down, true, false);
        if (found == null) {
            // 上側の地面。崖の上に立つプレイヤーが上から侵入したケース。
            found = searchRing(space, anchor.x(), anchor.z(), wide,
                    hint + UP_FAR, hint + 1, true, true);
        }
        if (found == null) {
            // アンカー直下の崖を深く探す。1 列分だけなので探索量は小さい。
            found = searchRing(space, anchor.x(), anchor.z(), 0,
                    hint - down - 1, hint - down * DEEP_DOWN_FACTOR, true, false);
        }
        if (found == null && alreadyInside) {
            // 既に保護の中にいるときは真上も候補にする。
            // 大きな保護の中央から側面まで数百ブロック飛ばされるのを避ける。
            found = searchRing(space, floor(fromX), floor(fromZ), narrow,
                    over + UP_NEAR, over - down, true, false);
        }

        if (found != null) return new Relocation(found, true);

        // 足場が無くても固体の中へは入れない。落下は途中で止めない。
        found = searchRing(space, anchor.x(), anchor.z(), narrow,
                hint + UP_NEAR, hint - down * DEEP_DOWN_FACTOR, false, false);
        if (found == null) {
            found = searchRing(space, anchor.x(), anchor.z(), narrow,
                    hint + UP_FAR, hint + 1, false, true);
        }
        if (found == null && alreadyInside) {
            found = searchRing(space, floor(fromX), floor(fromZ), narrow,
                    over + UP_NEAR, over - down * DEEP_DOWN_FACTOR, false, false);
        }

        return found == null ? null : new Relocation(found, false);
    }

    /**
     * 空中・高速移動、またはすでに保護内にいる場合は地面へ退避する。
     * 徒歩で境界に当たっただけなら、その場で進入を止める。
     */
    public static boolean needsForcedTeleport(boolean gliding, boolean flying, boolean riptiding,
                                              boolean inVehicle, boolean onGround, float fallDistance,
                                              boolean alreadyInside) {
        if (alreadyInside) return true;
        return gliding || flying || riptiding || inVehicle || !onGround || fallDistance > 2.0f;
    }

    public static float yawAway(Bounds bounds, double x, double z) {
        double centerX = (bounds.minX + bounds.maxX + 1) / 2.0;
        double centerZ = (bounds.minZ + bounds.maxZ + 1) / 2.0;
        double dx = x - centerX;
        double dz = z - centerZ;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    static Hit firstHit(Bounds b, double fx, double fy, double fz, double tx, double ty, double tz) {
        if (b.contains(floor(fx), floor(fy), floor(fz))) return null;

        double tMin = 0.0;
        double tMax = 1.0;
        Face enterFace = Face.NONE;

        HitAxis x = clipAxis(fx, tx, b.minX, b.maxX + 1.0, Face.MIN_X, Face.MAX_X, tMin, tMax, enterFace);
        if (x == null) return null;
        tMin = x.tMin;
        tMax = x.tMax;
        enterFace = x.face;

        HitAxis y = clipAxis(fy, ty, b.minY, b.maxY + 1.0, Face.MIN_Y, Face.MAX_Y, tMin, tMax, enterFace);
        if (y == null) return null;
        tMin = y.tMin;
        tMax = y.tMax;
        enterFace = y.face;

        HitAxis z = clipAxis(fz, tz, b.minZ, b.maxZ + 1.0, Face.MIN_Z, Face.MAX_Z, tMin, tMax, enterFace);
        if (z == null) return null;
        tMin = z.tMin;
        enterFace = z.face;

        if (enterFace == Face.NONE) return null;
        return new Hit(enterFace,
                fx + (tx - fx) * tMin,
                fy + (ty - fy) * tMin,
                fz + (tz - fz) * tMin);
    }

    private static HitAxis clipAxis(double from, double to, double min, double max,
                                    Face minFace, Face maxFace,
                                    double tMin, double tMax, Face currentFace) {
        double delta = to - from;
        if (Math.abs(delta) < 1.0e-9) {
            if (from < min || from >= max) return null;
            return new HitAxis(tMin, tMax, currentFace);
        }
        double inv = 1.0 / delta;
        double t0 = (min - from) * inv;
        double t1 = (max - from) * inv;
        Face f0 = minFace;
        Face f1 = maxFace;
        if (t0 > t1) {
            double tmp = t0;
            t0 = t1;
            t1 = tmp;
            Face tmpFace = f0;
            f0 = f1;
            f1 = tmpFace;
        }
        if (t0 > tMin) {
            tMin = t0;
            currentFace = f0;
        }
        tMax = Math.min(tMax, t1);
        if (tMin > tMax) return null;
        return new HitAxis(tMin, tMax, currentFace);
    }

    static Face nearestHorizontalFace(Bounds b, double x, double z) {
        double toMinX = x - b.minX;
        double toMaxX = b.maxX - x;
        double toMinZ = z - b.minZ;
        double toMaxZ = b.maxZ - z;
        Face face = Face.MIN_X;
        double best = toMinX;
        if (toMaxX < best) {
            best = toMaxX;
            face = Face.MAX_X;
        }
        if (toMinZ < best) {
            best = toMinZ;
            face = Face.MIN_Z;
        }
        if (toMaxZ < best) {
            face = Face.MAX_Z;
        }
        return face;
    }

    /** 当たった面（なければ最寄り側面）の外側 2 マス。Y は侵入した高さのまま。 */
    private static BlockPos anchor(Bounds b, Hit hit, double fromX, double fromY, double fromZ) {
        double hx = fromX;
        double hz = fromZ;
        Face face = nearestHorizontalFace(b, fromX, fromZ);
        if (hit != null) {
            hx = hit.x;
            hz = hit.z;
            face = hit.face;
            if (face == Face.MIN_Y || face == Face.MAX_Y) {
                face = nearestHorizontalFace(b, hx, hz);
            }
        }
        return offsetFromHit(b, face, hx, fromY, hz);
    }

    private static BlockPos offsetFromHit(Bounds b, Face face, double hx, double hy, double hz) {
        int y = floor(hy);
        int x = floor(hx);
        int z = floor(hz);
        switch (face) {
            case MAX_X:
                return new BlockPos(b.maxX + EXTERIOR_OFFSET, y, z);
            case MIN_Z:
                return new BlockPos(x, y, b.minZ - EXTERIOR_OFFSET);
            case MAX_Z:
                return new BlockPos(x, y, b.maxZ + EXTERIOR_OFFSET);
            case MIN_X:
            default:
                return new BlockPos(b.minX - EXTERIOR_OFFSET, y, z);
        }
    }

    private static int boundedY(StandingSpace space, int y) {
        return clamp(y, space.minY() + 1, space.maxY() - 2);
    }

    /** アンカー列から {@code radius} マスの角まで外側へ回的しつつ、Y を走査して最初の一致を返す。 */
    private static BlockPos searchRing(StandingSpace space, int cx, int cz, int radius,
                                       int fromY, int toY, boolean needGround, boolean ascending) {
        BlockPos found = searchColumn(space, cx, cz, fromY, toY, needGround, ascending);
        if (found != null) return found;
        for (int r = 1; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    found = searchColumn(space, cx + dx, cz + dz, fromY, toY, needGround, ascending);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private static BlockPos searchColumn(StandingSpace space, int x, int z,
                                         int fromY, int toY, boolean needGround, boolean ascending) {
        int low = boundedY(space, Math.min(fromY, toY));
        int high = boundedY(space, Math.max(fromY, toY));
        int start = ascending ? low : high;
        int end = ascending ? high : low;
        int step = ascending ? 1 : -1;
        for (int y = start; ascending ? y <= end : y >= end; y += step) {
            if (space.denied(x, y, z)) continue;
            if (needGround ? space.canStand(x, y, z) : space.passable(x, y, z)) return new BlockPos(x, y, z);
        }
        return null;
    }

    private static int floor(double v) {
        return (int) Math.floor(v);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    static final class Hit {
        final Face face;
        final double x;
        final double y;
        final double z;

        Hit(Face face, double x, double y, double z) {
            this.face = face;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class HitAxis {
        final double tMin;
        final double tMax;
        final Face face;

        HitAxis(double tMin, double tMax, Face face) {
            this.tMin = tMin;
            this.tMax = tMax;
            this.face = face;
        }
    }
}
