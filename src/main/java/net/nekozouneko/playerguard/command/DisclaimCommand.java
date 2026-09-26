package net.nekozouneko.playerguard.command;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.playerguard.PGConfig;
import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.command.sub.playerguard.ConfirmCommand;
import net.nekozouneko.playerguard.region.ProtectionAccess;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DisclaimCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PGMessages.error("このコマンドはプレイヤーのみ実行できます。"));
            return true;
        }
        Player p = (Player) sender;
        ProtectedRegion pr;
        World world;

        if (args.length == 0) {
            world = p.getWorld();
            pr = PGUtil.getCurrentPositionRegion(p);
        } else {
            Map.Entry<ProtectedRegion, World> found = PGUtil.findPlayerGuardRegions(args[0]);
            pr = found == null ? null : found.getKey();
            world = found == null ? null : found.getValue();
        }

        if (pr == null || world == null || !pr.getOwners().contains(p.getUniqueId())) {
            sender.sendMessage(PGMessages.error("ここには削除できる保護領域がありません。"));
            return true;
        }

        if (!ProtectionAccess.canDeleteProtection(p, pr)) {
            sender.sendMessage(PGMessages.error(
                    (PGConfig.allowSubownerDisclaim()
                    ? "領域を削除できるのは主オーナーまたはサブオーナーのみです。"
                    : "領域を削除できるのは主オーナーのみです。")
            ));
            return true;
        }

        final ProtectedRegion region = pr;
        final World targetWorld = world;
        ConfirmCommand.addConfirm(p.getUniqueId(), () ->
            PlayerGuard.getInstance().getScheduler().runGlobal(() -> {
                ProtectionDeletionService.Result result = ProtectionDeletionService.delete(p, region, targetWorld, "command-disclaim");
                if (result == ProtectionDeletionService.Result.DELETED) {
                    sender.sendMessage(PGMessages.success("保護領域 %s を削除しました。", PGMessages.highlight(region.getId())));
                    return;
                }
                if (result == ProtectionDeletionService.Result.REFUND_UNAVAILABLE || result == ProtectionDeletionService.Result.REFUND_FAILED) {
                    sender.sendMessage(PGMessages.error("返金に失敗したため、土地保護を削除しませんでした。"));
                    return;
                }
                sender.sendMessage(PGMessages.error("保護領域を削除できませんでした。"));
            })
        );
        sender.sendMessage(PGMessages.warn("削除を確定するには %s を実行してください。", PGMessages.highlight("/pg yes")));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
