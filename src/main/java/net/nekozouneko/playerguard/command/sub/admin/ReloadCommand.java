package net.nekozouneko.playerguard.command.sub.admin;

import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.command.sub.SubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ReloadCommand extends SubCommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, List<String> args) {
        PlayerGuard.getInstance().reload();
        sender.sendMessage("§aPlayerGuard の設定を再読み込みしました。");
        sender.sendMessage("§7確認待ちの土地保護作成は取り消しました。管理画面は閉じています。");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, List<String> args) {
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "playerguard.command.admin.reload";
    }
}
