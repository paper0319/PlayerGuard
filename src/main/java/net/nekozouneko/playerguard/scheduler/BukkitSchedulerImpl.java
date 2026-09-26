package net.nekozouneko.playerguard.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/** 純粋な Spigot サーバー向けの従来 BukkitScheduler ベース実装。 */
final class BukkitSchedulerImpl implements PGScheduler {

    private final Plugin plugin;
    private final List<BukkitTask> tasks = new ArrayList<>();

    BukkitSchedulerImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public PGTask runTimer(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask t = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        synchronized (tasks) { tasks.add(t); }
        return t::cancel;
    }

    @Override
    public PGTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask t = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        synchronized (tasks) { tasks.add(t); }
        return t::cancel;
    }

    @Override
    public void runGlobal(Runnable task) {
        // Spigot のドライバ tick / コマンド実行は既にメインスレッド。
        task.run();
    }

    @Override
    public void runOnEntity(Entity entity, Runnable task) {
        task.run();
    }

    @Override
    public boolean isOwnedByCurrentRegion(Location location) {
        return true;
    }

    @Override
    public boolean isOwnedByCurrentRegion(Entity entity) {
        return true;
    }

    @Override
    public void cancelAll() {
        synchronized (tasks) {
            for (BukkitTask t : tasks) t.cancel();
            tasks.clear();
        }
    }
}
