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
import net.nekozouneko.playerguard.scheduler.PGScheduler;
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
 * そのため退避は同じ tick 内のテレポートで行い、落下を途中で止めない。
 */
public class DeniedEntryListener implements Listener {

    private static final long TITLE_COOLDOWN_MS = 1500L;
    /**
     * 退避のクールダウン。落下中は毎 tick 進入判定が走るので、
     * 同じ退避が重複して要求されるのを防ぐ。
     */
    private static final long EJECT_COOLDOWN_MS = 200L;
    private static final double CLOSE_DISTANCE_SQUARED = 64.0;
    private static final Vector ZERO = new Vector();

    private final Map<UUID, Long> lastTitleAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> ejectRequestedAt = new ConcurrentHashMap<>();

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

        if (!forceTeleport) {
            event.setTo(from);
            ejectRequestedAt.remove(player.getUniqueId());
            showDeniedTitle(player);
            return;
        }

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long requestedAt = ejectRequestedAt.get(id);
        if (requestedAt != null && now - requestedAt < EJECT_COOLDOWN_MS) {
            // 直前の退避がまだ処理中なので、重ねてテレポートしない。
            event.setTo(from);
            showDeniedTitle(player);
            return;
        }
        ejectRequestedAt.put(id, now);

        // 退避前に落とすのは乗り物・滑翔・飛行だけ。落下速度は TP 確定後に止める。
        releaseMotion(player);

        Ejection ejection = destination(player, denied, from, to);
        if (ejection == null) {
            ejectRequestedAt.remove(id);
            event.setTo(from);
            showDeniedTitle(player);
            return;
        }
        eject(event, player, ejection.location(), from, ejection.standable());
        showDeniedTitle(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastTitleAt.remove(id);
        ejectRequestedAt.remove(id);
    }

    /**
     * 退避先に移動する。
     * 移動イベントはエンティティの所有スレッドで発火するため、通常は同期テレポートで同じ tick に着地する。
     * 非同期になるのは退避先が別リージョンにある場合だけで、その間だけ保護の外で留まる。
     * そこで落下速度を止めると、アンチチートに飛行と判定される。
     */
    private void eject(PlayerMoveEvent event, Player player, Location dest, Location from, boolean standable) {
        PGScheduler scheduler = scheduler();
        if (scheduler == null || scheduler.isOwnedByCurrentRegion(dest)) {
            // 同期テレポートでは setTo をしてはいけない。
            // CraftBukkit はイベント後に event.getTo() の位置へ呼び戻すため、
            // setTo を呼ぶとここで決めた位置が上書きされる。
            settle(player, player.teleport(dest), standable);
            return;
        }
        // 別リージョンへの移動は非同期になる。その間だけ位置を固定し、保護内へ入れさせない。
        if (from.distanceSquared(dest) <= CLOSE_DISTANCE_SQUARED) {
            event.setTo(dest);
        } else {
            // 遠い場合は向きだけ先に反映し、位置ズレ（=飛行判定）を抑える。
            Location thisTick = from.clone();
            thisTick.setYaw(dest.getYaw());
            thisTick.setPitch(dest.getPitch());
            event.setTo(thisTick);
        }
        player.teleportAsync(dest).thenAccept(ok -> settle(player, ok, standable));
    }

    /**
     * テレポートの成否にかかわらず状態を解放する。
     * 失敗（他プラグインにキャンセルされた等）でもクールダウンを解放するので、
     * 保護内に留まったまま再試行できなくなることはない。
     */
    private void settle(Player player, boolean teleported, boolean standable) {
        ejectRequestedAt.remove(player.getUniqueId());
        if (!teleported) return;
        PGScheduler scheduler = scheduler();
        if (scheduler == null) return;
        if (standable) {
            // 位置が移ったあとで運動状態を止める。先に止めると空中で静止し飛行判定される。
            scheduler.runOnEntity(player, () -> stopMotion(player));
        } else {
            // 足場が見つからず空中に退避した場合は落下を止めない。止めると空中で静止する。
            scheduler.runOnEntity(player, () -> releaseMotion(player));
        }
    }

    private static PGScheduler scheduler() {
        PlayerGuard plugin = PlayerGuard.getInstance();
        return plugin == null ? null : plugin.getScheduler();
    }

    private void showDeniedTitle(Player player) {
        Long last = lastTitleAt.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (last != null && now - last < TITLE_COOLDOWN_MS) return;
        lastTitleAt.put(player.getUniqueId(), now);
        player.sendTitle(PGMessages.deniedEntryTitle(), PGMessages.deniedEntrySubtitle(), 5, 40, 10);
    }

    /** 退避直前に解除する運動状態。落下速度は {@link #stopMotion(Player)} で TP 確定後に止める。 */
    private static void releaseMotion(Player player) {
        if (player.isInsideVehicle()) player.leaveVehicle();
        if (player.isGliding()) player.setGliding(false);
        if (player.isFlying()) player.setFlying(false);
    }

    private static void stopMotion(Player player) {
        releaseMotion(player);
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

    /** 退避先と、そこに足場があるかどうか。足場が無い場合は落下を止められない。 */
    private record Ejection(Location location, boolean standable) {}

    private static Ejection destination(Player player, ProtectedRegion region, Location from, Location to) {
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
        return new Ejection(loc, safe != null);
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
