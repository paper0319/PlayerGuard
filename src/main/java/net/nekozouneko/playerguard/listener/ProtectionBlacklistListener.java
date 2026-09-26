package net.nekozouneko.playerguard.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.region.ProtectionAccess;
import net.nekozouneko.playerguard.region.RegionBlacklist;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * 保護のブラックリストに入ったプレイヤーの建築・操作を拒否する。
 * メンバーであってもブラックリストが優先される。侵入拒否は {@link DeniedEntryListener} が担当する。
 */
public class ProtectionBlacklistListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        deny(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        deny(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        deny(event.getPlayer(), event.getClickedBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        deny(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        deny(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        deny(event.getPlayer(), event.getRightClicked().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        deny(player, event.getEntity().getLocation(), event);
    }

    private void deny(Player player, Location location, Cancellable event) {
        if (!PGConfig.isBlacklistEnabled()) return;
        if (location == null || location.getWorld() == null) return;
        if (ProtectionAccess.isAdmin(player)) return;
        ProtectedRegion region = blacklistedRegionAt(location.getWorld(), location, player);
        if (region == null) return;
        event.setCancelled(true);
        player.sendMessage(PGMessages.error("ブラックリストに入っているため、この操作はできません。"));
    }

    private static ProtectedRegion blacklistedRegionAt(World world, Location location, Player player) {
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));
        if (rm == null) return null;

        ApplicableRegionSet set = rm.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        ProtectedRegion best = null;
        for (ProtectedRegion region : set) {
            if (region instanceof GlobalProtectedRegion) continue;
            if (!StateFlag.test(region.getFlag(PlayerGuard.getGuardRegisteredFlag()))) continue;
            if (!RegionBlacklist.contains(region, player.getUniqueId())) continue;
            if (best == null || region.volume() < best.volume()) best = region;
        }
        return best;
    }
}
