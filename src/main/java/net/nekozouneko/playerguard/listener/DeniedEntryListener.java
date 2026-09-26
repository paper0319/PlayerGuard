package net.nekozouneko.playerguard.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.region.DeniedEntryRelocation;
import net.nekozouneko.playerguard.region.RegionBlacklist;
import net.nekozouneko.playerguard.region.RegionRoles;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 進入拒否の保護への入場を止める。
 * 徒歩は境界で止め、ジャンプ・エリトラ等は保護の外側近くへ退避する。
 * 空中引き戻しはアンチチートの飛行判定になりやすい。
 */
public class DeniedEntryListener implements Listener {

    private static final long TITLE_COOLDOWN_MS = 1500L;
    private static final double CLOSE_DISTANCE_SQUARED = 64.0;
    private static final Vector ZERO = new Vector();

    private final Map<UUID, Long> lastTitleAt = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!PGConfig.isDeniedEntryRelocationEnabled()) return;
        Location to = event.getTo();
        if (to == null) return;
        Location from = event.getFrom();
        if (sameBlock(from, to)) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        ProtectedRegion denied = deniedProtectionAt(player, to);
        if (denied == null) return;

        boolean alreadyInside = denied.contains(BukkitAdapter.asBlockVector(from));
        boolean forceTeleport = DeniedEntryRelocation.needsForcedTeleport(
                player.isGliding(), player.isFlying(), player.isRiptiding(),
                player.isInsideVehicle(), player.isOnGround(), player.getFallDistance(),
                alreadyInside);

        if (forceTeleport) {
            stopMotion(player);
            Location dest = destination(player, denied, from, to);
            if (dest == null) {
                event.setTo(from);
            } else {
                Location thisTick;
                if (from.distanceSquared(dest) <= CLOSE_DISTANCE_SQUARED) {
                    thisTick = dest;
                } else {
                    thisTick = from.clone();
                    thisTick.setYaw(dest.getYaw());
                    thisTick.setPitch(dest.getPitch());
                }
                event.setTo(thisTick);
                player.teleportAsync(dest).thenAccept(ok -> {
                    PlayerGuard plugin = PlayerGuard.getInstance();
                    if (plugin == null || plugin.getScheduler() == null) return;
                    plugin.getScheduler().runOnEntity(player, () -> {
                        player.setGliding(false);
                        if (player.isFlying()) player.setFlying(false);
                        player.setVelocity(ZERO);
                        player.setFallDistance(0);
                    });
                });
            }
        } else {
            event.setTo(from);
        }
        showDeniedTitle(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastTitleAt.remove(event.getPlayer().getUniqueId());
    }

    private void showDeniedTitle(Player player) {
        Long last = lastTitleAt.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (last != null && now - last < TITLE_COOLDOWN_MS) return;
        lastTitleAt.put(player.getUniqueId(), now);
        player.sendTitle(PGMessages.deniedEntryTitle(), PGMessages.deniedEntrySubtitle(), 5, 40, 10);
    }

    private static void stopMotion(Player player) {
        if (player.isInsideVehicle()) player.leaveVehicle();
        player.setGliding(false);
        if (player.isFlying()) player.setFlying(false);
        player.setVelocity(ZERO);
        player.setFallDistance(0);
    }

    private static boolean sameBlock(Location from, Location to) {
        return from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ();
    }

    private static ProtectedRegion deniedProtectionAt(Player player, Location location) {
        if (location.getWorld() == null) return null;
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(location.getWorld()));
        if (rm == null) return null;

        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        if (WorldGuard.getInstance().getPlatform().getSessionManager()
                .hasBypass(localPlayer, BukkitAdapter.adapt(location.getWorld()))) {
            return null;
        }

        ApplicableRegionSet set = rm.getApplicableRegions(BukkitAdapter.asBlockVector(location));

        ProtectedRegion best = null;
        for (ProtectedRegion region : set) {
            if (!isDenied(region, player.getUniqueId())) continue;
            if (best == null || region.volume() < best.volume()) best = region;
        }
        return best;
    }

    /** 侵入不可（ENTRY拒否かつ非メンバー、またはブラックリスト入り）の保護か。 */
    private static boolean isDenied(ProtectedRegion region, UUID playerId) {
        if (region instanceof GlobalProtectedRegion) return false;
        if (!StateFlag.test(region.getFlag(PlayerGuard.getGuardRegisteredFlag()))) return false;
        if (RegionBlacklist.contains(region, playerId)) return true;
        if (region.getFlag(Flags.ENTRY) != StateFlag.State.DENY) return false;
        return RegionRoles.roleOf(region, playerId) == RegionRoles.Role.NONE;
    }

    private static Location destination(Player player, ProtectedRegion region, Location from, Location to) {
        World world = from.getWorld();
        if (world == null) return null;

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        DeniedEntryRelocation.Bounds bounds = new DeniedEntryRelocation.Bounds(
                min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
        DeniedEntryRelocation.BlockPos anchor = DeniedEntryRelocation.exteriorAnchor(
                bounds, from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());

        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));
        DeniedEntryRelocation.StandingSpace space = new RegionStandingSpace(world, region, rm, player.getUniqueId());
        DeniedEntryRelocation.BlockPos safe = DeniedEntryRelocation.findSafe(
                space, anchor, PGConfig.getDeniedEntryRelocationSearchRadius(), PGConfig.getDeniedEntryRelocationSearchDown());

        DeniedEntryRelocation.BlockPos dest = safe != null ? safe : anchor;
        float yaw = DeniedEntryRelocation.yawAway(bounds, dest.x() + 0.5, dest.z() + 0.5);
        Location loc = new Location(world, dest.x() + 0.5, dest.y(), dest.z() + 0.5, yaw, 0f);
        if (safe == null) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 80, 0, false, false, false));
        }
        return loc;
    }

    private static final class RegionStandingSpace implements DeniedEntryRelocation.StandingSpace {
        private final World world;
        private final ProtectedRegion region;
        private final RegionManager regionManager;
        private final UUID playerId;

        RegionStandingSpace(World world, ProtectedRegion region, RegionManager regionManager,
                            UUID playerId) {
            this.world = world;
            this.region = region;
            this.regionManager = regionManager;
            this.playerId = playerId;
        }

        @Override public int minY() { return world.getMinHeight(); }
        @Override public int maxY() { return world.getMaxHeight() - 1; }

        @Override
        public boolean canStand(int x, int y, int z) {
            if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 1) return false;
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            Block below = world.getBlockAt(x, y - 1, z);
            if (below.isLiquid() || !below.getType().isSolid() || !feet.isPassable() || !head.isPassable()) return false;
            return !blockedByOtherDeny(x, y, z);
        }

        @Override
        public boolean denied(int x, int y, int z) {
            return region.contains(x, y, z);
        }

        private boolean blockedByOtherDeny(int x, int y, int z) {
            if (regionManager == null) return false;
            ApplicableRegionSet set = regionManager.getApplicableRegions(BlockVector3.at(x, y, z));
            for (ProtectedRegion other : set) {
                if (other.getId().equals(region.getId())) continue;
                if (isDenied(other, playerId)) return true;
            }
            return false;
        }
    }
}
