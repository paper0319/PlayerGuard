package net.nekozouneko.playerguard.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Bukkit / Folia のスケジューリング差異を吸収する抽象。
 * {@link #create(Plugin)} がランタイムを判定して適切な実装を返す。
 */
public interface PGScheduler {

    /** グローバル（メイン）スレッドで周期実行する。delay/period は tick。 */
    PGTask runTimer(Runnable task, long delayTicks, long periodTicks);

    /** 非同期スレッドで周期実行する。delay/period は tick 換算。 */
    PGTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks);

    /** グローバルリージョン上で一度だけ実行する。 */
    void runGlobal(Runnable task);

    /** 指定エンティティを所有するリージョンスレッド上で一度だけ実行する。 */
    void runOnEntity(Entity entity, Runnable task);

    /** 指定座標が現在のリージョンスレッドに所有されているか。Bukkit では常に true。 */
    boolean isOwnedByCurrentRegion(Location location);

    /** 指定エンティティが現在のリージョンスレッドに所有されているか。Bukkit では常に true。 */
    boolean isOwnedByCurrentRegion(Entity entity);

    /** このスケジューラが起動した全タスクをキャンセルする。 */
    void cancelAll();

    static PGScheduler create(Plugin plugin) {
        if (SchedulerSupport.isFoliaApiPresent()) {
            return new FoliaScheduler(plugin);
        }
        return new BukkitSchedulerImpl(plugin);
    }
}
