package net.nekozouneko.playerguard.command.sub.playerguard;

import net.nekozouneko.playerguard.PGMessages;
import net.nekozouneko.playerguard.PGUtil;
import net.nekozouneko.playerguard.PlayerGuard;
import net.nekozouneko.playerguard.command.PendingDeletion;
import net.nekozouneko.playerguard.command.ProtectionDeletionService;
import net.nekozouneko.playerguard.command.sub.SubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class ConfirmCommand extends SubCommand {

    private final static Map<UUID, Runnable> confirms = new HashMap<>();
    private final static Map<UUID, Long> timeouts = new HashMap<>();
    private final static Map<UUID, PendingDeletion> pendingDeletions = new HashMap<>();

    public static synchronized void addConfirm(UUID player, Runnable runnable) {
        confirms.put(player, runnable);
        timeouts.put(player, System.currentTimeMillis() + 60000);
    }

    public static synchronized void addDeletionConfirm(PendingDeletion deletion) {
        pendingDeletions.put(deletion.initiatorUuid(), deletion);
    }

    public static synchronized void removeConfirm(UUID player) {
        confirms.remove(player);
        timeouts.remove(player);
        pendingDeletions.remove(player);
    }

    public static synchronized Runnable getConfirm(UUID player) {
        Runnable task = confirms.get(player);
        Long timeout = timeouts.get(player);

        if (timeout == null || System.currentTimeMillis() > timeout) {
            removeConfirm(player);
            return null;
        }

        return task;
    }

    public static synchronized Map<UUID, Runnable> getConfirms() {
        new HashSet<>(confirms.keySet()).forEach(uuid -> {
            Long timeout = timeouts.get(uuid);
            if (timeout == null || System.currentTimeMillis() > timeout) {
                removeConfirm(uuid);
            }
        });

        return new HashMap<>(confirms);
    }

    public static synchronized void clearConfirms() {
        confirms.clear();
        timeouts.clear();
        pendingDeletions.clear();
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, List<String> args) {
        if (!(sender instanceof Player || sender instanceof ConsoleCommandSender)) {
            return true;
        }

        if (sender instanceof Player player) {
            PendingDeletion pending;
            synchronized (ConfirmCommand.class) {
                pending = pendingDeletions.get(player.getUniqueId());
                if (pending != null && pending.ifValidAt(System.currentTimeMillis()) == null) {
                    pendingDeletions.remove(player.getUniqueId());
                    pending = null;
                }
            }
            if (pending != null) {
                if (!pending.initiatorUuid().equals(player.getUniqueId())
                        || !(player.hasPermission("playerguard.admin.delete")
                        || player.hasPermission("playerguard.command.admin.delete")
                        || player.hasPermission("playerguard.admin.confirm"))) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "管理者削除を確認する権限がありません。");
                    return true;
                }
                PendingDeletion finalPending = pending;
                PlayerGuard.getInstance().getScheduler().runGlobal(() -> {
                    Map.Entry<com.sk89q.worldguard.protection.regions.ProtectedRegion, org.bukkit.World> found = PGUtil.findPlayerGuardRegions(finalPending.protectionId());
                    if (found == null || !finalPending.targetOwnerUuid().equals(net.nekozouneko.playerguard.region.RegionRoles.getPrimaryOwner(found.getKey()))) {
                        player.sendMessage(org.bukkit.ChatColor.RED + "削除対象の保護が見つかりません。");
                    } else {
                        ProtectionDeletionService.Result result = ProtectionDeletionService.delete(player, found.getKey(), found.getValue(), "command-admin");
                        player.sendMessage(result == ProtectionDeletionService.Result.DELETED ? org.bukkit.ChatColor.GREEN + "保護 '" + finalPending.protectionId() + "' を削除しました。" : org.bukkit.ChatColor.RED + "保護を削除できませんでした: " + result);
                    }
                    synchronized (ConfirmCommand.class) { pendingDeletions.remove(player.getUniqueId()); }
                });
                return true;
            }
        }
        Runnable confirm = getConfirm(sender instanceof Player ? ((Player) sender).getUniqueId() : null);

        if (confirm == null) {
            sender.sendMessage(PGMessages.warn("確定できる処理が見つかりませんでした。"));
            return true;
        }

        confirm.run();
        removeConfirm(sender instanceof Player ? ((Player) sender).getUniqueId() : null);
        sender.sendMessage(PGMessages.success("操作を続行しました。"));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, List<String> args) {
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "playerguard.command.playerguard";
    }
}
