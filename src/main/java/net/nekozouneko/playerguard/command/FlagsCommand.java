package net.nekozouneko.playerguard.command;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.md_5.bungee.api.ChatColor;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.gui.MenuGUI;
import net.nekozouneko.playerguard.region.ProtectionAccess;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FlagsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"このコマンドはプレイヤーからのみ実行できます。");
            return true;
        }

        Player p = (Player) sender;

        ProtectedRegion pr = PGUtil.getCurrentPositionRegion(p);

        if (pr == null || !ProtectionAccess.canManageFlags(p, pr)) {
            sender.sendMessage(ChatColor.DARK_RED+"■ "+ChatColor.RED+"ここにはあなたが管理できる保護領域がありません。");
            return true;
        }

        new MenuGUI(PlayerGuard.getInstance(), p, pr).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
