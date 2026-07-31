package net.nekozouneko.playerguard.command;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.nekozouneko.commons.spigot.command.TabCompletes;
import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.command.sub.SubCommand;
import net.nekozouneko.playerguard.command.sub.SubCommandManager;
import net.nekozouneko.playerguard.command.sub.playerguard.*;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerGuardCommand implements CommandExecutor, TabCompleter {

    private final SubCommandManager manager = new SubCommandManager();

    public PlayerGuardCommand() {
        manager.register("info", new InfoCommand(), "i");
        manager.register("transfer", new TransferCommand(), "give");
        manager.register("add", new AddCommand());
        manager.register("remove", new RemoveCommand(), "rm", "del");
        manager.register("confirm", new ConfirmCommand(), "yes");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                return false;
            }

            Player p = (Player) sender;
            new net.nekozouneko.playerguard.gui.RegionListGUI(p).open();
            return true;
        }

        SubCommand sc = manager.getCommand(args[0]);

        if (sc != null && sender.hasPermission(sc.getPermission())) {
            List<String> args2 = new ArrayList<>(Arrays.asList(args));
            return sc.execute(sender, command, label, args2.subList(1, args2.size()));
        }

        sender.sendMessage(PGMessages.error("権限がないか、そのコマンドは存在しません。"));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return TabCompletes.sorted(args[0], manager.getCommandNamesAndAliases().stream()
                    .filter(name -> {
                        SubCommand sc = manager.getCommand(name);
                        return sc != null && sender.hasPermission(sc.getPermission());
                    })
                    .collect(Collectors.toList())
            );
        }
        else {
            SubCommand sc = manager.getCommand(args[0]);
            if (sc != null && sender.hasPermission(sc.getPermission())) {
                List<String> args2 = new ArrayList<>(Arrays.asList(args));
                return sc.tabComplete(sender, command, label, args2.subList(1, args2.size()));
            }
        }

        return Collections.emptyList();
    }

    private void myInfoCommand(Player player) {
        /*
        ■ あなたの情報 (Taitaitatata547)
        保護制限: u/l
        ■ 保護している領域 (16)
        * abcdef123/world (x, y, z) -> (x, y, z)
         */

        PlayerGuard pg = PlayerGuard.getInstance();

        player.sendMessage(PGMessages.title("あなたの情報 (%s)", player.getName()));
        player.sendMessage(PGMessages.detail(
                "保護制限",
                PGMessages.highlight(pg.getProtectionUsed(player)) + "/" + PGMessages.highlight(pg.getProtectLimit(player))
        ));

        Map<ProtectedRegion, World> protects = PGUtil.getPlayerRegions(player);

        player.sendMessage(PGMessages.title("保護している領域 (%s)", PGMessages.highlight(protects.size())));

        protects.forEach((pr, w) ->
            player.sendMessage(String.format("%s* %s/%s %s(%d, %d, %d) -> (%d, %d, %d)",
                    org.bukkit.ChatColor.WHITE,
                    pr.getId(), w.getName(),
                    org.bukkit.ChatColor.GRAY,
                    pr.getMinimumPoint().x(), pr.getMinimumPoint().y(), pr.getMinimumPoint().z(),
                    pr.getMaximumPoint().x(), pr.getMaximumPoint().y(), pr.getMaximumPoint().z()
            ))
        );

    }

}
