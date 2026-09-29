package net.nekozouneko.playerguard.scheduler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PGSchedulerTest {

    @Test
    void bukkitIsNotRegionThreaded() {
        assertFalse(new BukkitSchedulerImpl(null).isRegionThreaded(),
                "plain Bukkit has a single main thread, so synchronous teleport is allowed");
    }

    @Test
    void foliaIsRegionThreaded() {
        assertTrue(new FoliaScheduler(null).isRegionThreaded(),
                "Folia rejects Entity#teleport on a region thread, so teleportAsync is required");
    }
}
