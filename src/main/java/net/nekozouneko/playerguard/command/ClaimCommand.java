package net.nekozouneko.playerguard.command;

import net.nekozouneko.playerguard.PlayerGuard;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ClaimCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ実行できます。");
            return true;
        }
        Player player = (Player) sender;
        if (!PlayerGuard.getInstance().getCreationConfirmationManager().hasPending(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "確認待ちの土地保護がありません。");
            player.sendMessage(ChatColor.GRAY + "金の斧で保護範囲を選択してください。");
            return true;
        }
        return PlayerGuard.getInstance().getCreationConfirmationManager().confirm(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
