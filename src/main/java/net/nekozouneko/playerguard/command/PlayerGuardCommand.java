package net.nekozouneko.playerguard.command;

import net.nekozouneko.commons.spigot.command.TabCompletes;
import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.command.sub.SubCommand;
import net.nekozouneko.playerguard.command.sub.SubCommandManager;
import net.nekozouneko.playerguard.command.sub.playerguard.*;
import net.nekozouneko.playerguard.gui.RegionListGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.stream.Collectors;

public final class PlayerGuardCommand implements CommandExecutor, TabCompleter {
    private final SubCommandManager manager = new SubCommandManager();
    public PlayerGuardCommand() {
        manager.register("info", new InfoCommand(), "i"); manager.register("transfer", new TransferCommand(), "give");
        manager.register("add", new AddCommand()); manager.register("remove", new RemoveCommand(), "rm", "del");
        manager.register("yes", new ConfirmCommand(), "confirm");
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { if (sender instanceof Player player) new RegionListGUI(player).open(); return true; }
        if ("invsee".equalsIgnoreCase(args[0])) return invsee(sender, args);
        SubCommand sub = manager.getCommand(args[0]);
        if (sub != null && sender.hasPermission(sub.getPermission())) return sub.execute(sender, command, label, Arrays.asList(args).subList(1, args.length));
        sender.sendMessage(PGMessages.error("権限がないか、そのコマンドは存在しません。")); return true;
    }
    private boolean invsee(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("playerguard.admin.view")) { player.sendMessage(ChatColor.RED + "他のプレイヤーの保護を閲覧する権限がありません。"); return true; }
        if (args.length != 2) { player.sendMessage(ChatColor.RED + "プレイヤー名を指定してください。"); return true; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.isOnline() && !target.hasPlayedBefore()) { player.sendMessage(ChatColor.RED + "指定したプレイヤーが見つかりません。"); return true; }
        new RegionListGUI(player, target.getUniqueId(), target.getName() == null ? args[1] : target.getName()).open(); return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) { List<String> values = manager.getCommandNamesAndAliases().stream().filter(name -> { SubCommand sub = manager.getCommand(name); return sub != null && sender.hasPermission(sub.getPermission()); }).collect(Collectors.toCollection(ArrayList::new)); if (sender.hasPermission("playerguard.admin.view")) values.add("invsee"); return TabCompletes.sorted(args[0], values); }
        if (args.length == 2 && "invsee".equalsIgnoreCase(args[0]) && sender.hasPermission("playerguard.admin.view")) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
        SubCommand sub = manager.getCommand(args[0]); return sub != null && sender.hasPermission(sub.getPermission()) ? sub.tabComplete(sender, command, label, Arrays.asList(args).subList(1, args.length)) : Collections.emptyList();
    }
}