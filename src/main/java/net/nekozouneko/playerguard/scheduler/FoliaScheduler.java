package net.nekozouneko.playerguard.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Folia / Paper のリージョンスケジューラ API ベース実装。
 * Folia API 型を参照するため、{@link PGScheduler#create(Plugin)} が
 * Folia API の存在を確認したときのみインスタンス化される（純粋 Spigot では
 * このクラスはロードされない）。
 */
final class FoliaScheduler implements PGScheduler {

    private static final long MS_PER_TICK = 50L;

    private final Plugin plugin;
    private final List<ScheduledTask> tasks = new ArrayList<>();

    FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public PGTask runTimer(Runnable task, long delayTicks, long periodTicks) {
        ScheduledTask t = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin, st -> task.run(),
                Math.max(1L, delayTicks), Math.max(1L, periodTicks));
        track(t);
        return t::cancel;
    }

    @Override
    public PGTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        ScheduledTask t = Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin, st -> task.run(),
                Math.max(1L, delayTicks) * MS_PER_TICK,
                Math.max(1L, periodTicks) * MS_PER_TICK,
                TimeUnit.MILLISECONDS);
        track(t);
        return t::cancel;
    }

    @Override
    public void runGlobal(Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, st -> task.run());
    }

    @Override
    public void runOnEntity(Entity entity, Runnable task) {
        // entity が退去済みの場合 run は null を返し、タスクは実行されない（許容）。
        entity.getScheduler().run(plugin, st -> task.run(), null);
    }

    @Override
    public boolean isOwnedByCurrentRegion(Location location) {
        return Bukkit.isOwnedByCurrentRegion(location);
    }

    @Override
    public boolean isOwnedByCurrentRegion(Entity entity) {
        return Bukkit.isOwnedByCurrentRegion(entity);
    }

    @Override
    public void cancelAll() {
        synchronized (tasks) {
            for (ScheduledTask t : tasks) {
                if (!t.isCancelled()) t.cancel();
            }
            tasks.clear();
        }
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
    }

    private void track(ScheduledTask t) {
        synchronized (tasks) { tasks.add(t); }
    }
}
