package net.nekozouneko.playerguard.region;

/**
 * 進入拒否された保護の外側へ退避する座標を、Bukkit に依存せず計算する。
 * エリトラ等の高速移動では WorldGuard の空中引き戻しではなく、当たった面の外側を使う。
 */
public final class DeniedEntryRelocation {

    public static final int EXTERIOR_OFFSET = 2;

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
        boolean denied(int x, int y, int z);
    }

    enum Face { NONE, MIN_X, MAX_X, MIN_Y, MAX_Y, MIN_Z, MAX_Z }

    private DeniedEntryRelocation() {}

    public static BlockPos exteriorAnchor(Bounds bounds,
                                          double fromX, double fromY, double fromZ,
                                          double toX, double toY, double toZ) {
        Hit hit = firstHit(bounds, fromX, fromY, fromZ, toX, toY, toZ);
        Face face;
        double hx;
        double hz;
        if (hit == null) {
            face = nearestHorizontalFace(bounds, fromX, fromZ);
            hx = fromX;
            hz = fromZ;
        } else {
            face = hit.face;
            hx = hit.x;
            hz = hit.z;
            if (face == Face.MIN_Y || face == Face.MAX_Y) {
                face = nearestHorizontalFace(bounds, hx, hz);
            }
        }
        return offsetFromHit(bounds, face, hx, fromY, hz);
    }

    public static BlockPos findSafe(StandingSpace space, BlockPos anchor, int radius, int searchDown) {
        if (space == null || anchor == null) return null;
        int radiusClamped = Math.max(0, radius);
        int startY = clamp(anchor.y() + 4, space.minY() + 1, space.maxY() - 2);
        int endY = Math.max(space.minY() + 1, Math.min(anchor.y(), startY) - Math.max(0, searchDown));
        BlockPos found = searchColumn(space, anchor.x(), anchor.z(), startY, endY);
        if (found != null) return found;
        for (int r = 1; r <= radiusClamped; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    found = searchColumn(space, anchor.x() + dx, anchor.z() + dz, startY, endY);
                    if (found != null) return found;
                }
            }
        }
        return null;
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

    private static BlockPos searchColumn(StandingSpace space, int x, int z, int startY, int endY) {
        for (int y = startY; y >= endY; y--) {
            if (space.denied(x, y, z)) continue;
            if (space.canStand(x, y, z)) return new BlockPos(x, y, z);
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
