package net.nekozouneko.playerguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public final class PGUtil {
    private PGUtil() {}

    public static ProtectedRegion getCurrentPositionRegion(Player player) {
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));

        ApplicableRegionSet ars =
                rm.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));

        return ars.getRegions().stream()
                .filter(region -> !(region instanceof GlobalProtectedRegion))
                .filter(region -> StateFlag.test(region.getFlag(PlayerGuard.getGuardRegisteredFlag())))
                .findFirst().orElse(null);
    }

    public static Map<ProtectedRegion, World> getPlayerRegions(Player player) {
        Map<ProtectedRegion, World> result = new HashMap<>();

        RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
        Bukkit.getWorlds().forEach(world -> {
            RegionManager rm = rc.get(BukkitAdapter.adapt(world));

            rm.getRegions().values().stream()
                    .filter(region -> !(region instanceof GlobalProtectedRegion))
                    .filter(region -> StateFlag.test(region.getFlag(PlayerGuard.getGuardRegisteredFlag())))
                    .filter(region -> region.getOwners().contains(player.getUniqueId()))
                    .forEach(region -> result.put(region, world));
        });

        return result;
    }

    public static Map.Entry<ProtectedRegion, World> findPlayerGuardRegions(String id) {
        ProtectedRegion region = null;
        World world = null;

        RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();

        for (World w : Bukkit.getWorlds()) {
            RegionManager rm = rc.get(BukkitAdapter.adapt(w));

            ProtectedRegion pr = rm.getRegion(id);
            if (pr == null) continue;

            boolean flagState = StateFlag.test(pr.getFlag(PlayerGuard.getGuardRegisteredFlag()));
            boolean notGlobal = !(pr instanceof GlobalProtectedRegion);

            if (flagState && notGlobal) {
                region = pr;
                world = w;
                break;
            }
        }

        if (region == null || world == null) return null;

        return new AbstractMap.SimpleEntry<>(region, world);
    }

    public static StateFlag.State boolToState(Boolean bool) {
        return bool == null ? null : (bool ? StateFlag.State.ALLOW : StateFlag.State.DENY);
    }

    public static long distanceBetweenRegions(ProtectedRegion a, ProtectedRegion b) {
        if (!((a instanceof ProtectedCuboidRegion) && (b instanceof ProtectedCuboidRegion))) return -1;

        long dx = Math.min(
                Math.min(delta(a.getMinimumPoint().x(), b.getMinimumPoint().x()), delta(a.getMaximumPoint().x(), b.getMaximumPoint().x())),
                Math.min(delta(a.getMinimumPoint().x(), b.getMaximumPoint().x()), delta(a.getMaximumPoint().x(), b.getMinimumPoint().x()))
        );
        long dy = Math.min(
                Math.min(delta(a.getMinimumPoint().y(), b.getMinimumPoint().y()), delta(a.getMaximumPoint().y(), b.getMaximumPoint().y())),
                Math.min(delta(a.getMinimumPoint().y(), b.getMaximumPoint().y()), delta(a.getMaximumPoint().y(), b.getMinimumPoint().y()))
        );
        long dz = Math.min(
                Math.min(delta(a.getMinimumPoint().z(), b.getMinimumPoint().z()), delta(a.getMaximumPoint().z(), b.getMaximumPoint().z())),
                Math.min(delta(a.getMinimumPoint().z(), b.getMaximumPoint().z()), delta(a.getMaximumPoint().z(), b.getMinimumPoint().z()))
        );


        return (long) (Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2) + Math.pow(dy, 2)));
    }

    public static long delta(long a, long b) {
        return Math.abs(a - b);
    }

    public static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }
}
