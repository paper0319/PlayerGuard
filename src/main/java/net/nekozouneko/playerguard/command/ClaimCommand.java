package net.nekozouneko.playerguard.command;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.flag.GuardFlags;
import net.nekozouneko.playerguard.region.RegionRoles;
import net.nekozouneko.playerguard.selection.SelectionStorage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class ClaimCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PGMessages.error("このコマンドはプレイヤーのみ実行できます。"));
            return true;
        }

        SelectionStorage ss = PlayerGuard.getInstance().getSelectionStorage();
        Player p = (Player) sender;

        CuboidRegion cr = ss.getSelection(p.getUniqueId());

        if (cr == null) {
            sender.sendMessage(PGMessages.warn("先に金の斧で保護する範囲を選択してください。"));
            return true;
        }

        RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager rm = rc.get(BukkitAdapter.adapt(p.getWorld()));

        long used = PlayerGuard.getInstance().getProtectionUsed(p);
        long limit = PlayerGuard.getInstance().getProtectLimit(p);

        if (36 > cr.getVolume()) {
            sender.sendMessage(PGMessages.error("保護領域の最小サイズは36ブロックです。現在サイズ: %s", PGMessages.highlight(cr.getVolume())));
            return true;
        }

        if (limit <= used + cr.getVolume() && limit != -1) {
            ss.clear(p.getUniqueId());
            p.sendMessage(PGMessages.error(
                    "保護上限を超えています。使用量: %s / 追加分: %s / 上限: %s",
                    PGMessages.highlight(used + cr.getVolume()),
                    PGMessages.highlight(cr.getVolume()),
                    PGMessages.highlight(limit)
            ));
            return true;
        }

        Set<String> allRegionIds = new HashSet<>();
        rc.getLoaded().forEach(rm2 -> allRegionIds.addAll(rm2.getRegions().keySet()));

        String id = Integer.toHexString(new Random().nextInt(0x10000000));
        int timeout = 30;
        boolean timedout = false;
        while (allRegionIds.contains(id)) {
            id = Integer.toHexString(new Random().nextInt(0x10000000));
            timeout--;

            if (timeout <= 0) {
                timedout = true;
                break;
            }
        }

        if (timedout) {
            sender.sendMessage(PGMessages.error("領域IDを生成できませんでした。もう一度試してください。"));
            return true;
        }

        ProtectedRegion protect = new ProtectedCuboidRegion(id, cr.getPos1(), cr.getPos2());
        protect.getOwners().addPlayer(p.getUniqueId());
        RegionRoles.setPrimaryOwner(protect, p.getUniqueId());
        GuardFlags.initRegionFlags(protect);
        protect.setFlag(PlayerGuard.getGuardRegisteredFlag(), StateFlag.State.ALLOW);

        ApplicableRegionSet ars = rm.getApplicableRegions(protect);
        long count = ars.getRegions().stream()
                .filter(pr -> !(pr instanceof GlobalProtectedRegion))
                .count();

        if (count > 0 || ars.testState(WorldGuardPlugin.inst().wrapPlayer(p), PlayerGuard.getGuardIgnoredFlag())) {
            sender.sendMessage(PGMessages.error("ほかの保護領域と重なっています。"));
            return true;
        }

        long minDistance = Long.MAX_VALUE;
        for (ProtectedRegion otherRegion : rm.getRegions().values()) {
            if (otherRegion.getFlag(PlayerGuard.getGuardRegisteredFlag()) != StateFlag.State.ALLOW) continue;
            if (!PGConfig.doApplyToSamePlayerSRegion() && otherRegion.getOwners().contains(p.getUniqueId())) continue;

            long d = PGUtil.distanceBetweenRegions(protect, otherRegion);
            if (d < 0) continue;
            minDistance = Math.min(minDistance, d);
        }

        if (minDistance <= PGConfig.getMinSpacingBetweenRegions()) {
            sender.sendMessage(PGMessages.error("ほかの領域との距離が近すぎます。最短距離: %s", PGMessages.highlight(minDistance)));
            return true;
        }

        final String regionId = id;
        PlayerGuard.getInstance().getScheduler().runGlobal(() -> {
            rm.addRegion(protect);
            ss.clear(p.getUniqueId());

            Object remaining = limit == -1 ? "無制限" : (limit - (used + protect.volume()));
            p.sendMessage(PGMessages.success(
                    "保護領域 %s を作成しました。残り保護量: %s ブロック",
                    PGMessages.highlight(regionId),
                    PGMessages.highlight(remaining)
            ));
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
